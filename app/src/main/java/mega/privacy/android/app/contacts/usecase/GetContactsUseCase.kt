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
import io.reactivex.rxjava3.core.FlowableEmitter
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import mega.privacy.android.app.R
import mega.privacy.android.app.contacts.list.data.ContactItem
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaChatListenerInterface
import mega.privacy.android.app.listeners.OptionalMegaGlobalListenerInterface
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.MegaUserUtils.getUserStatusColor
import mega.privacy.android.app.utils.MegaUserUtils.isExternalChange
import mega.privacy.android.app.utils.MegaUserUtils.isRequestedChange
import mega.privacy.android.app.utils.MegaUserUtils.wasRecentlyAdded
import mega.privacy.android.app.utils.StringUtils.decodeAliases
import mega.privacy.android.app.utils.StringUtils.decodeBase64
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.view.TextDrawable
import nz.mega.sdk.*
import nz.mega.sdk.MegaApiJava.*
import nz.mega.sdk.MegaChatApi.*
import nz.mega.sdk.MegaUser.VISIBILITY_VISIBLE
import java.io.File
import java.util.*
import javax.inject.Inject

class GetContactsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid
) {

    companion object {
        private const val NOT_FOUND = -1
    }

    fun get(): Flowable<List<ContactItem.Data>> =
        Flowable.create({ emitter: FlowableEmitter<List<ContactItem.Data>> ->
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
                        if (index != NOT_FOUND) {
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
                            val requestAliases = request.megaStringMap.decodeAliases()

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
                        logError(error.toThrowable().stackTraceToString())
                    }
                },
                onRequestTemporaryError = { _, error ->
                    logError(error.toThrowable().stackTraceToString())
                }
            )

            val chatListener = OptionalMegaChatListenerInterface(
                onChatOnlineStatusUpdate = { userHandle, status, _ ->
                    if (emitter.isCancelled) return@OptionalMegaChatListenerInterface

                    val index = contacts.indexOfFirst { it.handle == userHandle }
                    if (index != NOT_FOUND) {
                        val currentContact = contacts[index]
                        contacts[index] = currentContact.copy(
                            status = status,
                            statusColor = getUserStatusColor(status),
                            lastSeen = if (status == STATUS_ONLINE) {
                                context.getString(R.string.online_status)
                            } else {
                                megaChatApi.requestLastGreen(userHandle, null)
                                currentContact.lastSeen
                            }
                        )

                        emitter.onNext(contacts.sortedAlphabetically())
                    }
                },
                onChatPresenceLastGreen = { userHandle, lastGreen ->
                    if (emitter.isCancelled) return@OptionalMegaChatListenerInterface

                    val index = contacts.indexOfFirst { it.handle == userHandle }
                    if (index != NOT_FOUND) {
                        val currentContact = contacts[index]
                        contacts[index] = currentContact.copy(
                            lastSeen = TimeUtils.unformattedLastGreenDate(context, lastGreen)
                        )

                        emitter.onNext(contacts.sortedAlphabetically())
                    }
                }
            )

            val globalListener = OptionalMegaGlobalListenerInterface(
                onUsersUpdate = { users ->
                    if (emitter.isCancelled) return@OptionalMegaGlobalListenerInterface

                    users.forEach { user ->
                        val index = contacts.indexOfFirst { it.email == user.email }
                        if (index != NOT_FOUND) {
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
                        } else if (user.hasChanged(MegaUser.CHANGE_TYPE_ALIAS)) {
                            megaApi.getUserAttribute(user, USER_ATTR_ALIAS, userAttrsListener)
                        } else if (user.isRequestedChange() && user.visibility == VISIBILITY_VISIBLE) { // New contact
                            val contact = user.toContactItem().apply { requestMissingFields(userAttrsListener) }
                            contacts.add(contact)
                            emitter.onNext(contacts.sortedAlphabetically())
                        }
                    }
                }
            )

            megaChatApi.addChatListener(chatListener)
            megaApi.addGlobalListener(globalListener)

            contacts.forEach { it.requestMissingFields(userAttrsListener) }

            emitter.setDisposable(Disposable.fromAction {
                megaChatApi.removeChatListener(chatListener)
                megaApi.removeGlobalListener(globalListener)
            })
        }, BackpressureStrategy.LATEST)

    fun getMegaUser(userEmail: String): Single<MegaUser> =
        Single.fromCallable { megaApi.getContact(userEmail) }

    private fun MegaUser.toContactItem(): ContactItem.Data {
        val alias = megaChatApi.getUserAliasFromCache(handle)?.decodeBase64()
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

        return ContactItem.Data(
            handle = handle,
            email = email,
            alias = alias,
            fullName = fullName,
            status = userStatus,
            statusColor = getUserStatusColor(userStatus),
            avatarUri = userAvatar,
            placeholder = placeholder,
            isNew = wasRecentlyAdded()
        )
    }

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
