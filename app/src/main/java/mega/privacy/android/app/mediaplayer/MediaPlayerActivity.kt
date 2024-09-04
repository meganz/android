package mega.privacy.android.app.mediaplayer

import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.navigation.NavController
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.interfaces.showSnackbarWithChat
import mega.privacy.android.app.presentation.transfers.attach.NodeAttachmentViewModel
import mega.privacy.android.app.presentation.transfers.attach.createNodeAttachmentView
import mega.privacy.android.app.presentation.transfers.starttransfer.StartDownloadViewModel
import mega.privacy.android.app.presentation.transfers.starttransfer.view.createStartTransferView
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

/**
 * Media player Activity
 */
@AndroidEntryPoint
abstract class MediaPlayerActivity : PasscodeActivity() {
    internal val viewModel: MediaPlayerViewModel by viewModels()
    internal val startDownloadViewModel: StartDownloadViewModel by viewModels()
    internal val nodeAttachmentViewModel: NodeAttachmentViewModel by viewModels()

    internal var searchMenuItem: MenuItem? = null
    internal var optionsMenu: Menu? = null

    internal lateinit var navController: NavController

    protected fun addStartDownloadTransferView(root: ViewGroup) {
        root.addView(
            createStartTransferView(
                this,
                startDownloadViewModel.state,
                startDownloadViewModel::consumeDownloadEvent
            )
        )
    }

    protected fun addNodeAttachmentView(root: ViewGroup) {
        root.addView(
            createNodeAttachmentView(
                activity = this,
                viewModel = nodeAttachmentViewModel,
                showMessage = { message, id ->
                    showSnackbarWithChat(message, id)
                }
            )
        )
    }

    internal val selectImportFolderLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val toHandle = result.data?.getLongExtra(INTENT_EXTRA_KEY_IMPORT_TO, INVALID_HANDLE)
            if (toHandle != null) {
                viewModel.importChatNode(
                    chatId = getChatId(),
                    messageId = getMessageId(),
                    newParentHandle = NodeId(toHandle)
                )
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
        val chatId = getChatId()
        val msgId = getMessageId()

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

    internal fun getChatId() = intent.getLongExtra(INTENT_EXTRA_KEY_CHAT_ID, INVALID_HANDLE)
    internal fun getMessageId() = intent.getLongExtra(INTENT_EXTRA_KEY_MSG_ID, INVALID_HANDLE)

    internal fun saveChatNode() {
        val (chatId, message) = getChatMessage()
        startDownloadViewModel.onDownloadClicked(chatId, message?.msgId ?: INVALID_HANDLE)
    }

    internal fun saveFileLinkNode(serializedNode: String) {
        startDownloadViewModel.onDownloadClicked(serializedNode)
    }

    internal fun saveNodeFromFolderLink(nodeId: NodeId) {
        startDownloadViewModel.onFolderLinkChildNodeDownloadClicked(nodeId)
    }

    internal fun saveFromAlbumSharing(nodeId: NodeId) {
        viewModel.getNodeForAlbumSharing(nodeId.longValue)?.let { node ->
            startDownloadViewModel.onDownloadClicked(node.serialize())
        }
    }

    internal fun saveNode(nodeId: NodeId) {
        startDownloadViewModel.onDownloadClicked(nodeId)
    }

    /**
     * Close search mode
     */
    fun closeSearch() {
        searchMenuItem?.collapseActionView()
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

        /**
         * The previous type of media item
         */
        const val TYPE_PREVIOUS = 1

        /**
         * The playing type playing media item
         */
        const val TYPE_PLAYING = 2

        /**
         * The next type next media item
         */
        const val TYPE_NEXT = 3
    }
}
