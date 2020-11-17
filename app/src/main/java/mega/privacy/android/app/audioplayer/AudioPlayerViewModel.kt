package mega.privacy.android.app.audioplayer

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ActivityContext
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.components.saver.MegaNodeSaver
import mega.privacy.android.app.components.saver.OfflineNodeSaver
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.listeners.ChatBaseListener
import mega.privacy.android.app.listeners.CreateChatListener
import mega.privacy.android.app.listeners.CreateChatListener.SEND_FILE
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logWarning
import nz.mega.sdk.*
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import java.util.*

class AudioPlayerViewModel @ViewModelInject constructor(
    @ActivityContext private val context: Context,
    private val offlineNodeSaver: OfflineNodeSaver,
    private val megaNodeSaver: MegaNodeSaver,
    private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
) : BaseRxViewModel() {

    private val _snackbarToShow = MutableLiveData<Triple<Int, String, Long>>()
    val snackbarToShow: LiveData<Triple<Int, String, Long>> = _snackbarToShow

    private val _intentToLaunch = MutableLiveData<Intent>()
    val intentToLaunch: LiveData<Intent> = _intentToLaunch

    private val _itemToRemove = MutableLiveData<Long>()
    val itemToRemove: LiveData<Long> = _itemToRemove

    fun saveOfflineNode(handle: Long, activityStarter: (Intent, Int) -> Unit) {
        offlineNodeSaver.save(handle, false, activityStarter)
    }

    fun saveMegaNode(handle: Long, isFolderLink: Boolean, activityStarter: (Intent, Int) -> Unit) {
        megaNodeSaver.save(listOf(handle), false, isFolderLink, activityStarter)
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (offlineNodeSaver.handleActivityResult(requestCode, resultCode, data)
            || megaNodeSaver.handleActivityResult(requestCode, resultCode, data)
        ) {
            return
        }

        if (requestCode == REQUEST_CODE_SELECT_CHAT && resultCode == RESULT_OK && data != null) {
            handleSelectChatResult(data)
        } else if (requestCode == REQUEST_CODE_SELECT_MOVE_FOLDER && resultCode == RESULT_OK
            && data != null
        ) {
            handleMoveResult(data)
        } else if (requestCode == REQUEST_CODE_SELECT_COPY_FOLDER && resultCode == RESULT_OK
            && data != null
        ) {
            handleCopyResult(data)
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

    private fun handleMoveResult(intent: Intent) {
        val moveHandles = intent.getLongArrayExtra(INTENT_EXTRA_KEY_MOVE_HANDLES)
        if (moveHandles == null || moveHandles.isEmpty()) {
            return
        }

        val toHandle = intent.getLongExtra(INTENT_EXTRA_KEY_MOVE_TO, INVALID_HANDLE)
        val parent = megaApi.getNodeByHandle(toHandle) ?: return

        val listener = object : BaseListener(context) {
            override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
                if (request.type == MegaRequest.TYPE_MOVE) {
                    _snackbarToShow.value = Triple(
                        SNACKBAR_TYPE,
                        context.getString(
                            if (e.errorCode == MegaError.API_OK) R.string.context_correctly_moved
                            else R.string.context_no_moved
                        ),
                        MEGACHAT_INVALID_HANDLE
                    )
                }
            }
        }

        for (handle in moveHandles) {
            val node = megaApi.getNodeByHandle(handle)
            if (node != null) {
                _itemToRemove.value = handle
                megaApi.moveNode(node, parent, listener)
            }
        }
    }

    private fun handleCopyResult(intent: Intent) {
        val copyHandles = intent.getLongArrayExtra(INTENT_EXTRA_KEY_COPY_HANDLES)
        if (copyHandles == null || copyHandles.isEmpty()) {
            return
        }

        val toHandle = intent.getLongExtra(INTENT_EXTRA_KEY_COPY_TO, INVALID_HANDLE)
        val parent = megaApi.getNodeByHandle(toHandle) ?: return

        val listener = object : BaseListener(context) {
            override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
                if (request.type == MegaRequest.TYPE_COPY) {
                    when (e.errorCode) {
                        MegaError.API_OK -> {
                            _snackbarToShow.value = Triple(
                                SNACKBAR_TYPE,
                                context.getString(R.string.context_correctly_copied),
                                MEGACHAT_INVALID_HANDLE
                            )
                        }
                        MegaError.API_EOVERQUOTA -> {
                            val toLaunch = Intent(context, ManagerActivityLollipop::class.java)
                            toLaunch.action = ACTION_OVERQUOTA_STORAGE
                            _intentToLaunch.value = toLaunch
                        }
                        MegaError.API_EGOINGOVERQUOTA -> {
                            val toLaunch = Intent(context, ManagerActivityLollipop::class.java)
                            toLaunch.action = ACTION_PRE_OVERQUOTA_STORAGE
                            _intentToLaunch.value = toLaunch
                        }
                        else -> {
                            _snackbarToShow.value = Triple(
                                SNACKBAR_TYPE,
                                context.getString(R.string.context_no_copied),
                                MEGACHAT_INVALID_HANDLE
                            )
                        }
                    }
                }
            }
        }

        for (handle in copyHandles) {
            val node = megaApi.getNodeByHandle(handle)
            if (node != null) {
                megaApi.copyNode(node, parent, listener)
            }
        }
    }

    fun renameNode(node: MegaNode, newName: String) {
        megaApi.renameNode(node, newName, object : BaseListener(context) {
            override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
                if (request.type == MegaRequest.TYPE_RENAME) {
                    _snackbarToShow.value = Triple(
                        SNACKBAR_TYPE,
                        context.getString(
                            if (e.errorCode == MegaError.API_OK) R.string.context_correctly_renamed
                            else R.string.context_no_renamed
                        ),
                        MEGACHAT_INVALID_HANDLE
                    )
                }
            }
        })
    }

    fun moveNodeToRubbishBin(node: MegaNode) {
        megaApi.moveNode(node, megaApi.rubbishNode, object : BaseListener(context) {
            override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
                if (request.type == MegaRequest.TYPE_MOVE) {
                    _snackbarToShow.value = Triple(
                        SNACKBAR_TYPE,
                        context.getString(
                            if (e.errorCode == MegaError.API_OK) R.string.context_correctly_moved
                            else R.string.context_no_moved
                        ),
                        MEGACHAT_INVALID_HANDLE
                    )
                }
            }
        })
    }

    override fun onCleared() {
        super.onCleared()
        offlineNodeSaver.destroy()
        megaNodeSaver.destroy()
    }
}
