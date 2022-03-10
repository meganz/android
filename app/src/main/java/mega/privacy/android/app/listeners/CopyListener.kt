package mega.privacy.android.app.listeners

import android.content.Context
import android.content.Intent
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.controllers.ChatController
import mega.privacy.android.app.utils.AlertsAndWarnings.showForeignStorageOverQuotaWarningDialog
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants.ACTION_OVERQUOTA_STORAGE
import mega.privacy.android.app.utils.Constants.ACTION_PRE_OVERQUOTA_STORAGE
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import nz.mega.sdk.*

class CopyListener(
    private val action: Int,
    private val snackbarShower: SnackbarShower,
    private val activityLauncher: ActivityLauncher?,
    context: Context
) : BaseListener(context) {

    private val messagesSelected = ArrayList<MegaChatMessage>()

    private var counter = 0
    private var error = 0

    private var chatController: ChatController? = null
    private var chatId = MegaChatApiJava.MEGACHAT_INVALID_HANDLE
    private var exportListener: ExportListener? = null

    constructor(
        action: Int,
        messagesSelected: List<MegaChatMessage>,
        counter: Int,
        context: Context,
        snackbarShower: SnackbarShower,
        chatController: ChatController,
        chatId: Long
    ) : this(action, snackbarShower, null, context) {
        initFields(messagesSelected, counter, chatController, chatId, null)
    }

    constructor(
        action: Int,
        messagesSelected: List<MegaChatMessage>,
        counter: Int,
        context: Context,
        snackbarShower: SnackbarShower,
        chatController: ChatController,
        chatId: Long,
        exportListener: ExportListener?
    ) : this(action, snackbarShower, null, context) {
        initFields(messagesSelected, counter, chatController, chatId, exportListener)
    }

    private fun initFields(
        messagesSelected: List<MegaChatMessage>,
        counter: Int,
        chatController: ChatController,
        chatId: Long,
        exportListener: ExportListener?
    ) {
        this.messagesSelected.addAll(messagesSelected)
        this.counter = counter
        this.chatController = chatController
        this.chatId = chatId
        this.exportListener = exportListener
    }

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (request.type != MegaRequest.TYPE_COPY) {
            return
        }

        counter--

        if (e.errorCode != MegaError.API_OK) {
            logError("Error copying")
            error++
        }

        when (action) {
            COPY -> {
                when (e.errorCode) {
                    MegaError.API_OK -> {
                        snackbarShower.showSnackbar(getString(R.string.context_correctly_copied))
                    }
                    MegaError.API_EOVERQUOTA -> {
                        if (api.isForeignNode(request.parentHandle)) {
                            showForeignStorageOverQuotaWarningDialog(context)
                            return
                        }

                        val intent = Intent(context, ManagerActivity::class.java)
                        intent.action = ACTION_OVERQUOTA_STORAGE
                        activityLauncher?.launchActivity(intent)
                    }
                    MegaError.API_EGOINGOVERQUOTA -> {
                        val intent = Intent(context, ManagerActivity::class.java)
                        intent.action = ACTION_PRE_OVERQUOTA_STORAGE
                        activityLauncher?.launchActivity(intent)
                    }
                    else -> {
                        snackbarShower.showSnackbar(getString(R.string.context_no_copied))
                    }
                }
            }
            MULTIPLE_FORWARD_MESSAGES -> {
                if (counter == 0) {
                    if (error > 0) {
                        val message = getQuantityString(R.plurals.error_forwarding_messages, error)
                        val intent = Intent(BroadcastConstants.BROADCAST_ACTION_ERROR_COPYING_NODES)
                        intent.putExtra(BroadcastConstants.ERROR_MESSAGE_TEXT, message)
                        context.sendBroadcast(intent)
                    } else {
                        logDebug("Forward message")
                        chatController?.forwardMessages(messagesSelected, chatId)
                    }
                }
            }
            MULTIPLE_IMPORT_CONTACT_MESSAGES -> {
                if (counter == 0) {
                    if (error > 0) {
                        if (exportListener != null) {
                            exportListener!!.errorImportingNodes()
                        } else {
                            snackbarShower.showSnackbar(
                                getQuantityString(R.plurals.context_link_export_error, error)
                            )
                        }
                    } else {
                        val node = api.getNodeByHandle(request.nodeHandle)

                        if (node == null) {
                            logWarning("Node is NULL")
                            return
                        }

                        if (exportListener != null) {
                            exportListener!!.updateNodeHandle(
                                messagesSelected[0].msgId,
                                node.handle
                            )

                            logDebug("Export Node")
                            api.exportNode(node, exportListener)
                        } else {
                            logDebug("Share Node")

                            ChatUtil.shareNodeFromChat(
                                context, node, chatId, messagesSelected[0].msgId
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val COPY = 1
        const val MULTIPLE_FORWARD_MESSAGES = 2
        const val MULTIPLE_IMPORT_CONTACT_MESSAGES = 3
    }
}
