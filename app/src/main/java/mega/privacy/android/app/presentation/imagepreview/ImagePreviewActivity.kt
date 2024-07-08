package mega.privacy.android.app.presentation.imagepreview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import mega.privacy.android.app.activities.contract.SelectFolderToImportActivityContract
import mega.privacy.android.app.activities.contract.SelectFolderToMoveActivityContract
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.modalbottomsheet.nodelabel.NodeLabelBottomSheetDialogFragment
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.fileinfo.FileInfoActivity
import mega.privacy.android.app.presentation.hidenode.HiddenNodesOnboardingActivity
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel.Companion.FETCHER_PARAMS
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel.Companion.IMAGE_NODE_FETCHER_SOURCE
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel.Companion.IMAGE_PREVIEW_IS_FOREIGN
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel.Companion.IMAGE_PREVIEW_MENU_OPTIONS
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel.Companion.PARAMS_CURRENT_IMAGE_NODE_ID_VALUE
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewState
import mega.privacy.android.app.presentation.imagepreview.slideshow.SlideshowActivity
import mega.privacy.android.app.presentation.imagepreview.view.ImagePreviewScreen
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.MegaNodeDialogUtil
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.MegaNodeUtil.onNodeTapped
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.mobile.analytics.event.PhotoPreviewSaveToDeviceMenuToolbarEvent
import mega.privacy.mobile.analytics.event.PhotoPreviewScreenEvent
import mega.privacy.mobile.analytics.event.PlaySlideshowMenuToolbarEvent
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import javax.inject.Inject

@AndroidEntryPoint
class ImagePreviewActivity : BaseActivity() {
    @Inject
    lateinit var getThemeMode: GetThemeMode

    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

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

    private val selectImportFolderLauncher: ActivityResultLauncher<LongArray> =
        registerForActivityResult(
            SelectFolderToImportActivityContract(),
            ::handleImportFolderResult
        )

    private val hiddenNodesOnboardingLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::handleHiddenNodesOnboardingResult,
        )

    private val viewModel: ImagePreviewViewModel by viewModels()
    private val nodeAttacher: MegaAttacher by lazy { MegaAttacher(this) }

    private var tempNodeId: NodeId? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Analytics.tracker.trackEvent(PhotoPreviewScreenEvent)
        if (savedInstanceState != null) {
            nodeAttacher.restoreState(savedInstanceState)
        }
        setContent {
            val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                PasscodeContainer(
                    passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                    content = {
                        ImagePreviewScreen(
                            onClickBack = ::finish,
                            onClickVideoPlay = ::playVideo,
                            onClickSlideshow = ::playSlideshow,
                            onClickInfo = ::checkInfo,
                            onClickFavourite = ::favouriteNode,
                            onClickLabel = ::handleLabel,
                            onClickOpenWith = ::handleOpenWith,
                            onClickSaveToDevice = ::saveNodeToDevice,
                            onClickImport = ::importNode,
                            onSwitchAvailableOffline = ::setAvailableOffline,
                            onClickGetLink = ::getNodeLink,
                            onClickSendTo = ::sendNodeToChat,
                            onClickShare = ::shareNode,
                            onClickRename = ::renameNode,
                            onClickHide = ::hideNode,
                            onClickHideHelp = ::showHiddenNodesOnboarding,
                            onClickUnhide = ::unhideNode,
                            onClickMove = ::moveNode,
                            onClickCopy = ::copyNode,
                            onClickRestore = ::restoreNode,
                            onClickRemove = ::removeNode,
                            onClickMoveToRubbishBin = ::moveNodeToRubbishBin,
                        )
                    }
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

    private fun handleImportFolderResult(result: Pair<LongArray, Long>?) {
        result ?: return
        val (handles, toHandle) = result

        val targetHandle = handles.firstOrNull()
        if (targetHandle != null && targetHandle != MegaApiJava.INVALID_HANDLE && toHandle != MegaApiJava.INVALID_HANDLE) {
            viewModel.importNode(
                context = this,
                importHandle = targetHandle,
                toHandle = toHandle,
            )
        }
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
            { this.saveNodeByOpenWith() },
            this,
            this,
            true
        )
    }

    private fun saveNodeToDevice(imageNode: ImageNode) {
        Analytics.tracker.trackEvent(PhotoPreviewSaveToDeviceMenuToolbarEvent)
        viewModel.executeTransfer(transferMessage = getString(R.string.resume_paused_transfers_text))
    }

    private fun importNode(imageNode: ImageNode) {
        selectImportFolderLauncher.launch(longArrayOf(imageNode.id.longValue))
    }

    private fun setAvailableOffline(checked: Boolean, imageNode: ImageNode) {
        viewModel.setNodeAvailableOffline(
            setOffline = checked,
            imageNode = imageNode
        )
    }

    private fun sendNodeToChat(imageNode: ImageNode) {
        nodeAttacher.attachNode(MegaNode.unserialize(imageNode.serializedData))
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

    private fun hideNode(
        imageNode: ImageNode,
        accountDetail: AccountDetail?,
        isHiddenNodesOnboarded: Boolean?,
    ) {
        val isPaid = accountDetail?.levelDetail?.accountType?.isPaid ?: false
        val isHiddenNodesOnboarded = isHiddenNodesOnboarded ?: false

        if (!isPaid) {
            val intent = HiddenNodesOnboardingActivity.createScreen(
                context = this,
                isOnboarding = false,
            )
            hiddenNodesOnboardingLauncher.launch(intent)
            overridePendingTransition(0, 0)
        } else if (isHiddenNodesOnboarded) {
            viewModel.hideNode(nodeId = imageNode.id)
        } else {
            tempNodeId = imageNode.id
            showHiddenNodesOnboarding()
        }
    }

    private fun showHiddenNodesOnboarding() {
        viewModel.setHiddenNodesOnboarded()

        val intent = HiddenNodesOnboardingActivity.createScreen(
            context = this,
            isOnboarding = true,
        )
        hiddenNodesOnboardingLauncher.launch(intent)
        overridePendingTransition(0, 0)
    }

    private fun unhideNode(imageNode: ImageNode) {
        viewModel.unhideNode(nodeId = imageNode.id)
    }

    private fun moveNode(imageNode: ImageNode) {
        selectMoveFolderLauncher.launch(longArrayOf(imageNode.id.longValue))
    }

    private fun copyNode(imageNode: ImageNode) {
        selectCopyFolderLauncher.launch(longArrayOf(imageNode.id.longValue))
    }

    private fun restoreNode(imageNode: ImageNode) {
        val nodeId = imageNode.id
        val restoreId = imageNode.restoreId ?: return

        viewModel.moveNode(
            context = this,
            moveHandle = nodeId.longValue,
            toHandle = restoreId.longValue,
        )
    }

    private fun removeNode(imageNode: ImageNode) {
        viewModel.deleteNode(nodeId = imageNode.id)
    }

    private fun moveNodeToRubbishBin(imageNode: ImageNode) {
        viewModel.moveToRubbishBin(imageNode.id)
    }

    private fun playSlideshow() {
        Analytics.tracker.trackEvent(PlaySlideshowMenuToolbarEvent)
        val intent = Intent(this, SlideshowActivity::class.java)
        intent.putExtras(this@ImagePreviewActivity.intent)
        startActivity(intent)
    }

    private fun playVideo(imageNode: ImageNode) {
        lifecycleScope.launch {
            viewModel.playVideo(
                this@ImagePreviewActivity,
                imageNode = imageNode,
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        nodeAttacher.saveState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        when {
            nodeAttacher.handleActivityResult(requestCode, resultCode, intent, this) ->
                return

            else -> super.onActivityResult(requestCode, resultCode, intent)
        }
    }

    /**
     * Upon a node is open with, if it cannot be previewed in-app,
     * then download it first, this download will be marked as "download by open with".
     *
     */
    private fun saveNodeByOpenWith() {
        viewModel.executeTransfer(
            transferMessage = getString(R.string.resume_paused_transfers_text),
            downloadForPreview = true,
        )
    }

    private fun handleHiddenNodesOnboardingResult(result: ActivityResult) {
        if (result.resultCode != Activity.RESULT_OK) return
        val nodeId = tempNodeId ?: return

        viewModel.hideNode(nodeId)

        val message = resources.getQuantityString(R.plurals.hidden_nodes_result_message, 1, 1)
        viewModel.setResultMessage(message)
    }

    companion object {
        private const val TAG = "ImagePreviewActivity"

        fun createIntent(
            context: Context,
            imageSource: ImagePreviewFetcherSource,
            menuOptionsSource: ImagePreviewMenuSource,
            anchorImageNodeId: NodeId? = null,
            params: Map<String, Any> = mapOf(),
            isForeign: Boolean = false,
        ): Intent {
            return Intent(context, ImagePreviewActivity::class.java).apply {
                putExtra(IMAGE_NODE_FETCHER_SOURCE, imageSource)
                putExtra(IMAGE_PREVIEW_MENU_OPTIONS, menuOptionsSource)
                putExtra(PARAMS_CURRENT_IMAGE_NODE_ID_VALUE, anchorImageNodeId?.longValue)
                putExtra(FETCHER_PARAMS, bundleOf(*params.toList().toTypedArray()))
                putExtra(IMAGE_PREVIEW_IS_FOREIGN, isForeign)
            }
        }

        fun createSecondaryIntent(
            context: Context,
            imageSource: ImagePreviewFetcherSource,
            menuOptionsSource: ImagePreviewMenuSource,
            anchorImageNodeId: Long? = null,
            params: Map<String, Any> = mapOf(),
            isForeign: Boolean = false,
        ): Intent {
            return createIntent(
                context = context,
                imageSource = imageSource,
                menuOptionsSource = menuOptionsSource,
                anchorImageNodeId = anchorImageNodeId?.let { NodeId(it) },
                params = params,
                isForeign = isForeign,
            )
        }
    }
}