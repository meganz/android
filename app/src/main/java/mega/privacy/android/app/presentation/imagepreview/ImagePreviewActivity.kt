package mega.privacy.android.app.presentation.imagepreview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity.Companion.SCALE
import com.yalantis.ucrop.view.OverlayView.Companion.FREESTYLE_CROP_MODE_ENABLE_WITH_PASS_THROUGH
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.snackbar.MegaSnackbar
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.OfflineFileInfoActivity
import mega.privacy.android.app.activities.contract.NameCollisionActivityContract
import mega.privacy.android.app.activities.contract.SelectFolderToCopyActivityContract
import mega.privacy.android.app.activities.contract.SelectFolderToImportActivityContract
import mega.privacy.android.app.activities.contract.SelectFolderToMoveActivityContract
import mega.privacy.android.app.appstate.content.navigation.LegacyActivityScaffold
import mega.privacy.android.app.appstate.content.navigation.NavigationResultManager
import mega.privacy.android.app.components.largebundle.largeBundleHolder
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.modalbottomsheet.nodelabel.NodeLabelBottomSheetDialogFragmentFactory
import mega.privacy.android.app.presentation.container.SharedAppContainer
import mega.privacy.android.app.presentation.extensions.getStorageState
import mega.privacy.android.app.presentation.fileinfo.FileInfoActivity
import mega.privacy.android.app.presentation.hidenode.HiddenNodesOnboardingActivity
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel.Companion.FETCHER_PARAMS
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel.Companion.IMAGE_NODE_FETCHER_SOURCE
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel.Companion.IMAGE_PREVIEW_ADD_TO_ALBUM
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel.Companion.IMAGE_PREVIEW_IS_FOREIGN
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel.Companion.IMAGE_PREVIEW_MENU_OPTIONS
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel.Companion.PARAMS_CURRENT_IMAGE_NODE_ID_VALUE
import mega.privacy.android.app.presentation.imagepreview.fetcher.CloudDriveImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.FavouriteImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.PublicFileImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.RubbishBinImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.SharedItemsImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewState
import mega.privacy.android.app.presentation.imagepreview.slideshow.SlideshowActivity
import mega.privacy.android.app.presentation.imagepreview.view.ImagePreviewScreen
import mega.privacy.android.app.presentation.offline.action.HandleOfflineNodeActions
import mega.privacy.android.app.presentation.photos.albums.add.AddToAlbumActivity
import mega.privacy.android.app.presentation.transfers.attach.NodeAttachmentView
import mega.privacy.android.app.presentation.transfers.attach.NodeAttachmentViewModel
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.MegaNodeDialogUtil
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.MegaNodeUtil.onNodeTapped
import mega.privacy.android.core.nodecomponents.components.offline.OfflineNodeActionsViewModel
import mega.privacy.android.core.sharedcomponents.extension.isDarkMode
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.node.ExportNodeUseCase
import mega.privacy.android.domain.usecase.node.NodeExistsInCurrentLocationUseCase
import mega.privacy.android.domain.usecase.node.RenameNodeUseCase
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.transition.opaqueFadeBackwardTransition
import mega.privacy.android.navigation.contract.transition.opaqueFadeForwardTransition
import mega.privacy.android.navigation.destination.VideoEditorScreenNavKey
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.ImagePreviewGetLinkMenuItemEvent
import mega.privacy.mobile.analytics.event.PhotoEditorMenuItemEvent
import mega.privacy.mobile.analytics.event.PhotoPreviewSaveToDeviceMenuToolbarEvent
import mega.privacy.mobile.analytics.event.PhotoPreviewScreenEvent
import mega.privacy.mobile.analytics.event.PlaySlideshowMenuToolbarEvent
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Activity to view an image node
 */
@AndroidEntryPoint
class ImagePreviewActivity : BaseActivity() {
    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    @Inject
    lateinit var navigationResultManager: NavigationResultManager

    @Inject
    lateinit var featureDestinations: Set<@JvmSuppressWildcards FeatureDestination>

    @Inject
    lateinit var nodeLabelBottomSheetDialogFragmentFactory: NodeLabelBottomSheetDialogFragmentFactory

    @Inject
    lateinit var exportNodeUseCase: ExportNodeUseCase

    @Inject
    lateinit var getRootNodeUseCase: GetRootNodeUseCase

    @Inject
    lateinit var nodeExistsInCurrentLocationUseCase: NodeExistsInCurrentLocationUseCase

    @Inject
    lateinit var renameNodeUseCase: RenameNodeUseCase

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

    private val nameCollisionActivityLauncher = registerForActivityResult(
        NameCollisionActivityContract()
    ) { result ->
        result?.let {
            showSnackbar(SNACKBAR_TYPE, it, INVALID_HANDLE)
        }
    }

    private val addToAlbumLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::handleAddToAlbumResult,
        )

    private val viewModel: ImagePreviewViewModel by viewModels()
    private val nodeAttachmentViewModel: NodeAttachmentViewModel by viewModels()
    private val offlineNodeActionsViewModel: OfflineNodeActionsViewModel by viewModels()

    private var tempNodeId: NodeId? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        setupImmersiveMode()
        super.onCreate(savedInstanceState)
        Analytics.tracker.trackEvent(PhotoPreviewScreenEvent)
        setContent {
            val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val uiState by viewModel.state.collectAsStateWithLifecycle()
            val isFromLinkWithoutLogin = uiState.isFromLink && !uiState.isLoggedIn
            // Host a nav3 scaffold so the viewer can navigate to other destinations (e.g. the video editor)
            LegacyActivityScaffold(
                container = { content ->
                    SharedAppContainer(
                        themeMode = themeMode,
                        isSessionRequired = !isFromLinkWithoutLogin,
                        useLegacyStatusBarColor = false,
                        finishOnSessionRefresh = false,
                        content = content,
                    )
                },
                initialKey = ImagePreviewNavKey,
                navigationResultManager = navigationResultManager,
                featureDestinations = featureDestinations,
                onEmptyBackStack = ::finish,
                // The Activity window is translucent (to support flick-to-dismiss), so the default
                // crossfade would briefly reveal the Activity behind it. These keep the screen
                // underneath opaque and only fade the top-most entry.
                transitionSpec = { opaqueFadeForwardTransition },
                popTransitionSpec = { opaqueFadeBackwardTransition },
            ) { navigationHandler, _ ->
                entry<ImagePreviewNavKey> {
                    ImagePreviewContent(
                        onEditVideo = { imageNode ->
                            navigationHandler.navigate(VideoEditorScreenNavKey(imageNode.id.longValue))
                        },
                    )
                }
            }
        }
        setupFlow()
    }

    @Composable
    private fun ImagePreviewContent(onEditVideo: (ImageNode) -> Unit) {
        val themeMode by monitorThemeModeUseCase()
            .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
        val isDarkMode = themeMode.isDarkMode()
        val snackbarHostState = remember { SnackbarHostState() }
        val systemUiController = rememberSystemUiController()
        LaunchedEffect(systemUiController, isDarkMode) {
            systemUiController.setSystemBarsColor(
                color = Color.Transparent,
                darkIcons = !isDarkMode,
            )
        }
        val uiState by viewModel.state.collectAsStateWithLifecycle()

        OriginalTheme(isDark = isDarkMode) {
            ImagePreviewScreen(
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                onClickBack = ::finish,
                onClickEdit = { imageNode ->
                    if (imageNode.type is VideoFileTypeInfo) {
                        // TODO add analytics event
                        onEditVideo(imageNode)
                    } else {
                        Analytics.tracker.trackEvent(PhotoEditorMenuItemEvent)
                        viewModel.launchPhotoEditor(imageNode)
                    }
                },
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
                onClickSendTo = {
                    nodeAttachmentViewModel.startAttachNodes(listOf(it.id))
                },
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
                onClickAddToAlbum = ::addToAlbum,
            )

            NodeAttachmentView(
                viewModel = nodeAttachmentViewModel,
                snackbarHostState = snackbarHostState,
            )
            HandleOfflineNodeActions(
                viewModel = offlineNodeActionsViewModel,
                snackBarHostState = snackbarHostState,
                coroutineScope = rememberCoroutineScope(),
            )

            // Render snackbar from Nav3 components, e.g. LegacyActivityScaffold > StartTransferComponent
            LocalSnackBarHostState.current?.let { hostState ->
                Box(
                    modifier = Modifier.fillMaxSize().systemBarsPadding(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    MegaSnackbar(hostState)
                }
            }
        }
        EventEffect(
            event = uiState.openPhotoEditorEvent,
            onConsumed = { viewModel.onOpenPhotoEditorEventConsumed() },
        ) { (imageNode, imagePath) ->
            openPhotoEditor(imagePath.toUri(), imageNode.name)
        }
    }

    override fun shouldSetStatusBarTextColor() = false

    private fun setupImmersiveMode() {
        // Apply for Android version 10 or greater
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Draw behind display cutouts.
            window.attributes.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS

            // No scrim behind transparent navigation bar.
            window.setFlags(FLAG_LAYOUT_NO_LIMITS, FLAG_LAYOUT_NO_LIMITS)

            // System bars use fade by default to hide/show. Make them slide instead.
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun setupFlow() {
        viewModel.state
            .onEach(::handleState)
            .flowWithLifecycle(lifecycle, minActiveState = Lifecycle.State.RESUMED)
            .launchIn(lifecycleScope)
    }

    private fun handleState(state: ImagePreviewState) {
        manageCopyMoveException(state.copyMoveException)
        manageNameCollision(state.nameCollision)
    }

    private fun manageNameCollision(nameCollision: NameCollision?) {
        nameCollision ?: return
        nameCollisionActivityLauncher.launch(arrayListOf(nameCollision))
        viewModel.onNameCollisionConsumed()
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
        if (viewModel.isInOfflineMode()) {
            Intent(this, OfflineFileInfoActivity::class.java).apply {
                putExtra(Constants.HANDLE, imageNode.id.longValue.toString())
            }.apply {
                startActivity(this)
            }
        } else {
            Intent(this, FileInfoActivity::class.java).apply {
                putExtra(Constants.HANDLE, imageNode.id.longValue)
                putExtra(Constants.NAME, imageNode.name)
            }.apply {
                startActivity(this)
            }
        }
    }

    private fun favouriteNode(imageNode: ImageNode) {
        viewModel.favouriteNode(imageNode)
    }

    private fun handleLabel(imageNode: ImageNode) {
        lifecycleScope.launch {
            nodeLabelBottomSheetDialogFragmentFactory.newInstance(imageNode.id.longValue)
                .show(supportFragmentManager, TAG)
        }
    }

    private fun handleOpenWith(imageNode: ImageNode) {
        if (viewModel.isInOfflineMode()) {
            offlineNodeActionsViewModel.handleOpenWithIntentById(imageNode.id)
            return
        }
        lifecycleScope.launch {
            val node = viewModel.resolveMegaNode(imageNode) ?: run {
                Timber.w("handleOpenWith: cannot resolve MegaNode for ${imageNode.id}")
                return@launch
            }
            onNodeTapped(
                this@ImagePreviewActivity,
                node,
                { saveNodeByOpenWith() },
                this@ImagePreviewActivity,
                this@ImagePreviewActivity,
                true
            )
        }
    }

    private fun saveNodeToDevice() {
        Analytics.tracker.trackEvent(PhotoPreviewSaveToDeviceMenuToolbarEvent)
        viewModel.executeTransfer()
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

    private fun getNodeLink(imageNode: ImageNode) {
        Analytics.tracker.trackEvent(ImagePreviewGetLinkMenuItemEvent)
        if (getStorageState() == StorageState.PayWall) {
            showOverDiskQuotaPaywallWarning()
            return
        }
        LinksUtil.showGetLinkActivity(this, imageNode.id.longValue)
    }

    private fun shareNode(imageNode: ImageNode) {
        when {
            viewModel.isInOfflineMode() -> {
                offlineNodeActionsViewModel.handleShareOfflineNodeById(
                    nodeId = imageNode.id,
                    isOnline = false
                )
            }

            viewModel.isFileLink() -> {
                intent.getStringExtra(FETCHER_PARAMS)?.let { paramsKey ->
                    lifecycleScope.launch {
                        largeBundleHolder.get(paramsKey)
                            ?.getString(PublicFileImageNodeFetcher.URL)
                            ?.let { link ->
                                MegaNodeUtil.shareLink(
                                    this@ImagePreviewActivity,
                                    link,
                                    imageNode.name,
                                )
                            }
                    }
                }
            }

            else -> lifecycleScope.launch {
                val node = viewModel.resolveMegaNode(imageNode) ?: run {
                    Timber.w("shareNode: cannot resolve MegaNode for ${imageNode.id}")
                    return@launch
                }
                shareCurrentNode(node)
            }
        }
    }

    private fun shareCurrentNode(node: nz.mega.sdk.MegaNode) {
        val localPath = FileUtil.getLocalFile(node)
        if (!localPath.isNullOrBlank() && !node.isFolder) {
            FileUtil.shareFile(this, File(localPath), node.name)
            return
        }
        if (node.isExported) {
            val intent = Intent(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_SUBJECT, node.name)
            MegaNodeUtil.startShareIntent(this, intent, node.publicLink, node.name)
            return
        }
        lifecycleScope.launch {
            runCatching {
                exportNodeUseCase(
                    nodeToExport = NodeId(node.handle),
                    callerName = "ImagePreviewActivity:share",
                )
            }.onSuccess { link ->
                val intent = Intent(Intent.ACTION_SEND)
                    .putExtra(Intent.EXTRA_SUBJECT, node.name)
                MegaNodeUtil.startShareIntent(
                    this@ImagePreviewActivity,
                    intent,
                    link,
                    node.name,
                )
            }.onFailure { Timber.e(it) }
        }
    }

    private fun renameNode(imageNode: ImageNode) {
        lifecycleScope.launch {
            val node = viewModel.resolveMegaNode(imageNode) ?: run {
                Timber.w("renameNode: cannot resolve MegaNode for ${imageNode.id}")
                return@launch
            }
            val snackbarShower = object : SnackbarShower {
                override fun showSnackbar(type: Int, content: String?, chatId: Long) {
                    content?.let { viewModel.setResultMessage(it) }
                }

                override fun showSnackbar(type: Int, content: String, action: () -> Unit) {
                    viewModel.setResultMessage(content)
                }
            }
            MegaNodeDialogUtil.showRenameNodeDialog(
                context = this@ImagePreviewActivity,
                node = node,
                snackbarShower = snackbarShower,
                actionNodeCallback = null,
                onRenameConfirmed = { handle, newName ->
                    performRename(handle, newName, snackbarShower)
                },
                getRootNodeUseCase = getRootNodeUseCase,
                nodeExistsInCurrentLocationUseCase = nodeExistsInCurrentLocationUseCase,
            )
        }
    }

    private fun performRename(
        nodeHandle: Long,
        newName: String,
        snackbarShower: SnackbarShower,
    ) {
        lifecycleScope.launch {
            runCatching { renameNodeUseCase(nodeHandle, newName) }
                .onSuccess {
                    snackbarShower.showSnackbar(getString(sharedR.string.context_correctly_renamed))
                }
                .onFailure {
                    Timber.e(it, "Error renaming node")
                    snackbarShower.showSnackbar(getString(R.string.context_no_renamed))
                }
        }
    }

    private fun hideNode(
        imageNode: ImageNode,
        accountType: AccountType?,
        isBusinessAccountExpired: Boolean,
        isHiddenNodesOnboarded: Boolean?,
    ) {
        val isPaid = accountType?.isPaid ?: false

        if (!isPaid || isBusinessAccountExpired) {
            val intent = HiddenNodesOnboardingActivity.createScreen(
                context = this,
                isOnboarding = false,
            )
            hiddenNodesOnboardingLauncher.launch(intent)
            overridePendingTransition(0, 0)
        } else if (isHiddenNodesOnboarded == true) {
            viewModel.hideNode(nodeId = imageNode.id)
            val message = resources.getQuantityString(R.plurals.hidden_nodes_result_message, 1, 1)
            viewModel.setResultMessage(message)
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
        val message =
            resources.getQuantityString(sharedR.plurals.unhidden_nodes_result_message, 1, 1)
        viewModel.setResultMessage(message)
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

    private fun addToAlbum(imageNode: ImageNode) {
        val intent = Intent(this, AddToAlbumActivity::class.java).apply {
            val ids = listOf(imageNode).map { it.id.longValue }.toTypedArray()
            val viewType = 0.takeIf { imageNode.type is ImageFileTypeInfo } ?: 1

            putExtra("ids", ids)
            putExtra("type", viewType)
        }
        addToAlbumLauncher.launch(intent)
    }

    private val activityResultLauncherUCrop =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data ?: return@registerForActivityResult
            when (result.resultCode) {
                RESULT_OK -> {
                    val uri = UCrop.getOutput(data)
                    viewModel.uploadCurrentEditedImage(uri ?: return@registerForActivityResult)
                }

                UCrop.RESULT_ERROR -> {
                    val error = UCrop.getError(data)
                    showSnackbar(
                        SNACKBAR_TYPE,
                        error?.message ?: getString(sharedR.string.general_text_error),
                        INVALID_HANDLE
                    )
                }
            }
        }

    private fun openPhotoEditor(uri: Uri, destinationFileName: String) {
        var uCrop = UCrop.of(uri, Uri.fromFile(File(cacheDir, destinationFileName)))
        uCrop.withOptions(
            UCrop.Options().apply {
                setFreeStyleCropMode(FREESTYLE_CROP_MODE_ENABLE_WITH_PASS_THROUGH)
                setCompressionQuality(90)
                setToolbarTitle(getString(R.string.title_edit_profile_info))
                setToolbarTitleTextGravity(Gravity.START)
                setToolbarColor(getColor(R.color.dark_grey))
                setStatusBarColor(getColor(R.color.dark_grey))
                setCropGridCornerColor(getColor(R.color.color_support_info))
                setRootViewBackgroundColor(getColor(R.color.black))
                setToolbarWidgetColor(getColor(R.color.white))
                setActiveControlsWidgetColor(getColor(R.color.white))
                setImageToCropBoundsAnimDuration(200)
                setAllowedGestures(
                    tabRotate = SCALE,
                    tabScale = SCALE,
                    tabAspectRatio = SCALE,
                )
                setDiscardDialogMessage(getString(sharedR.string.general_dialog_title_discard_changes))
                setDiscardDialogCancelText(getString(R.string.button_cancel))
                setDiscardDialogDiscardText(getString(R.string.settings_help_report_issue_discard_button))
            }
        )
        uCrop.start(this@ImagePreviewActivity, activityResultLauncherUCrop)
    }

    private fun playSlideshow() {
        Analytics.tracker.trackEvent(PlaySlideshowMenuToolbarEvent)
        val intent = Intent(this, SlideshowActivity::class.java)
        this@ImagePreviewActivity.intent.putExtra(
            PARAMS_CURRENT_IMAGE_NODE_ID_VALUE,
            viewModel.state.value.currentImageNode?.id?.longValue
        )
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

    /**
     * Upon a node is open with, if it cannot be previewed in-app,
     * then download it first, this download will be marked as "download by open with".
     *
     */
    private fun saveNodeByOpenWith() {
        viewModel.executeTransfer(downloadForPreview = true)
    }

    private fun handleHiddenNodesOnboardingResult(result: ActivityResult) {
        if (result.resultCode != Activity.RESULT_OK) return
        val nodeId = tempNodeId ?: return

        viewModel.hideNode(nodeId)

        val message = resources.getQuantityString(R.plurals.hidden_nodes_result_message, 1, 1)
        viewModel.setResultMessage(message)
    }

    private fun handleAddToAlbumResult(result: ActivityResult) {
        if (result.resultCode != Activity.RESULT_OK) return
        val message = result.data?.getStringExtra("message") ?: return

        viewModel.setResultMessage(message)
    }

    override fun onDestroy() {
        if (isFinishing) {
            intent.getStringExtra(FETCHER_PARAMS)?.let { largeBundleHolder.release(it) }
        }
        viewModel.clearImageResultCache()
        super.onDestroy()
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
            enableAddToAlbum: Boolean = false,
        ): Intent {
            val paramsKey = context.largeBundleHolder.put(
                bundleOf(*params.toList().toTypedArray())
            )
            return Intent(context, ImagePreviewActivity::class.java).apply {
                putExtra(IMAGE_NODE_FETCHER_SOURCE, imageSource)
                putExtra(IMAGE_PREVIEW_MENU_OPTIONS, menuOptionsSource)
                putExtra(PARAMS_CURRENT_IMAGE_NODE_ID_VALUE, anchorImageNodeId?.longValue)
                putExtra(FETCHER_PARAMS, paramsKey)
                putExtra(IMAGE_PREVIEW_IS_FOREIGN, isForeign)
                putExtra(IMAGE_PREVIEW_ADD_TO_ALBUM, enableAddToAlbum)
            }
        }

        fun createIntent(
            context: Context,
            fileNodeId: Long,
            parentNodeId: Long,
            nodeSourceType: Int?,
        ): Intent? {
            val (imageSource, menuOptionsSource, paramKey) = when (nodeSourceType) {
                NodeSourceTypeInt.FILE_BROWSER_ADAPTER -> Triple(
                    ImagePreviewFetcherSource.CLOUD_DRIVE,
                    ImagePreviewMenuSource.CLOUD_DRIVE,
                    CloudDriveImageNodeFetcher.PARENT_ID
                )

                NodeSourceTypeInt.RUBBISH_BIN_ADAPTER -> Triple(
                    ImagePreviewFetcherSource.RUBBISH_BIN,
                    ImagePreviewMenuSource.RUBBISH_BIN,
                    RubbishBinImageNodeFetcher.PARENT_ID
                )

                NodeSourceTypeInt.INCOMING_SHARES_ADAPTER,
                NodeSourceTypeInt.OUTGOING_SHARES_ADAPTER,
                    -> Triple(
                    ImagePreviewFetcherSource.SHARED_ITEMS,
                    ImagePreviewMenuSource.SHARED_ITEMS,
                    SharedItemsImageNodeFetcher.PARENT_ID
                )

                NodeSourceTypeInt.LINKS_ADAPTER -> Triple(
                    ImagePreviewFetcherSource.SHARED_ITEMS,
                    ImagePreviewMenuSource.LINKS,
                    SharedItemsImageNodeFetcher.PARENT_ID
                )

                NodeSourceTypeInt.BACKUPS_ADAPTER -> Triple(
                    ImagePreviewFetcherSource.CLOUD_DRIVE,
                    ImagePreviewMenuSource.CLOUD_DRIVE,
                    CloudDriveImageNodeFetcher.PARENT_ID
                )

                NodeSourceTypeInt.FAVOURITES_ADAPTER -> Triple(
                    ImagePreviewFetcherSource.FAVOURITE,
                    ImagePreviewMenuSource.FAVOURITE,
                    FavouriteImageNodeFetcher.NODE_ID
                )

                else -> {
                    Timber.e("Unknown node source type: $nodeSourceType")
                    return null
                }
            }

            val params = if (nodeSourceType == NodeSourceTypeInt.FAVOURITES_ADAPTER) {
                mapOf(paramKey to fileNodeId)
            } else {
                mapOf(paramKey to parentNodeId)
            }

            return createIntent(
                context = context,
                imageSource = imageSource,
                menuOptionsSource = menuOptionsSource,
                anchorImageNodeId = NodeId(fileNodeId),
                params = params,
                enableAddToAlbum = nodeSourceType in listOf(
                    NodeSourceTypeInt.FILE_BROWSER_ADAPTER,
                    NodeSourceTypeInt.OUTGOING_SHARES_ADAPTER,
                )
            )
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
                enableAddToAlbum = false,
            )
        }
    }
}
