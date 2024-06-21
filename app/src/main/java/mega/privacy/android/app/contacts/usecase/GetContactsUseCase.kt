package mega.privacy.android.app.contacts.usecase

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.rx3.asFlowable
import mega.privacy.android.app.R
import mega.privacy.android.app.contacts.list.data.ContactItem
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.usecase.chat.GetChatChangesUseCase
import mega.privacy.android.app.usecase.chat.GetChatChangesUseCase.Result.OnChatConnectionStateUpdate
import mega.privacy.android.app.usecase.chat.GetChatChangesUseCase.Result.OnChatOnlineStatusUpdate
import mega.privacy.android.app.usecase.chat.GetChatChangesUseCase.Result.OnChatPresenceLastGreen
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.MegaUserUtils.getUserStatusColor
import mega.privacy.android.app.utils.MegaUserUtils.wasRecentlyAdded
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.view.TextDrawable
import mega.privacy.android.data.extensions.getDecodedAliases
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.repository.AccountRepository
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.USER_ATTR_ALIAS
import nz.mega.sdk.MegaApiJava.USER_ATTR_AVATAR
import nz.mega.sdk.MegaApiJava.USER_ATTR_FIRSTNAME
import nz.mega.sdk.MegaApiJava.USER_ATTR_LASTNAME
import nz.mega.sdk.MegaChatApi.STATUS_ONLINE
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaUser
import nz.mega.sdk.MegaUser.VISIBILITY_VISIBLE
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Get contacts use case
 *
 * @property getChatChangesUseCase    Get chat changes use case
 * @property megaContactsMapper       Mapper to convert MegaUser to ContactItem.Data
 * @property getContacts              Get contacts
 * @property getUserAttribute          Get user attribute
 * @property getContact               Get contact
 * @property areCredentialsVerified   Check if credentials are verified
 *  @property getUserFullnameFromCache Get user full name from cache
 *  @property requestLastGreen         Request last green
 *  @property getChatRoomIdByUser      Get chat room id by user
 *  @property getUserAvatar            Get user avatar
 *  @property onlineString             Get online string
 *  @property getUnformattedLastSeenDate Get unformatted last seen date
 *  @property getAliasMap              Get alias map
 *  @property getUserUpdates           Get user updates
 */
class GetContactsUseCase(
    private val getChatChangesUseCase: GetChatChangesUseCase,
    private val megaContactsMapper: (MegaUser, File) -> ContactItem.Data,
    private val getContacts: () -> ArrayList<MegaUser>,
    private val getUserAttribute: (String, Int, MegaRequestListenerInterface) -> Unit,
    private val getContact: (String) -> MegaUser?,
    private val areCredentialsVerified: (MegaUser) -> Boolean,
    private val getUserFullnameFromCache: (Long) -> String,
    private val requestLastGreen: (Long) -> Unit,
    private val getChatRoomIdByUser: (Long) -> Long?,
    private val getUserAvatar: (String, String, MegaRequestListenerInterface) -> Unit,
    private val onlineString: () -> String,
    private val getUnformattedLastSeenDate: (Int) -> String,
    private val getAliasMap: (MegaRequest) -> Map<Long, String>,
    private val getUserUpdates: () -> Flow<UserUpdate>,
) {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        @MegaApi megaApi: MegaApiAndroid,
        megaChatApi: MegaChatApiAndroid,
        getChatChangesUseCase: GetChatChangesUseCase,
        accountsRepository: AccountRepository,
    ) : this(
        getChatChangesUseCase = getChatChangesUseCase,
        megaContactsMapper = { user, avatarFolder ->
            user.toContactItem(
                avatarFolder = avatarFolder,
                megaChatApi = megaChatApi,
                megaApi = megaApi,
                context = context
            )
        },
        getContacts = { megaApi.contacts },
        getUserAttribute = { email, attr, listener ->
            megaApi.getUserAttribute(email, attr, listener)
        },
        getContact = { email ->
            megaApi.getContact(email)
        },
        areCredentialsVerified = { user ->
            megaApi.areCredentialsVerified(user)
        },
        getUserFullnameFromCache = { handle ->
            megaChatApi.getUserFullnameFromCache(handle)
        },
        requestLastGreen = { handle ->
            megaChatApi.requestLastGreen(handle, null)
        },
        getChatRoomIdByUser = { handle ->
            megaChatApi.getChatRoomByUser(handle)?.chatId
        },
        getUserAvatar = { email, file, listener ->
            megaApi.getUserAvatar(email, file, listener)
        },
        onlineString = {
            context.getString(R.string.online_status)
        },
        getUnformattedLastSeenDate = { lastGreen ->
            TimeUtils.unformattedLastGreenDate(context, lastGreen)
        },
        getAliasMap = { request ->
            request.megaStringMap.getDecodedAliases()
        }, getUserUpdates = {
            accountsRepository.monitorUserUpdates()
        }
    )


    /**
     * Gets contacts.
     *
     * @param avatarFolder Avatar folder in cache.
     */
    fun get(avatarFolder: File): Flowable<List<ContactItem.Data>> = Flowable.create({ emitter ->
        val disposable = CompositeDisposable()
        val contacts = getContactList(avatarFolder)

        emitter.onNext(contacts.sortedAlphabetically())

        val userAttrsListener = OptionalMegaRequestListenerInterface(
            onRequestFinish = { request, error ->
                if (emitter.isCancelled) return@OptionalMegaRequestListenerInterface

                if (error.errorCode == MegaError.API_OK) {
                    val index = contacts.indexOfFirst { it.email == request.email }
                    if (index != INVALID_POSITION) {
                        val currentContact = contacts[index]

                        when (request.paramType) {
                            USER_ATTR_AVATAR -> {
                                if (!request.file.isNullOrBlank()) {
                                    contacts[index] = currentContact.copy(
                                        avatarUri = File(request.file).toUri()
                                    )
                                }
                            }

                            USER_ATTR_FIRSTNAME, USER_ATTR_LASTNAME ->
                                contacts[index] = currentContact.copy(
                                    fullName = getUserFullnameFromCache(currentContact.handle)
                                )

                            USER_ATTR_ALIAS ->
                                contacts[index] = currentContact.copy(
                                    alias = request.text
                                )
                        }

                        emitter.onNext(contacts.sortedAlphabetically())
                    } else if (request.paramType == USER_ATTR_ALIAS) {
                        val requestAliases = getAliasMap(request)

                        contacts.forEachIndexed { indexToUpdate, contact ->
                            var newAlias: String? = null
                            if (requestAliases.isNotEmpty() && requestAliases.containsKey(contact.handle)) {
                                newAlias = requestAliases[contact.handle]
                            }
                            if (newAlias != contact.alias) {
                                contacts[indexToUpdate] = contact.copy(alias = newAlias)
                            }
                        }

                        emitter.onNext(contacts.sortedAlphabetically())
                    }
                } else {
                    Timber.e(error.toThrowable())
                }
            },
            onRequestTemporaryError = { _, error ->
                Timber.e(error.toThrowable())
            }
        )

        getChatChangesUseCase.get()
            .filter { it is OnChatOnlineStatusUpdate || it is OnChatPresenceLastGreen || it is OnChatConnectionStateUpdate }
            .subscribeBy(
                onNext = { change ->
                    if (emitter.isCancelled) return@subscribeBy

                    when (change) {
                        is OnChatOnlineStatusUpdate -> {
                            val index = contacts.indexOfFirst { it.handle == change.userHandle }
                            if (index != INVALID_POSITION) {
                                val currentContact = contacts[index]
                                contacts[index] = currentContact.copy(
                                    status = change.status,
                                    statusColor = getUserStatusColor(change.status),
                                    lastSeen = if (change.status == STATUS_ONLINE) {
                                        onlineString()
                                    } else {
                                        requestLastGreen(change.userHandle)
                                        currentContact.lastSeen
                                    }
                                )

                                emitter.onNext(contacts.sortedAlphabetically())
                            }
                        }

                        is OnChatPresenceLastGreen -> {
                            val index = contacts.indexOfFirst { it.handle == change.userHandle }
                            if (index != INVALID_POSITION) {
                                val currentContact = contacts[index]
                                contacts[index] = currentContact.copy(
                                    lastSeen = getUnformattedLastSeenDate(
                                        change.lastGreen
                                    )
                                )

                                emitter.onNext(contacts.sortedAlphabetically())
                            }
                        }

                        is OnChatConnectionStateUpdate -> {
                            val index = contacts.indexOfFirst {
                                it.isNew && change.chatid == getChatRoomIdByUser(it.handle)
                            }
                            if (index != INVALID_POSITION) {
                                val currentContact = contacts[index]
                                contacts[index] = currentContact.copy(
                                    isNew = false
                                )

                                emitter.onNext(contacts.sortedAlphabetically())
                            }
                        }

                        else -> {
                            // Nothing to do
                        }
                    }
                },
                onError = { Timber.e(it) }
            ).addTo(disposable)

        getUserUpdates().asFlowable()
            .subscribeBy(
                onNext = { userUpdate ->
                    if (emitter.isCancelled) return@subscribeBy

                    userUpdate.changesByEmail().forEach { (email, changes) ->
                        val index = contacts.indexOfFirst { it.email == email }
                        when {
                            index != INVALID_POSITION -> {
                                when {
                                    changes.contains(UserChanges.Avatar) -> email?.let {
                                        getUserAttribute(
                                            it, USER_ATTR_AVATAR,
                                            userAttrsListener
                                        )
                                    }

                                    changes.contains(UserChanges.Firstname) -> email?.let {
                                        getUserAttribute(
                                            it,
                                            USER_ATTR_FIRSTNAME,
                                            userAttrsListener
                                        )
                                    }

                                    changes.contains(UserChanges.Lastname) -> email?.let {
                                        getUserAttribute(
                                            it,
                                            USER_ATTR_LASTNAME,
                                            userAttrsListener
                                        )
                                    }

                                    changes.any { it is UserChanges.Visibility && it.userVisibility != UserVisibility.Visible } -> {
                                        contacts.removeAt(index)
                                        emitter.onNext(contacts.sortedAlphabetically())
                                    }
                                }
                            }

                            changes.contains(UserChanges.Alias) -> {
                                email?.let {
                                    getUserAttribute(
                                        it, USER_ATTR_ALIAS, userAttrsListener
                                    )
                                }
                            }

                            changes.any { it is UserChanges.Visibility && it.userVisibility == UserVisibility.Visible } -> { // New contact
                                val newContactsList = getContactList(avatarFolder)
                                val currentContactIds = contacts.map { it.handle }
                                val diff =
                                    newContactsList.filter { it.handle !in currentContactIds }
                                contacts.addAll(diff)
                                emitter.onNext(contacts.sortedAlphabetically())
                                diff.forEach {
                                    it.requestMissingFields(
                                        avatarFolder, userAttrsListener
                                    )
                                }
                            }

                            changes.contains(UserChanges.AuthenticationInformation) -> {
                                mutableListOf<ContactItem.Data>()
                                    .apply { addAll(contacts) }
                                    .forEachIndexed { i, _ ->
                                        val currentContact = contacts[i]
                                        val currentUser =
                                            getContact(currentContact.email)

                                        currentUser?.let {
                                            val isVerified = areCredentialsVerified(it)

                                            if (currentContact.isVerified != isVerified) {
                                                contacts[i] = currentContact.copy(
                                                    isVerified = isVerified
                                                )
                                            }
                                        }
                                    }

                                emitter.onNext(contacts.sortedAlphabetically())
                            }
                        }
                    }
                },
                onError = { Timber.e(it) }
            ).addTo(disposable)

        contacts.forEach { it.requestMissingFields(avatarFolder, userAttrsListener) }

        emitter.setCancellable { disposable.clear() }
    }, BackpressureStrategy.LATEST)

    private fun getContactList(avatarFolder: File) =
        getContacts().filter { it.visibility == VISIBILITY_VISIBLE }
            .map { megaContactsMapper(it, avatarFolder) }.toMutableList()



    /**
     * Request missing fields for current `ContactItem.Data`
     *
     * @param avatarFolder Avatar folder in cache.
     * @param listener  Callback to retrieve requested fields
     */
    private fun ContactItem.Data.requestMissingFields(
        avatarFolder: File,
        listener: MegaRequestListenerInterface,
    ) {
        if (avatarUri == null) {
            val userAvatarFile = File(avatarFolder, "$email.jpg").absolutePath
            getUserAvatar(email, userAvatarFile, listener)
        }
        if (fullName.isNullOrBlank()) {
            getUserAttribute(email, USER_ATTR_FIRSTNAME, listener)
            getUserAttribute(email, USER_ATTR_LASTNAME, listener)
        }
        if (alias.isNullOrBlank()) {
            getUserAttribute(email, USER_ATTR_ALIAS, listener)
        }
        if (status != STATUS_ONLINE) {
            requestLastGreen(handle)
        }
    }

    private fun MutableList<ContactItem.Data>.sortedAlphabetically(): List<ContactItem.Data> =
        sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, ContactItem.Data::getTitle))
}

/**
 * Build ContactItem.Data from MegaUser object
 *
 * @param avatarFolder Avatar folder in cache.
 * @return  ContactItem.Data
 */
private fun MegaUser.toContactItem(
    avatarFolder: File,
    megaChatApi: MegaChatApiAndroid,
    megaApi: MegaApiAndroid,
    context: Context,
): ContactItem.Data {
    val alias = megaChatApi.getUserAliasFromCache(handle)
    val fullName = megaChatApi.getUserFullnameFromCache(handle)
    val userStatus = megaChatApi.getUserOnlineStatus(handle)
    val userImageColor = megaApi.getUserAvatarColor(this)?.toColorInt() ?: -1
    val title = when {
        !alias.isNullOrBlank() -> alias
        !fullName.isNullOrBlank() -> fullName
        else -> email
    }
    val placeholder = getImagePlaceholder(context = context, title = title, color = userImageColor)
    val userAvatarFile = File(avatarFolder, "$email.jpg")
    val userAvatar = if (userAvatarFile.exists()) {
        userAvatarFile.toUri()
    } else {
        null
    }
    val isNew = wasRecentlyAdded() && megaChatApi.getChatRoomByUser(handle) == null
    val isVerified = megaApi.areCredentialsVerified(this)

    return ContactItem.Data(
        handle = handle,
        email = email,
        alias = alias,
        fullName = fullName,
        status = userStatus,
        statusColor = getUserStatusColor(userStatus),
        avatarUri = userAvatar,
        placeholder = placeholder,
        isNew = isNew,
        isVerified = isVerified
    )
}

/**
 * Build Avatar placeholder Drawable given a Title and a Color
 *
 * @param title     Title string
 * @param color     Background color
 * @return          Drawable with the placeholder
 */
private fun getImagePlaceholder(context: Context, title: String, @ColorInt color: Int): Drawable =
    TextDrawable.builder()
        .beginConfig()
        .width(context.resources.getDimensionPixelSize(R.dimen.image_contact_size))
        .height(context.resources.getDimensionPixelSize(R.dimen.image_contact_size))
        .fontSize(context.resources.getDimensionPixelSize(R.dimen.image_contact_text_size))
        .textColor(ContextCompat.getColor(context, R.color.white))
        .bold()
        .toUpperCase()
        .endConfig()
        .buildRound(AvatarUtil.getFirstLetter(title), color)
