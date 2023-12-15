package mega.privacy.android.app.presentation.imagepreview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.contract.SelectFolderToCopyActivityContract
import mega.privacy.android.app.activities.contract.SelectFolderToMoveActivityContract
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.main.dialog.rubbishbin.ConfirmMoveToRubbishBinDialogFragment
import mega.privacy.android.app.modalbottomsheet.nodelabel.NodeLabelBottomSheetDialogFragment
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.fileinfo.FileInfoActivity
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel.Companion.FETCHER_PARAMS
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel.Companion.IMAGE_NODE_FETCHER_SOURCE
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel.Companion.IMAGE_PREVIEW_MENU_OPTIONS
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel.Companion.PARAMS_CURRENT_IMAGE_NODE_ID_VALUE
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewState
import mega.privacy.android.app.presentation.imagepreview.slideshow.SlideshowActivity
import mega.privacy.android.app.presentation.imagepreview.view.ImagePreviewScreen
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.MegaNodeDialogUtil
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.MegaNodeUtil.onNodeTapped
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.mobile.analytics.event.PhotoPreviewSaveToDeviceMenuToolbarEvent
import mega.privacy.mobile.analytics.event.PhotoPreviewScreenEvent
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import java.lang.ref.WeakReference
import javax.inject.Inject

@AndroidEntryPoint
class ImagePreviewActivity : BaseActivity() {

    @Inject
    lateinit var getThemeMode: GetThemeMode

    @Inject
    lateinit var imagePreviewVideoLauncher: ImagePreviewVideoLauncher
    private val selectMoveFolderLauncher: ActivityResultLauncher<LongArray> =
        registerForActivityResult(
            SelectFolderToMoveActivityContract(),
            ::handleMoveFolderResult,
        )
    private val selectCopyFolderLauncher: ActivityResultLauncher<LongArray> =
        registerForActivityResult(
            SelectFolderToCopyActivityContract(),
            ::handleCopyFolderResult,
        )
    private val viewModel: ImagePreviewViewModel by viewModels()
    private val nodeSaver: NodeSaver by lazy {
        NodeSaver(
            activityLauncher = this,
            permissionRequester = this,
            snackbarShower = this,
            confirmDialogShower = AlertsAndWarnings.showSaveToDeviceConfirmDialog(this),
        )
    }
    private val nodeAttacher: MegaAttacher by lazy { MegaAttacher(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Toast.makeText(this, "New Image Preview", Toast.LENGTH_SHORT).show()
        Analytics.tracker.trackEvent(PhotoPreviewScreenEvent)
        if (savedInstanceState != null) {
            nodeSaver.restoreState(savedInstanceState)
            nodeAttacher.restoreState(savedInstanceState)
        }
        setContent {
            val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            MegaAppTheme(isDark = themeMode.isDarkMode()) {
                ImagePreviewScreen(
                    onClickBack = ::finish,
                    onClickVideoPlay = ::playVideo,
                    onClickSlideshow = ::playSlideshow,
                    onClickInfo = ::checkInfo,
                    onClickFavourite = ::favouriteNode,
                    onClickLabel = ::handleLabel,
                    onClickOpenWith = ::handleOpenWith,
                    onClickSaveToDevice = ::saveNodeToDevice,
                    onSwitchAvailableOffline = ::setAvailableOffline,
                    onClickGetLink = ::getNodeLink,
                    onClickSendTo = ::sendNodeToChat,
                    onClickShare = ::shareNode,
                    onClickRename = ::renameNode,
                    onClickMove = ::moveNode,
                    onClickCopy = ::copyNode,
                    onClickMoveToRubbishBin = ::moveNodeToRubbishBin,
                )
            }
        }
        setupFlow()
    }

    private fun setupFlow() {
        viewModel.state
            .onEach(::handleState)
            .flowWithLifecycle(lifecycle, minActiveState = Lifecycle.State.RESUMED)
            .launchIn(lifecycleScope)
    }

    private fun handleState(state: ImagePreviewState) {
        manageCopyMoveException(state.copyMoveException)
    }

    private fun handleMoveFolderResult(result: Pair<LongArray, Long>?) {
        result ?: return
        val (handles, toHandle) = result

        val targetHandle = handles.firstOrNull()
        if (targetHandle == null || targetHandle == MegaApiJava.INVALID_HANDLE || toHandle == MegaApiJava.INVALID_HANDLE) return

        viewModel.moveNode(
            context = this,
            moveHandle = targetHandle,
            toHandle = toHandle,
        )
    }

    private fun handleCopyFolderResult(result: Pair<LongArray, Long>?) {
        result ?: return
        val (handles, toHandle) = result

        val targetHandle = handles.firstOrNull()
        if (targetHandle == null || targetHandle == MegaApiJava.INVALID_HANDLE || toHandle == MegaApiJava.INVALID_HANDLE) return

        viewModel.copyNode(
            context = this,
            copyHandle = targetHandle,
            toHandle = toHandle,
        )
    }

    private fun checkInfo(imageNode: ImageNode) {
        val intent = Intent(this, FileInfoActivity::class.java).apply {
            putExtra(Constants.HANDLE, imageNode.id.longValue)
            putExtra(Constants.NAME, imageNode.name)
        }
        startActivity(intent)
    }

    private fun favouriteNode(imageNode: ImageNode) {
        viewModel.favouriteNode(imageNode)
    }

    private fun handleLabel(imageNode: ImageNode) {
        NodeLabelBottomSheetDialogFragment.newInstance(imageNode.id.longValue)
            .show(supportFragmentManager, TAG)
    }

    private fun handleOpenWith(imageNode: ImageNode) {
        onNodeTapped(
            this,
            MegaNode.unserialize(imageNode.serializedData),
            this::saveNodeByOpenWith,
            this,
            this,
            true
        )
    }

    private fun saveNodeToDevice(imageNode: ImageNode) {
        Analytics.tracker.trackEvent(PhotoPreviewSaveToDeviceMenuToolbarEvent)
        viewModel.executeTransfer(transferMessage = getString(R.string.resume_paused_transfers_text)) {
            saveNode(MegaNode.unserialize(imageNode.serializedData))
        }
    }

    private fun setAvailableOffline(checked: Boolean, imageNode: ImageNode) {
        viewModel.setNodeAvailableOffline(
            activity = WeakReference(this@ImagePreviewActivity),
            setOffline = checked,
            imageNode = imageNode
        )
    }

    private fun sendNodeToChat(imageNode: ImageNode) {
        nodeAttacher.attachNode(imageNode.id.longValue)
    }

    private fun getNodeLink(imageNode: ImageNode) {
        LinksUtil.showGetLinkActivity(this, imageNode.id.longValue)
    }

    private fun shareNode(imageNode: ImageNode) {
        MegaNodeUtil.shareNode(this, MegaNode.unserialize(imageNode.serializedData))
    }

    private fun renameNode(imageNode: ImageNode) {
        val node = MegaNode.unserialize(imageNode.serializedData)
        MegaNodeDialogUtil.showRenameNodeDialog(this, node, this, null)
    }

    private fun moveNode(imageNode: ImageNode) {
        selectMoveFolderLauncher.launch(longArrayOf(imageNode.id.longValue))
    }

    private fun copyNode(imageNode: ImageNode) {
        selectCopyFolderLauncher.launch(longArrayOf(imageNode.id.longValue))
    }

    private fun moveNodeToRubbishBin(imageNode: ImageNode) {
        ConfirmMoveToRubbishBinDialogFragment.newInstance(listOf(imageNode.id.longValue))
            .show(
                supportFragmentManager,
                ConfirmMoveToRubbishBinDialogFragment.TAG
            )
    }

    private fun playSlideshow() {
        val intent = Intent(this, SlideshowActivity::class.java)
        intent.putExtras(this@ImagePreviewActivity.intent)
        startActivity(intent)
    }

    private fun playVideo(imageNode: ImageNode) {
        lifecycleScope.launch {
            imagePreviewVideoLauncher.launchVideoScreen(
                imageNode = imageNode,
                context = this@ImagePreviewActivity,
            )
        }
    }

    private fun saveNode(node: MegaNode) {
        PermissionUtils.checkNotificationsPermission(this)
        nodeSaver.saveNode(
            node,
            highPriority = false,
            isFolderLink = node.isForeign,
            fromMediaViewer = true,
            needSerialize = true,
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        nodeSaver.saveState(outState)
        nodeAttacher.saveState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        when {
            nodeSaver.handleActivityResult(this, requestCode, resultCode, intent) ->
                return

            nodeAttacher.handleActivityResult(requestCode, resultCode, intent, this) ->
                return

            else -> super.onActivityResult(requestCode, resultCode, intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        nodeSaver.handleRequestPermissionsResult(requestCode)
    }

    override fun onDestroy() {
        nodeSaver.destroy()
        super.onDestroy()
    }

    /**
     * Upon a node is open with, if it cannot be previewed in-app,
     * then download it first, this download will be marked as "download by open with".
     *
     * @param node Node to be downloaded.
     */
    private fun saveNodeByOpenWith(node: MegaNode) {
        PermissionUtils.checkNotificationsPermission(this)
        nodeSaver.saveNodes(
            nodes = listOf(node),
            highPriority = true,
            isFolderLink = false,
            fromMediaViewer = false,
            needSerialize = false,
            downloadForPreview = true,
            downloadByOpenWith = true
        )
    }

    companion object {
        private const val TAG = "ImagePreviewActivity"

        fun createIntent(
            context: Context,
            imageSource: ImagePreviewFetcherSource,
            menuOptionsSource: ImagePreviewMenuSource,
            anchorImageNodeId: NodeId,
            params: Map<String, Any> = mapOf(),
        ): Intent {
            return Intent(context, ImagePreviewActivity::class.java).apply {
                putExtra(IMAGE_NODE_FETCHER_SOURCE, imageSource)
                putExtra(IMAGE_PREVIEW_MENU_OPTIONS, menuOptionsSource)
                putExtra(PARAMS_CURRENT_IMAGE_NODE_ID_VALUE, anchorImageNodeId.longValue)
                putExtra(FETCHER_PARAMS, bundleOf(*params.toList().toTypedArray()))
            }
        }
    }
}