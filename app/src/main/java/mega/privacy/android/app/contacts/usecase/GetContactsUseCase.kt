package mega.privacy.android.app.contacts.usecase

import android.content.Context
import androidx.annotation.ColorRes
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.FlowableEmitter
import io.reactivex.rxjava3.disposables.Disposable
import mega.privacy.android.app.R
import mega.privacy.android.app.contacts.data.ContactItem
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaChatListenerInterface
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.TimeUtils
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.*
import nz.mega.sdk.MegaChatApi.*
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaUser
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import javax.inject.Inject
import kotlin.random.Random

class GetContactsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid
) {

    companion object {
        private const val NOT_FOUND = -1
        private const val RECENTLY_ADDED_MIN_HOURS = 24
    }

    fun get(): Flowable<List<ContactItem>> =
        Flowable.create({ emitter: FlowableEmitter<List<ContactItem>> ->
            val contacts = megaApi.contacts.map { megaUser ->
                val userName = megaChatApi.getUserFirstnameFromCache(megaUser.handle)
                val userStatus = megaChatApi.getUserOnlineStatus(megaUser.handle)
                val userImageColor = megaApi.getUserAvatarColor(megaUser).toColorInt()
                val userImageFile = megaUser.email.getUserImageFile()
                val userImageUri = if (userImageFile.exists()) {
                    userImageFile.toUri()
                } else {
                    null
                }

                ContactItem(
                    handle = megaUser.handle,
                    email = megaUser.email,
                    name = userName,
                    status = userStatus,
                    statusColor = userStatus.getStatusColor(),
                    imageUri = userImageUri,
                    imageColor = userImageColor,
                    isNew = megaUser.wasRecentlyAdded()
                )
            }.toMutableList()

            emitter.onNext(contacts)

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
                                        imageUri = File(request.file).toUri()
                                    )
                                USER_ATTR_FIRSTNAME ->
                                    contacts[index] = currentContact.copy(
                                        name = request.text
                                    )
                            }

                            emitter.onNext(contacts)
                        }
                    } else {
                        logError(error.toThrowable().stackTraceToString())
                    }
                },
                onRequestTemporaryError = { _, error ->
                    logError(error.toThrowable().stackTraceToString())
                }
            )

            contacts.forEach { contact ->
                val userImageFile = contact.email.getUserImageFile().absolutePath
                megaApi.getUserAvatar(contact.email, userImageFile, userAttrsListener)
                megaApi.getUserAttribute(contact.email, USER_ATTR_FIRSTNAME, userAttrsListener)

                if (contact.status != STATUS_ONLINE) {
                    megaChatApi.requestLastGreen(contact.handle, null)
                }
            }

            val chatListener = OptionalMegaChatListenerInterface(
                onChatOnlineStatusUpdate = { userHandle, status, _ ->
                    if (emitter.isCancelled) return@OptionalMegaChatListenerInterface

                    val index = contacts.indexOfFirst { it.handle == userHandle }
                    if (index != NOT_FOUND) {
                        val currentContact = contacts[index]
                        contacts[index] = currentContact.copy(
                            status = status,
                            statusColor = status.getStatusColor(),
                            lastSeen = if (status == STATUS_ONLINE) {
                                context.getString(R.string.online_status)
                            } else {
                                megaChatApi.requestLastGreen(userHandle, null)
                                currentContact.lastSeen
                            }
                        )

                        emitter.onNext(contacts)
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

                        emitter.onNext(contacts)
                    }
                }
            )

            megaChatApi.addChatListener(chatListener)

            emitter.setDisposable(Disposable.fromAction {
                megaChatApi.removeChatListener(chatListener)
            })
        }, BackpressureStrategy.BUFFER)

    @ColorRes
    private fun Int.getStatusColor(): Int =
        when (this) {
            STATUS_AWAY -> R.color.orange_400
            STATUS_ONLINE -> R.color.lime_green_500
            STATUS_BUSY -> R.color.salmon_700
            else -> R.color.grey_700
        }

    private fun String.getUserImageFile(): File =
        CacheFolderManager.buildAvatarFile(context, "$this.jpg")

    private fun MegaUser.wasRecentlyAdded(): Boolean {
        val now = LocalDateTime.now()
        val addedTime = Instant.ofEpochSecond(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        return Duration.between(addedTime, now).toHours() < RECENTLY_ADDED_MIN_HOURS
    }
}
