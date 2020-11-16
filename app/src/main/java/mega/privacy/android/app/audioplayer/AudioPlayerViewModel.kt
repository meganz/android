package mega.privacy.android.app.audioplayer

import android.content.Context
import android.content.Intent
import androidx.hilt.lifecycle.ViewModelInject
import dagger.hilt.android.qualifiers.ActivityContext
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.components.saver.MegaNodeSaver
import mega.privacy.android.app.components.saver.OfflineNodeSaver
import mega.privacy.android.app.listeners.ChatBaseListener
import mega.privacy.android.app.listeners.CreateChatListener
import mega.privacy.android.app.listeners.CreateChatListener.SEND_FILE
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.MESSAGE_SNACKBAR_TYPE
import mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logWarning
import nz.mega.sdk.*
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import java.util.*

class AudioPlayerViewModel @ViewModelInject constructor(
    @ActivityContext private val context: Context,
    private val offlineNodeSaver: OfflineNodeSaver,
    private val megaNodeSaver: MegaNodeSaver,
    private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
) : BaseRxViewModel() {

    fun saveOfflineNode(handle: Long, activityStarter: (Intent, Int) -> Unit) {
        offlineNodeSaver.save(handle, false, activityStarter)
    }

    fun saveMegaNode(handle: Long, isFolderLink: Boolean, activityStarter: (Intent, Int) -> Unit) {
        megaNodeSaver.save(listOf(handle), false, isFolderLink, activityStarter)
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        return offlineNodeSaver.handleActivityResult(requestCode, resultCode, data)
                || megaNodeSaver.handleActivityResult(requestCode, resultCode, data)
    }

    fun handleSelectChatResult(intent: Intent) {
        val nodeHandles = intent.getLongArrayExtra(Constants.NODE_HANDLES)
        if (nodeHandles == null || nodeHandles.isEmpty()) {
            return
        }

        val chatHandles = intent.getLongArrayExtra(Constants.SELECTED_CHATS)
        val contactHandles = intent.getLongArrayExtra(Constants.SELECTED_USERS)

        if (contactHandles != null && contactHandles.isNotEmpty()) {
            val users = ArrayList<MegaUser>()
            for (i in contactHandles.indices) {
                val user = megaApi.getContact(MegaApiAndroid.userHandleToBase64(contactHandles[i]))
                if (user != null) {
                    users.add(user)
                }
            }

            val chats = ArrayList<MegaChatRoom>()
            if (chatHandles != null && chatHandles.isNotEmpty()) {
                for (i in chatHandles.indices) {
                    val chatRoom = megaChatApi.getChatRoom(chatHandles[i])
                    if (chatRoom != null) {
                        chats.add(chatRoom)
                    }
                }
            }

            val listener = CreateChatListener(chats, users, nodeHandles[0], context, SEND_FILE)
            for (user in users) {
                val peers = MegaChatPeerList.createInstance()
                peers.addPeer(user.handle, MegaChatPeerList.PRIV_STANDARD)
                megaChatApi.createChat(false, peers, listener)
            }
        } else if (chatHandles != null && chatHandles.isNotEmpty()) {
            val chatCount = chatHandles.size

            val megaChatRequestListener = object : ChatBaseListener(context) {
                private var successSent = 0
                private var errorSent = 0

                override fun onRequestFinish(
                    api: MegaChatApiJava,
                    request: MegaChatRequest,
                    e: MegaChatError
                ) {
                    super.onRequestFinish(api, request, e)

                    if (request.type == MegaChatRequest.TYPE_ATTACH_NODE_MESSAGE) {
                        if (e.errorCode == MegaChatError.ERROR_OK) {
                            logDebug("File sent correctly")
                            successSent++
                        } else {
                            logWarning("File NOT sent: " + e.errorCode + "___" + e.errorString)
                            errorSent++
                        }

                        if (chatCount == errorSent + successSent) {
                            if (successSent == chatCount) {
                                if (chatCount == 1) {
                                    (context as AudioPlayerActivity).showSnackbar(
                                        MESSAGE_SNACKBAR_TYPE,
                                        context.getString(R.string.sent_as_message),
                                        request.chatHandle
                                    )
                                } else {
                                    (context as AudioPlayerActivity).showSnackbar(
                                        MESSAGE_SNACKBAR_TYPE,
                                        context.getString(R.string.sent_as_message),
                                        MEGACHAT_INVALID_HANDLE
                                    )
                                }
                            } else if (errorSent == chatCount) {
                                (context as AudioPlayerActivity).showSnackbar(
                                    SNACKBAR_TYPE,
                                    context.getString(R.string.error_attaching_node_from_cloud),
                                    MEGACHAT_INVALID_HANDLE
                                )
                            } else {
                                (context as AudioPlayerActivity).showSnackbar(
                                    MESSAGE_SNACKBAR_TYPE,
                                    context.getString(R.string.error_sent_as_message),
                                    MEGACHAT_INVALID_HANDLE
                                )
                            }
                        }
                    }
                }
            }

            for (i in chatHandles.indices) {
                megaChatApi.attachNode(chatHandles[i], nodeHandles[0], megaChatRequestListener)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        offlineNodeSaver.destroy()
        megaNodeSaver.destroy()
    }
}
