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
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.MegaUserUtils.getUserAvatarFile
import mega.privacy.android.app.utils.MegaUserUtils.getUserStatusColor
import mega.privacy.android.app.utils.MegaUserUtils.wasRecentlyAdded
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.view.TextDrawable
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.*
import nz.mega.sdk.MegaChatApi.*
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaUser
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
                .map { megaUser ->
                    val fullName = megaChatApi.getUserFullnameFromCache(megaUser.handle)
                    val userStatus = megaChatApi.getUserOnlineStatus(megaUser.handle)
                    val userImageColor = megaApi.getUserAvatarColor(megaUser).toColorInt()
                    val placeholder = getImagePlaceholder(fullName ?: megaUser.email, userImageColor)
                    val userAvatarFile = getUserAvatarFile(context, megaUser.email)
                    val userAvatar = if (userAvatarFile?.exists() == true) {
                        userAvatarFile.toUri()
                    } else {
                        null
                    }

                    ContactItem.Data(
                        handle = megaUser.handle,
                        email = megaUser.email,
                        fullName = fullName,
                        status = userStatus,
                        statusColor = getUserStatusColor(userStatus),
                        avatarUri = userAvatar,
                        placeholder = placeholder,
                        isNew = megaUser.wasRecentlyAdded()
                    )
                }
                .toMutableList()

            emitter.onNext(contacts.sortedBy(ContactItem.Data::getTitle))

            val userAttrsListener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (emitter.isCancelled) return@OptionalMegaRequestListenerInterface

                    if (error.errorCode == MegaError.API_OK) {
                        val index = contacts.indexOfFirst { it.email == request.email }
                        if (index != NOT_FOUND) {
                            val currentContact = contacts[index]

                            when (request.paramType) {
                                USER_ATTR_AVATAR ->
                                    contacts[index] = currentContact.copy(
                                        avatarUri = File(request.file).toUri()
                                    )
                                USER_ATTR_FIRSTNAME, USER_ATTR_LASTNAME ->
                                    contacts[index] = currentContact.copy(
                                        fullName = megaChatApi.getUserFullnameFromCache(currentContact.handle)
                                    )
                                USER_ATTR_ALIAS ->
                                    contacts[index] = currentContact.copy(
                                        alias = request.text
                                    )
                            }

                            emitter.onNext(contacts.sortedBy { it.fullName ?: it.email })
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

                        emitter.onNext(contacts.sortedBy(ContactItem.Data::getTitle))
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

                        emitter.onNext(contacts.sortedBy(ContactItem.Data::getTitle))
                    }
                }
            )

            megaChatApi.addChatListener(chatListener)

            contacts.forEach { contact ->
                if (contact.avatarUri == null) {
                    val userAvatarFile = getUserAvatarFile(context, contact.email)?.absolutePath
                    megaApi.getUserAvatar(contact.email, userAvatarFile, userAttrsListener)
                }
                if (contact.fullName.isNullOrBlank()) {
                    megaApi.getUserAttribute(contact.email, USER_ATTR_FIRSTNAME, userAttrsListener)
                    megaApi.getUserAttribute(contact.email, USER_ATTR_LASTNAME, userAttrsListener)
                }
                megaApi.getUserAttribute(contact.email, USER_ATTR_ALIAS, userAttrsListener)

                if (contact.status != STATUS_ONLINE) {
                    megaChatApi.requestLastGreen(contact.handle, null)
                }
            }

            emitter.setDisposable(Disposable.fromAction {
                megaChatApi.removeChatListener(chatListener)
            })
        }, BackpressureStrategy.LATEST)

    fun getMegaUser(userEmail: String): Single<MegaUser> =
        Single.fromCallable { megaApi.getContact(userEmail) }

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
            .buildRound(title.first().toString(), color)
}
