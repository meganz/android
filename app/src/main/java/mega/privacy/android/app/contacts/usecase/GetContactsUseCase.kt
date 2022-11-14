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
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import mega.privacy.android.app.R
import mega.privacy.android.app.contacts.list.data.ContactItem
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.usecase.GetGlobalChangesUseCase
import mega.privacy.android.app.usecase.chat.GetChatChangesUseCase
import mega.privacy.android.app.usecase.chat.GetChatChangesUseCase.Result.OnChatConnectionStateUpdate
import mega.privacy.android.app.usecase.chat.GetChatChangesUseCase.Result.OnChatOnlineStatusUpdate
import mega.privacy.android.app.usecase.chat.GetChatChangesUseCase.Result.OnChatPresenceLastGreen
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.MegaUserUtils.getUserStatusColor
import mega.privacy.android.app.utils.MegaUserUtils.isExternalChange
import mega.privacy.android.app.utils.MegaUserUtils.wasRecentlyAdded
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.view.TextDrawable
import mega.privacy.android.data.extensions.getDecodedAliases
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.USER_ATTR_ALIAS
import nz.mega.sdk.MegaApiJava.USER_ATTR_AVATAR
import nz.mega.sdk.MegaApiJava.USER_ATTR_FIRSTNAME
import nz.mega.sdk.MegaApiJava.USER_ATTR_LASTNAME
import nz.mega.sdk.MegaChatApi.STATUS_ONLINE
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaUser
import nz.mega.sdk.MegaUser.VISIBILITY_VISIBLE
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Use case to retrieve contacts for current user.
 *
 * @property context                    Application context required to get resources
 * @property megaApi                    MegaApi required to call the SDK
 * @property megaChatApi                MegaChatApi required to call the SDK
 * @property getChatChangesUseCase      Use case required to get contact request updates
 * @property getGlobalChangesUseCase    Use case required to get contact updates
 */
class GetContactsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    private val getChatChangesUseCase: GetChatChangesUseCase,
    private val getGlobalChangesUseCase: GetGlobalChangesUseCase
) {

    fun get(): Flowable<List<ContactItem.Data>> =
        Flowable.create({ emitter ->
            val disposable = CompositeDisposable()
            val contacts = megaApi.contacts
                .filter { it.visibility == VISIBILITY_VISIBLE }
                .map { it.toContactItem() }
                .toMutableList()

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
                                        fullName = megaChatApi.getUserFullnameFromCache(currentContact.handle)
                                    )
                                USER_ATTR_ALIAS ->
                                    contacts[index] = currentContact.copy(
                                        alias = request.text
                                    )
                            }

                            emitter.onNext(contacts.sortedAlphabetically())
                        } else if (request.paramType == USER_ATTR_ALIAS) {
                            val requestAliases = request.megaStringMap.getDecodedAliases()

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
                                            context.getString(R.string.online_status)
                                        } else {
                                            megaChatApi.requestLastGreen(change.userHandle, null)
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
                                        lastSeen = TimeUtils.unformattedLastGreenDate(
                                            context,
                                            change.lastGreen
                                        )
                                    )

                                    emitter.onNext(contacts.sortedAlphabetically())
                                }
                            }
                            is OnChatConnectionStateUpdate -> {
                                val index = contacts.indexOfFirst {
                                    it.isNew && change.chatid == megaChatApi.getChatRoomByUser(it.handle)?.chatId
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
                    onError = Timber::e
                ).addTo(disposable)

            getGlobalChangesUseCase.get()
                .filter { it is GetGlobalChangesUseCase.Result.OnUsersUpdate }
                .map { (it as GetGlobalChangesUseCase.Result.OnUsersUpdate).users ?: emptyList() }
                .subscribeBy(
                    onNext = { users ->
                        if (emitter.isCancelled) return@subscribeBy

                        users.forEach { user ->
                            val index = contacts.indexOfFirst { it.email == user.email }
                            when {
                                index != INVALID_POSITION -> {
                                    when {
                                        user.isExternalChange() && user.hasChanged(MegaUser.CHANGE_TYPE_AVATAR) ->
                                            megaApi.getUserAttribute(user.email, USER_ATTR_ALIAS, userAttrsListener)
                                        user.hasChanged(MegaUser.CHANGE_TYPE_FIRSTNAME) ->
                                            megaApi.getUserAttribute(user.email, USER_ATTR_FIRSTNAME, userAttrsListener)
                                        user.hasChanged(MegaUser.CHANGE_TYPE_LASTNAME) ->
                                            megaApi.getUserAttribute(user.email, USER_ATTR_LASTNAME, userAttrsListener)
                                        user.visibility != VISIBILITY_VISIBLE -> {
                                            contacts.removeAt(index)
                                            emitter.onNext(contacts.sortedAlphabetically())
                                        }
                                    }
                                }
                                user.hasChanged(MegaUser.CHANGE_TYPE_ALIAS) -> {
                                    megaApi.getUserAttribute(user, USER_ATTR_ALIAS, userAttrsListener)
                                }
                                user.visibility == VISIBILITY_VISIBLE -> { // New contact
                                    val contact = user.toContactItem()
                                    contacts.add(contact)
                                    emitter.onNext(contacts.sortedAlphabetically())
                                    contact.requestMissingFields(userAttrsListener)
                                }
                            }
                        }
                    },
                    onError = Timber::e
                ).addTo(disposable)

            contacts.forEach { it.requestMissingFields(userAttrsListener) }

            emitter.setCancellable { disposable.clear() }
        }, BackpressureStrategy.LATEST)

    /**
     * Get MegaUser from email
     *
     * @param userEmail     Email to retrieve
     * @return              Single containing MegaUser
     */
    fun getMegaUser(userEmail: String): Single<MegaUser> =
        Single.fromCallable { megaApi.getContact(userEmail) }

    /**
     * Build ContactItem.Data from MegaUser object
     *
     * @return  ContactItem.Data
     */
    private fun MegaUser.toContactItem(): ContactItem.Data {
        val alias = megaChatApi.getUserAliasFromCache(handle)
        val fullName = megaChatApi.getUserFullnameFromCache(handle)
        val userStatus = megaChatApi.getUserOnlineStatus(handle)
        val userImageColor = megaApi.getUserAvatarColor(this).toColorInt()
        val title = when {
            !alias.isNullOrBlank() -> alias
            !fullName.isNullOrBlank() -> fullName
            else -> email
        }
        val placeholder = getImagePlaceholder(title, userImageColor)
        val userAvatarFile = AvatarUtil.getUserAvatarFile(context, email)
        val userAvatar = if (userAvatarFile?.exists() == true) {
            userAvatarFile.toUri()
        } else {
            null
        }
        val isNew = wasRecentlyAdded() && megaChatApi.getChatRoomByUser(handle) == null

        return ContactItem.Data(
            handle = handle,
            email = email,
            alias = alias,
            fullName = fullName,
            status = userStatus,
            statusColor = getUserStatusColor(userStatus),
            avatarUri = userAvatar,
            placeholder = placeholder,
            isNew = isNew
        )
    }

    /**
     * Request missing fields for current `ContactItem.Data`
     *
     * @param listener  Callback to retrieve requested fields
     */
    private fun ContactItem.Data.requestMissingFields(listener: MegaRequestListenerInterface) {
        if (avatarUri == null) {
            val userAvatarFile = AvatarUtil.getUserAvatarFile(context, email)?.absolutePath
            megaApi.getUserAvatar(email, userAvatarFile, listener)
        }
        if (fullName.isNullOrBlank()) {
            megaApi.getUserAttribute(email, USER_ATTR_FIRSTNAME, listener)
            megaApi.getUserAttribute(email, USER_ATTR_LASTNAME, listener)
        }
        if (alias.isNullOrBlank()) {
            megaApi.getUserAttribute(email, USER_ATTR_ALIAS, listener)
        }
        if (status != STATUS_ONLINE) {
            megaChatApi.requestLastGreen(handle, null)
        }
    }

    /**
     * Build Avatar placeholder Drawable given a Title and a Color
     *
     * @param title     Title string
     * @param color     Background color
     * @return          Drawable with the placeholder
     */
    private fun getImagePlaceholder(title: String, @ColorInt color: Int): Drawable =
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

    private fun MutableList<ContactItem.Data>.sortedAlphabetically(): List<ContactItem.Data> =
        sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, ContactItem.Data::getTitle))
}
