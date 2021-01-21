package mega.privacy.android.app.audioplayer

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ActivityContext
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.components.saver.MegaNodeSaver
import mega.privacy.android.app.components.saver.OfflineNodeSaver
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.listeners.ChatBaseListener
import mega.privacy.android.app.listeners.CreateChatListener
import mega.privacy.android.app.listeners.CreateChatListener.SEND_FILE
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.MegaNodeUtilKt
import nz.mega.sdk.*
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import java.util.*

/**
 * ViewModel for main audio player UI logic.
 */
class AudioPlayerViewModel @ViewModelInject constructor(
    @ActivityContext private val context: Context,
    private val offlineNodeSaver: OfflineNodeSaver,
    private val megaNodeSaver: MegaNodeSaver,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    private val dbHandler: DatabaseHandler,
) : BaseRxViewModel() {

    private val _snackbarToShow = MutableLiveData<Triple<Int, String, Long>>()
    val snackbarToShow: LiveData<Triple<Int, String, Long>> = _snackbarToShow

    private val _itemToRemove = MutableLiveData<Long>()
    val itemToRemove: LiveData<Long> = _itemToRemove

    /**
     * Save an offline node to device.
     *
     * @param handle node handle
     * @param activityStarter function to start activity
     */
    fun saveOfflineNode(handle: Long, activityStarter: (Intent, Int) -> Unit) {
        val node = dbHandler.findByHandle(handle) ?: return
        offlineNodeSaver.save(listOf(node), false, activityStarter)
    }

    /**
     * Save a mega node to device.
     *
     * @param handle node handle
     * @param isFolderLink if this node is a folder link node
     * @param activityStarter function to start activity
     */
    fun saveMegaNode(handle: Long, isFolderLink: Boolean, activityStarter: (Intent, Int) -> Unit) {
        megaNodeSaver.save(listOf(handle), false, isFolderLink, activityStarter)
    }

    /**
     * Handle activity result launched by NodeSaver.
     *
     * @param requestCode requestCode of onActivityResult
     * @param resultCode resultCode of onActivityResult
     * @param data data of onActivityResult
     * @param snackbarShower interface to show snackbar
     * @param activityLauncher interface to start activity
     */
    fun handleActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        snackbarShower: SnackbarShower,
        activityLauncher: ActivityLauncher
    ) {
        if (offlineNodeSaver.handleActivityResult(requestCode, resultCode, data)
            || megaNodeSaver.handleActivityResult(requestCode, resultCode, data)
            || resultCode != RESULT_OK || data == null
        ) {
            return
        }

        when (requestCode) {
            REQUEST_CODE_SELECT_CHAT -> {
                handleSelectChatResult(data)
            }
            REQUEST_CODE_SELECT_MOVE_FOLDER -> {
                val handles = MegaNodeUtilKt.handleSelectMoveFolderResult(
                    requestCode, resultCode, data, snackbarShower
                )

                for (handle in handles) {
                    _itemToRemove.value = handle
                }
            }
            REQUEST_CODE_SELECT_COPY_FOLDER -> {
                MegaNodeUtilKt.handleSelectCopyFolderResult(
                    requestCode, resultCode, data, snackbarShower, activityLauncher
                )
            }
        }
    }

    private fun handleSelectChatResult(intent: Intent) {
        val nodeHandles = intent.getLongArrayExtra(NODE_HANDLES)
        if (nodeHandles == null || nodeHandles.isEmpty()) {
            return
        }

        val chatHandles = intent.getLongArrayExtra(SELECTED_CHATS)
        val contactHandles = intent.getLongArrayExtra(SELECTED_USERS)

        if (contactHandles != null && contactHandles.isNotEmpty()) {
            val users = ArrayList<MegaUser>()
            for (handle in contactHandles) {
                val user = megaApi.getContact(MegaApiAndroid.userHandleToBase64(handle))
                if (user != null) {
                    users.add(user)
                }
            }

            val chats = ArrayList<MegaChatRoom>()
            if (chatHandles != null && chatHandles.isNotEmpty()) {
                for (handle in chatHandles) {
                    val chatRoom = megaChatApi.getChatRoom(handle)
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
                                    _snackbarToShow.value = Triple(
                                        MESSAGE_SNACKBAR_TYPE,
                                        context.getString(R.string.sent_as_message),
                                        request.chatHandle
                                    )
                                } else {
                                    _snackbarToShow.value = Triple(
                                        MESSAGE_SNACKBAR_TYPE,
                                        context.getString(R.string.sent_as_message),
                                        MEGACHAT_INVALID_HANDLE
                                    )
                                }
                            } else if (errorSent == chatCount) {
                                _snackbarToShow.value = Triple(
                                    SNACKBAR_TYPE,
                                    context.getString(R.string.error_attaching_node_from_cloud),
                                    MEGACHAT_INVALID_HANDLE
                                )
                            } else {
                                _snackbarToShow.value = Triple(
                                    MESSAGE_SNACKBAR_TYPE,
                                    context.getString(R.string.error_sent_as_message),
                                    MEGACHAT_INVALID_HANDLE
                                )
                            }
                        }
                    }
                }
            }

            for (handle in chatHandles) {
                megaChatApi.attachNode(handle, nodeHandles[0], megaChatRequestListener)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        offlineNodeSaver.destroy()
        megaNodeSaver.destroy()
    }
}
