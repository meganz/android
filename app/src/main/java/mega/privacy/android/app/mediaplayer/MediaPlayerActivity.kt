package mega.privacy.android.app.mediaplayer

import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.main.controllers.ChatController
import mega.privacy.android.app.presentation.transfers.startdownload.StartDownloadViewModel
import mega.privacy.android.app.presentation.transfers.startdownload.view.createStartDownloadView
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CHAT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_COPY_HANDLES
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_COPY_TO
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IMPORT_TO
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MOVE_HANDLES
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MOVE_TO
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MSG_ID
import mega.privacy.android.domain.entity.node.NodeId
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaNode
import timber.log.Timber

/**
 * Media player Activity
 */
@AndroidEntryPoint
abstract class MediaPlayerActivity : PasscodeActivity() {
    internal val viewModel: MediaPlayerViewModel by viewModels()
    internal val startDownloadViewModel: StartDownloadViewModel by viewModels()

    internal var searchMenuItem: MenuItem? = null
    internal var optionsMenu: Menu? = null

    internal lateinit var navController: NavController

    internal val nodeAttacher by lazy { MegaAttacher(this) }
    private var currentRequestCode: Int? = null

    internal val nodeSaver by lazy {
        NodeSaver(
            activityLauncher = this,
            permissionRequester = this,
            snackbarShower = this,
            confirmDialogShower = AlertsAndWarnings.showSaveToDeviceConfirmDialog(this)
        )
    }

    protected fun addStartDownloadTransferView(root: ViewGroup) {
        root.addView(
            createStartDownloadView(
                this,
                startDownloadViewModel.state,
                startDownloadViewModel::consumeDownloadEvent
            )
        )
    }

    private val nodeAttacherLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            currentRequestCode?.let {
                if (nodeAttacher.handleActivityResult(
                        requestCode = it,
                        resultCode = result.resultCode,
                        data = result.data,
                        snackbarShower = this
                    )
                ) {
                    currentRequestCode = null
                }
            }

        }

    private val nodeSaverLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            currentRequestCode?.let {
                if (nodeSaver.handleActivityResult(
                        activity = this,
                        requestCode = it,
                        resultCode = result.resultCode,
                        intent = result.data
                    )
                ) {
                    currentRequestCode = null
                }
            }
        }

    internal val selectImportFolderLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val node = getChatMessageNode()
            val toHandle = result.data?.getLongExtra(INTENT_EXTRA_KEY_IMPORT_TO, INVALID_HANDLE)
            if (node != null && toHandle != null) {
                viewModel.importNode(node = node, newParentHandle = toHandle)
            }
        }

    internal val selectFolderToMoveLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val moveHandles = result.data?.getLongArrayExtra(INTENT_EXTRA_KEY_MOVE_HANDLES)
            val toHandle = result.data?.getLongExtra(INTENT_EXTRA_KEY_MOVE_TO, INVALID_HANDLE)
            if (moveHandles != null && toHandle != null)
                viewModel.moveNode(
                    nodeHandle = moveHandles[0],
                    newParentHandle = toHandle,
                )
        }

    internal val selectFolderToCopyLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val copyHandles = result.data?.getLongArrayExtra(INTENT_EXTRA_KEY_COPY_HANDLES)
            val toHandle = result.data?.getLongExtra(INTENT_EXTRA_KEY_COPY_TO, INVALID_HANDLE)
            if (copyHandles != null && toHandle != null) {
                viewModel.copyNode(
                    nodeHandle = copyHandles[0],
                    newParentHandle = toHandle,
                )
            }
        }

    /**
     * Get chat id and chat message from the launch intent.
     *
     * @return first is chat id, second is chat message
     */
    internal fun getChatMessage(): Pair<Long, MegaChatMessage?> {
        val chatId = intent.getLongExtra(INTENT_EXTRA_KEY_CHAT_ID, INVALID_HANDLE)
        val msgId = intent.getLongExtra(INTENT_EXTRA_KEY_MSG_ID, INVALID_HANDLE)

        if (chatId == INVALID_HANDLE || msgId == INVALID_HANDLE) {
            return Pair(chatId, null)
        }

        return Pair(
            first = chatId,
            second = megaChatApi.getMessage(chatId, msgId)
                ?: megaChatApi.getMessageFromNodeHistory(
                    chatId,
                    msgId
                )
        )
    }

    /**
     * Get chat message node from the launch intent.
     *
     * @return chat message node
     */
    internal fun getChatMessageNode(): MegaNode? {
        val pair = getChatMessage()
        val message = pair.second ?: return null

        return ChatController(this).authorizeNodeIfPreview(
            message.megaNodeList.get(0),
            megaChatApi.getChatRoom(pair.first)
        )
    }

    internal fun saveChatNode() {
        lifecycleScope.launch {
            if (startDownloadViewModel.shouldDownloadWithDownloadWorker()) {
                val (chatId, message) = getChatMessage()
                startDownloadViewModel.onDownloadClicked(chatId, message?.msgId ?: INVALID_HANDLE)
            } else {
                getChatMessageNode()?.let { node ->
                    nodeSaver.saveNode(
                        node = node,
                        highPriority = true,
                        fromMediaViewer = true,
                        needSerialize = true
                    )
                }
            }
        }
    }

    internal fun saveFileLinkNode(serializedNode: String) {
        lifecycleScope.launch {
            if (startDownloadViewModel.shouldDownloadWithDownloadWorker()) {
                startDownloadViewModel.onDownloadClicked(serializedNode)
            } else {
                MegaNode.unserialize(serializedNode)
                    ?.let { currentDocument ->
                        Timber.d("currentDocument NOT NULL")
                        nodeSaver.saveNode(
                            currentDocument,
                            isFolderLink = false,
                            fromMediaViewer = true,
                            needSerialize = true
                        )
                    } ?: Timber.w("currentDocument is NULL")
            }
        }
    }

    internal fun saveNodeFromFolderLink(nodeId: NodeId) {
        lifecycleScope.launch {
            if (startDownloadViewModel.shouldDownloadWithDownloadWorker()) {
                startDownloadViewModel.onFolderLinkChildNodeDownloadClicked(nodeId)
            } else {
                nodeSaver.saveHandle(
                    handle = nodeId.longValue,
                    isFolderLink = true,
                    fromMediaViewer = true
                )
            }
        }
    }

    internal fun saveFromAlbumSharing(nodeId: NodeId) {
        viewModel.getNodeForAlbumSharing(nodeId.longValue)?.let { node ->
            lifecycleScope.launch {
                if (startDownloadViewModel.shouldDownloadWithDownloadWorker()) {
                    startDownloadViewModel.onDownloadClicked(node.serialize())
                } else {
                    nodeSaver.saveNode(
                        node = node,
                        fromMediaViewer = true,
                        needSerialize = true
                    )
                }
            }
        }
    }

    internal fun saveNode(nodeId: NodeId) {
        lifecycleScope.launch {
            if (startDownloadViewModel.shouldDownloadWithDownloadWorker()) {
                startDownloadViewModel.onDownloadClicked(nodeId)
            } else {
                nodeSaver.saveHandle(
                    handle = nodeId.longValue,
                    isFolderLink = false,
                    fromMediaViewer = true
                )
            }
        }
    }

    /**
     * Close search mode
     */
    fun closeSearch() {
        searchMenuItem?.collapseActionView()
    }

    /**
     * launch activity for result
     *
     * @param intent
     * @param requestCode
     */
    override fun launchActivityForResult(intent: Intent, requestCode: Int) {
        currentRequestCode = requestCode
        if (currentRequestCode == Constants.REQUEST_CODE_SELECT_CHAT) {
            nodeAttacherLauncher
        } else {
            nodeSaverLauncher
        }.launch(intent)
    }

    internal abstract fun setupToolbar()

    /**
     * Set toolbar title
     *
     * @param title toolbar title
     */
    internal abstract fun setToolbarTitle(title: String)

    /**
     * Hide tool bar
     *
     * @param animate true is that adds hide animation, otherwise is false
     */
    abstract fun hideToolbar(animate: Boolean = true)

    /**
     * Show tool bar
     *
     * @param animate true is that adds show animation, otherwise is false
     */
    abstract fun showToolbar(animate: Boolean = true)

    /**
     * Set draggable
     *
     * @param draggable ture is draggable, otherwise is false
     */
    abstract fun setDraggable(draggable: Boolean)

    /**
     * Show system UI
     */
    abstract fun showSystemUI()

    /**
     * Setup toolbar colors
     *
     * @param showElevation true is show elevation, otherwise is false.
     */
    abstract fun setupToolbarColors(showElevation: Boolean = false)

    companion object {

        /**
         * The zero value for toolbar elevation
         */
        const val TOOLBAR_ELEVATION_ZERO = 0F

        /**
         * The zero value for translation Y
         */
        const val TRANSLATION_Y_ZERO = 0F

        internal const val TIMEOUT_FOR_DEFAULT_MENU_ITEM: Long = 100
        internal const val TIMEOUT_FOR_SHARED_MENU_ITEM: Long = 500
    }
}
