package mega.privacy.android.app.contacts.usecase

import android.content.Context
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.FlowableEmitter
import io.reactivex.rxjava3.disposables.Disposable
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.contacts.data.ContactItem
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.ErrorUtils.toThrowable
import mega.privacy.android.app.utils.LogUtil.*
import mega.privacy.android.app.utils.TimeUtils
import nz.mega.sdk.*
import java.io.File
import javax.inject.Inject

class GetContactsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    private val databaseHandler: DatabaseHandler
) {

    fun get(): Flowable<List<ContactItem>> =
        Flowable.create({ emitter: FlowableEmitter<List<ContactItem>> ->
            val contacts = megaApi.contacts.map { megaUser ->
                val userName = databaseHandler.findContactByHandle(megaUser.handle.toString()).name
                val userStatus = megaChatApi.getUserOnlineStatus(megaUser.handle)
                val userImageColor = megaApi.getUserAvatarColor(megaUser).toColorInt()
                val userImageFile = getUserImageFile(megaUser.email)
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
                    imageUri = userImageUri,
                    imageColor = userImageColor
                )
            }.toMutableList()

            emitter.onNext(contacts)

            val chatListener = buildChatListener(
                onChatOnlineStatusUpdate = { userHandle, status ->
                    val index = contacts.indexOfFirst { it.handle == userHandle }

                    if (index != -1) {
                        val contact = contacts[index]
                        contacts[index] = contact.copy(
                            status = status
                        )
                    }

                    emitter.onNext(contacts.toList())
                },
                onChatPresenceLastGreen = { userHandle, lastGreen ->
                    val index = contacts.indexOfFirst { it.handle == userHandle }

                    if (index != -1) {
                        val contact = contacts[index]
                        contacts[index] = contact.copy(
                            lastSeen = TimeUtils.lastGreenDate(context, lastGreen)
                        )
                    }

                    emitter.onNext(contacts.toList())
                }
            )

            megaChatApi.addChatListener(chatListener)

            val avatarListener = buildAvatarListener { request, error ->
                if (emitter.isCancelled) return@buildAvatarListener

                if (error.errorCode == MegaError.API_OK) {
                    val index = contacts.indexOfFirst { it.email == request.email }

                    if (index != -1) {
                        val userImageUri = File(request.file).toUri()

                        val oldContact = contacts[index]
                        contacts[index] = oldContact.copy(
                            imageUri = userImageUri
                        )
                    }

                    emitter.onNext(contacts.toList())
                } else {
                    logError(error.toThrowable().stackTraceToString())
                }
            }

            contacts.forEach { contact ->
                val userImageFile = getUserImageFile(contact.email).absolutePath
                megaApi.getUserAvatar(contact.email, userImageFile, avatarListener)

                if (contact.status != MegaChatApi.STATUS_ONLINE) {
                    megaChatApi.requestLastGreen(contact.handle, null)
                }
            }

            emitter.setDisposable(Disposable.fromAction {
                megaChatApi.removeChatListener(chatListener)
            })
        }, BackpressureStrategy.BUFFER)

    private fun getUserImageFile(userEmail: String): File =
        CacheFolderManager.buildAvatarFile(context, "$userEmail.jpg")

    private fun buildAvatarListener(onFinishAction: (MegaRequest, MegaError) -> Unit) =
        object : MegaRequestListenerInterface {
            override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
                onFinishAction.invoke(request, e)
            }

            override fun onRequestTemporaryError(api: MegaApiJava, request: MegaRequest, e: MegaError) {
                logError(e.toThrowable().stackTraceToString())
            }

            override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {}
            override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {}
        }

    private fun buildChatListener(
        onChatOnlineStatusUpdate: (Long, Int) -> Unit,
        onChatPresenceLastGreen: (Long, Int) -> Unit
    ): MegaChatListenerInterface =
        object : MegaChatListenerInterface {
            override fun onChatOnlineStatusUpdate(
                api: MegaChatApiJava,
                userhandle: Long,
                status: Int,
                inProgress: Boolean
            ) {
                onChatOnlineStatusUpdate.invoke(userhandle, status)
            }

            override fun onChatPresenceLastGreen(
                api: MegaChatApiJava,
                userhandle: Long,
                lastGreen: Int
            ) {
                onChatPresenceLastGreen.invoke(userhandle, lastGreen)
            }

            override fun onChatListItemUpdate(api: MegaChatApiJava, item: MegaChatListItem) {}
            override fun onChatInitStateUpdate(api: MegaChatApiJava, newState: Int) {}
            override fun onChatPresenceConfigUpdate(api: MegaChatApiJava, config: MegaChatPresenceConfig) {}
            override fun onChatConnectionStateUpdate(api: MegaChatApiJava, chatid: Long, newState: Int) {}
        }
}
