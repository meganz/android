package mega.privacy.android.app.listeners

import android.content.Context
import android.content.Intent
import mega.privacy.android.app.R
import mega.privacy.android.app.main.controllers.ChatController
import mega.privacy.android.app.main.megachat.AndroidMegaChatMessage
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaNodeUtil.startShareIntent
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import timber.log.Timber

/**
 * Export listener
 *
 * @property context: Context
 */
class ExportListener constructor(private val context: Context) :
    MegaRequestListenerInterface {
    private var shareIntent: Intent? = null
    private var numberRemove = 0
    private var pendingRemove = 0
    private var numberError = 0
    private var action: String = ""
    private var numberExport = 0
    private var pendingExport = 0
    private var exportedLinks: StringBuilder? = null
    private var messageId = MegaChatApiJava.MEGACHAT_INVALID_HANDLE
    private var chatId = MegaChatApiJava.MEGACHAT_INVALID_HANDLE
    private var messages: List<AndroidMegaChatMessage>? = null
    private val msgIdNodeHandle: HashMap<Long, Long> = HashMap()
    private var onExportFinishedListener: (() -> Unit)? = null

    /**
     * Constructor used for launch a view intent to share content through the link created
     * when the request finishes.
     *
     * @param context                  Current Context
     * @param shareIntent              Intent to share the content
     * @param onExportFinishedListener Listener to manage the result of export request.
     */
    constructor(
        context: Context,
        shareIntent: Intent?,
        onExportFinishedListener: (() -> Unit)?,
    ) : this(context) {
        action = Constants.ACTION_SHARE_NODE
        this.shareIntent = shareIntent
        this.onExportFinishedListener = onExportFinishedListener
    }

    /**
     * Constructor used for launch a view intent to share content through the link created
     * when the request finishes.
     *
     * @param context     current Context
     * @param shareIntent Intent to share the content
     */
    constructor(context: Context, shareIntent: Intent?, messageId: Long, chatId: Long) : this(
        context
    ) {
        action = Constants.ACTION_SHARE_MSG
        this.shareIntent = shareIntent
        this.messageId = messageId
        this.chatId = chatId
        numberExport = 1
        pendingExport = numberExport
    }

    /**
     * Constructor used for remove links of one or more nodes
     *
     * @param context      Current Context
     * @param numberRemove Number of nodes to remove the link
     */
    constructor(context: Context, numberRemove: Int) : this(context) {
        action = Constants.ACTION_REMOVE_LINK
        pendingRemove = numberRemove
        this.numberRemove = pendingRemove
    }

    /**
     * Constructor used for remove the link of a node.
     *
     * @param context                  Current Context
     * @param onExportFinishedListener Listener to manage the result of export request.
     */
    constructor(context: Context, onExportFinishedListener: (() -> Unit)?) : this(
        context
    ) {
        action = Constants.ACTION_REMOVE_LINK
        pendingRemove = 1
        numberRemove = pendingRemove
        this.onExportFinishedListener = onExportFinishedListener
    }

    /**
     * Constructor used for export multiple nodes, then combine links with already exported nodes,
     * then share those links.
     *
     * @param context       current Context
     * @param numberExport  number of nodes to remove the link
     * @param exportedLinks links of already exported nodes
     * @param shareIntent   Intent to share the content
     */
    constructor(
        context: Context, numberExport: Int, exportedLinks: StringBuilder?,
        shareIntent: Intent?, messages: ArrayList<AndroidMegaChatMessage>, chatId: Long,
    ) : this(context) {
        action = Constants.ACTION_SHARE_MSG
        this.shareIntent = shareIntent
        this.messages = messages
        this.chatId = chatId
        this.numberExport = numberExport
        pendingExport = numberExport
        this.exportedLinks = exportedLinks
        for (msg in messages) {
            val msgId = msg.message.msgId
            val nodeHandle = msg.message.megaNodeList[0].handle
            msgIdNodeHandle[nodeHandle] = msgId
        }
    }

    /**
     * Constructor used for export multiple nodes, then combine links with already exported nodes,
     * then share those links.
     *
     * @param context       current Context
     * @param numberExport  number of nodes to remove the link
     * @param exportedLinks links of already exported nodes
     * @param shareIntent   Intent to share the content
     */
    constructor(
        context: Context,
        numberExport: Int,
        exportedLinks: StringBuilder?,
        shareIntent: Intent?,
    ) : this(context) {
        action = Constants.ACTION_SHARE_NODE
        this.shareIntent = shareIntent
        this.numberExport = numberExport
        pendingExport = numberExport
        this.exportedLinks = exportedLinks
    }

    /**
     * Method for updating the handle of the node associated with a message.
     * This is necessary when a node is to be shared and it is necessary to import the node into the cloud and use that node to get a share link.
     *
     * @param msgID         Message ID.
     * @param newNodeHandle node Handle of a node imported.
     */
    fun updateNodeHandle(msgID: Long, newNodeHandle: Long) {
        if (getKeyByValueAndRemoveIt(msgIdNodeHandle, msgID)) {
            msgIdNodeHandle[newNodeHandle] = msgID
        }
    }

    /**
     * Method to display a Snackbar when all nodes have not been imported correctly.
     */
    fun errorImportingNodes() {
        numberError++
        pendingExport--
        if (pendingExport == 0) {
            Timber.e("%s errors exporting nodes", numberExport)
            Util.showSnackbar(
                context,
                context.resources.getQuantityString(
                    R.plurals.context_link_export_error,
                    numberExport
                )
            )
        }
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
        if (request.type == MegaRequest.TYPE_EXPORT) {
            when (action) {
                Constants.ACTION_REMOVE_LINK -> {
                    pendingRemove--
                    if (e.errorCode != MegaError.API_OK) {
                        numberError++
                    }
                    if (pendingRemove == 0) {
                        if (numberError > 0) {
                            Timber.e("Removing link error")
                            Util.showSnackbar(
                                context,
                                context.resources.getQuantityString(
                                    R.plurals.context_link_removal_error,
                                    numberRemove
                                )
                            )
                        } else {
                            onExportFinishedListener?.invoke()
                            Util.showSnackbar(
                                context,
                                context.resources.getQuantityString(
                                    R.plurals.context_link_removal_success,
                                    numberRemove
                                )
                            )
                        }
                    }
                }
                Constants.ACTION_SHARE_NODE, Constants.ACTION_SHARE_MSG ->
                    request.link?.let { nonNullRequestLink ->
                        Timber.d("The link is created")
                        if (e.errorCode != MegaError.API_OK) {
                            numberError++
                        } else {
                            exportedLinks?.append(nonNullRequestLink)?.append("\n\n")
                        }
                        if (exportedLinks == null && numberError == 0) {
                            Timber.d("Start share one item")
                            shareIntent?.let { nonNullShareIntent ->
                                startShareIntent(context, nonNullShareIntent, nonNullRequestLink)
                                onExportFinishedListener?.invoke()
                            }
                            return
                        }
                        pendingExport--
                        if (pendingExport == 0) {
                            shareIntent?.let { nonNullShareIntent ->
                                if (numberError < numberExport) {
                                    startShareIntent(
                                        context,
                                        nonNullShareIntent,
                                        exportedLinks.toString()
                                    )
                                }
                            }

                            if (numberError > 0) {
                                Timber.e("%s errors exporting nodes", numberError)
                                Util.showSnackbar(
                                    context, context.resources
                                        .getQuantityString(
                                            R.plurals.context_link_export_error,
                                            numberExport
                                        )
                                )
                                return
                            }
                        }
                    } ?: run {
                        if (action == Constants.ACTION_SHARE_MSG) {
                            // It is necessary to import the node into the cloud to create a new link from that node.
                            Timber.e(
                                "Error exporting node: %s, it is necessary to import the node",
                                e.errorString
                            )
                            val chatC = ChatController(context)
                            if (messages.isNullOrEmpty()) {
                                Timber.d("One node to import to MEGA and then share")
                            } else {
                                if (msgIdNodeHandle.isNotEmpty()) {
                                    messageId = (msgIdNodeHandle[request.nodeHandle] ?: 0L)
                                    Timber.d("Several nodes to import to MEGA and then share the links")
                                    chatC.setExportListener(this)
                                }
                            }
                            chatC.importNode(messageId, chatId, Constants.IMPORT_TO_SHARE_OPTION)
                        } else {
                            Timber.e("Error exporting node: %s", e.errorString)
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
        //Do nothing
    }

    /**
     * Interface to manage the export result.
     */
    interface OnExportFinishedListener {
        /**
         * Called when export request finished.
         */
        fun onExportFinished()
    }

    companion object {
        /**
         * Method to get the key in Map with the value and remove this entry.
         *
         * @param map   The Map.
         * @param value The value.
         * @param <T>   First param of the Map.
         * @param <E>   Second param of the Map.
         * @return True, if it has been found and removed. False, if not.
         */
        fun <T, E> getKeyByValueAndRemoveIt(map: MutableMap<T, E>?, value: E): Boolean {
            for ((key, value1) in map!!) {
                if (value == value1) {
                    map.remove(key)
                    return true
                }
            }
            return false
        }
    }
}