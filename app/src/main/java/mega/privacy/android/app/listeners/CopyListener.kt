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
import mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import timber.log.Timber

/**
 * CopyListener
 *
 * @property action : Integer
 * @property snackbarShower : SnackbarShower
 * @property activityLauncher : ActivityLauncher
 * @property context : Context
 */
class CopyListener(
    private val action: Int,
    private val snackbarShower: SnackbarShower,
    private val activityLauncher: ActivityLauncher?,
    private val context: Context,
) : MegaRequestListenerInterface {

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
        chatId: Long,
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
        exportListener: ExportListener?,
    ) : this(action, snackbarShower, null, context) {
        initFields(messagesSelected, counter, chatController, chatId, exportListener)
    }

    private fun initFields(
        messagesSelected: List<MegaChatMessage>,
        counter: Int,
        chatController: ChatController,
        chatId: Long,
        exportListener: ExportListener?,
    ) {
        this.messagesSelected.addAll(messagesSelected)
        this.counter = counter
        this.chatController = chatController
        this.chatId = chatId
        this.exportListener = exportListener
    }

    /**
     * Callback function for onRequestStart
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     */
    override fun onRequestStart(api: MegaApiJava?, request: MegaRequest?) {
        // Do nothing
    }

    /**
     * Callback function for onRequestUpdate
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     */
    override fun onRequestUpdate(api: MegaApiJava?, request: MegaRequest?) {
        // Do nothing
    }

    /**
     * Callback function for onRequestFinish
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     * @param e: MegaError
     */
    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (request.type == MegaRequest.TYPE_COPY) {
            counter--

            if (e.errorCode != MegaError.API_OK) {
                Timber.e("Error copying")
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

                            Intent(context, ManagerActivity::class.java).run {
                                action = ACTION_OVERQUOTA_STORAGE
                                activityLauncher?.launchActivity(this)
                            }
                        }

                        MegaError.API_EGOINGOVERQUOTA -> {
                            Intent(context, ManagerActivity::class.java).run {
                                action = ACTION_PRE_OVERQUOTA_STORAGE
                                activityLauncher?.launchActivity(this)
                            }
                        }
                        else -> {
                            snackbarShower.showSnackbar(getString(R.string.context_no_copied))
                        }
                    }
                }
                MULTIPLE_FORWARD_MESSAGES -> {
                    if (counter == 0) {
                        if (error > 0) {
                            Intent(BroadcastConstants.BROADCAST_ACTION_ERROR_COPYING_NODES).run {
                                putExtra(BroadcastConstants.ERROR_MESSAGE_TEXT,
                                    getQuantityString(R.plurals.error_forwarding_messages, error))
                                context.sendBroadcast(this)
                            }
                        } else {
                            Timber.d("Forward message")
                            chatController?.forwardMessages(messagesSelected, chatId)
                        }
                    }
                }
                MULTIPLE_IMPORT_CONTACT_MESSAGES -> {
                    if (counter == 0) {
                        if (error > 0) {
                            exportListener?.errorImportingNodes() ?: run {
                                snackbarShower.showSnackbar(
                                    getQuantityString(R.plurals.context_link_export_error, error)
                                )
                            }
                        } else {
                            val node = api.getNodeByHandle(request.nodeHandle)

                            if (node == null) {
                                Timber.w("Node is NULL")
                                return
                            }
                            exportListener?.let {
                                it.updateNodeHandle(
                                    messagesSelected[0].msgId,
                                    node.handle
                                )
                                Timber.d("Export Node")
                                api.exportNode(node, it)
                            } ?: run {
                                Timber.d("Share Node")

                                ChatUtil.shareNodeFromChat(
                                    context, node, chatId, messagesSelected[0].msgId
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Callback function for onRequestTemporaryError
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     * @param e: MegaError
     */
    override fun onRequestTemporaryError(api: MegaApiJava?, request: MegaRequest?, e: MegaError?) {
        // Do nothing
    }

    companion object {
        const val COPY = 1
        const val MULTIPLE_FORWARD_MESSAGES = 2
        const val MULTIPLE_IMPORT_CONTACT_MESSAGES = 3
    }
}
