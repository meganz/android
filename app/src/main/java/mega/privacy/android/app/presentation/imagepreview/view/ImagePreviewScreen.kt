@file:OptIn(
    ExperimentalComposeUiApi::class
)

package mega.privacy.android.app.presentation.imagepreview.view

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomAppBar
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.TopAppBar
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import me.saket.telephoto.ExperimentalTelephotoApi
import me.saket.telephoto.flick.FlickToDismiss
import me.saket.telephoto.flick.FlickToDismissState
import me.saket.telephoto.flick.rememberFlickToDismissState
import me.saket.telephoto.zoomable.DoubleClickToZoomListener
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.ZoomableImageState
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel
import mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponent
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.layouts.navigationBarsLandscapePadding
import mega.privacy.android.shared.original.core.ui.controls.text.MiddleEllipsisText
import mega.privacy.android.shared.original.core.ui.theme.extensions.black_white
import mega.privacy.android.shared.original.core.ui.theme.extensions.white_alpha_070_grey_alpha_070
import mega.privacy.android.shared.original.core.ui.theme.extensions.white_black
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.ImagePreviewHideNodeMenuToolBarEvent

@Composable
internal fun ImagePreviewScreen(
    onClickBack: () -> Unit,
    onClickVideoPlay: (ImageNode) -> Unit,
    onClickEdit: (ImageNode) -> Unit,
    onClickSlideshow: () -> Unit,
    onClickInfo: (ImageNode) -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: ImagePreviewViewModel = viewModel(),
    onClickFavourite: (ImageNode) -> Unit = {},
    onClickLabel: (ImageNode) -> Unit = {},
    onClickOpenWith: (ImageNode) -> Unit = {},
    onClickSaveToDevice: () -> Unit = {},
    onClickImport: (ImageNode) -> Unit = {},
    onSwitchAvailableOffline: ((checked: Boolean, ImageNode) -> Unit)? = null,
    onClickGetLink: (ImageNode) -> Unit = {},
    onClickSendTo: (ImageNode) -> Unit = {},
    onClickShare: (ImageNode) -> Unit = {},
    onClickRename: (ImageNode) -> Unit = {},
    onClickHide: (ImageNode, AccountType?, Boolean, Boolean?) -> Unit = { _, _, _, _ -> },
    onClickHideHelp: () -> Unit = {},
    onClickUnhide: (ImageNode) -> Unit = {},
    onClickMove: (ImageNode) -> Unit = {},
    onClickCopy: (ImageNode) -> Unit = {},
    onClickRestore: (ImageNode) -> Unit = {},
    onClickRemove: (ImageNode) -> Unit = {},
    onClickMoveToRubbishBin: (ImageNode) -> Unit = {},
    onClickAddToAlbum: (ImageNode) -> Unit = {},
) {
    val viewState by viewModel.state.collectAsStateWithLifecycle()
    val imageNodes = viewState.imageNodes

    if (viewState.isInitialized && imageNodes.isEmpty()) {
        LaunchedEffect(Unit) {
            onClickBack()
        }
    }

    val isHiddenNodesEnabled by produceState(initialValue = false) {
        value = viewModel.isHiddenNodesActive()
    }

    val currentImageNodeIndex = viewState.currentImageNodeIndex
    val accountType = viewState.accountType
    val isBusinessAccountExpired = viewState.isBusinessAccountExpired
    val isHiddenNodesOnboarded = viewState.isHiddenNodesOnboarded
    viewState.currentImageNode?.let { currentImageNode ->
        val isCurrentImageNodeAvailableOffline = viewState.isCurrentImageNodeAvailableOffline
        var showRemoveLinkDialog by rememberSaveable { mutableStateOf(false) }
        var showMoveToRubbishBinDialog by rememberSaveable { mutableStateOf(false) }
        var showRemoveDialog by rememberSaveable { mutableStateOf(false) }

        val inFullScreenMode = viewState.inFullScreenMode
        val scaffoldState = rememberScaffoldState(snackbarHostState = snackbarHostState)
        val context = LocalContext.current
        val pagerState = rememberPagerState(
            initialPage = currentImageNodeIndex,
            initialPageOffsetFraction = 0f,
        ) {
            imageNodes.size
        }

        val coroutineScope = rememberCoroutineScope()
        val modalSheetState = rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Hidden,
            skipHalfExpanded = false,
        )

        var flickOffsetFraction by remember { mutableFloatStateOf(0f) }

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.distinctUntilChanged().collect { page ->
                viewState.imageNodes.getOrNull(page)?.let {
                    viewModel.setCurrentImageNodeIndex(page)
                    viewModel.setCurrentImageNode(it)
                    viewModel.setCurrentImageNodeAvailableOffline(it)
                }
            }
        }

        if (viewState.resultMessage.isNotEmpty()) {
            LaunchedEffect(Unit) {
                scaffoldState.snackbarHostState.showAutoDurationSnackbar(
                    message = viewState.resultMessage,
                )
                viewModel.clearResultMessage()
            }
        }

        if (viewState.showDeletedMessage) {
            LaunchedEffect(Unit) {
                scaffoldState.snackbarHostState.showAutoDurationSnackbar(
                    message = context.getString(R.string.context_correctly_removed),
                )
                viewModel.hideDeletedMessage()
            }
        }

        if (showRemoveLinkDialog) {
            MegaAlertDialog(
                text = stringResource(sharedR.string.remove_links_warning_message),
                confirmButtonText = stringResource(id = R.string.general_remove),
                cancelButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
                onConfirm = {
                    viewModel.disableExport(currentImageNode)
                    hideBottomSheet(coroutineScope, modalSheetState)
                    showRemoveLinkDialog = false
                },
                onDismiss = {
                    showRemoveLinkDialog = false
                },
            )
        }

        if (showMoveToRubbishBinDialog) {
            MegaAlertDialog(
                text = stringResource(id = R.string.confirmation_move_to_rubbish),
                confirmButtonText = stringResource(id = R.string.general_move),
                cancelButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
                onConfirm = {
                    onClickMoveToRubbishBin(currentImageNode)
                    hideBottomSheet(coroutineScope, modalSheetState)
                    showMoveToRubbishBinDialog = false
                },
                onDismiss = {
                    showMoveToRubbishBinDialog = false
                }
            )
        }

        if (showRemoveDialog) {
            MegaAlertDialog(
                text = stringResource(id = R.string.confirmation_delete_from_mega),
                confirmButtonText = stringResource(id = R.string.general_remove),
                cancelButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
                onConfirm = {
                    onClickRemove(currentImageNode)
                    showRemoveDialog = false
                },
                onDismiss = {
                    showRemoveDialog = false
                }
            )
        }

        BackHandler {
            onClickBack()
        }

        MegaScaffold(
            modifier = Modifier
                .semantics { testTagsAsResourceId = true },
            scaffoldState = scaffoldState,
            contentWindowInsets = WindowInsets.ime,
            backgroundAlpha = 0f,
            content = { innerPadding ->
                ImagePreviewContent(
                    modifier = Modifier
                        .background(
                            color = (Color.Black.takeIf { flickOffsetFraction == 0f && inFullScreenMode }
                                ?: MaterialTheme.colors.white_black).copy(alpha = 1f - flickOffsetFraction),
                        )
                        .padding(innerPadding),
                    pagerState = pagerState,
                    imageNodes = imageNodes,
                    currentImageNodeIndex = currentImageNodeIndex,
                    currentImageNode = currentImageNode,
                    downloadImage = viewModel::monitorImageResult,
                    getImagePath = viewModel::getHighestResolutionImagePath,
                    getErrorImagePath = viewModel::getFallbackImagePath,
                    onImageTap = { viewModel.switchFullScreenMode() },
                    onFlick = {
                        flickOffsetFraction = it
                    },
                    onSwitchFullScreenMode = viewModel::setFullScreenMode,
                    onClickVideoPlay = onClickVideoPlay,
                    topAppBar = { imageNode ->
                        AnimatedVisibility(
                            visible = !inFullScreenMode,
                            enter = slideInVertically(initialOffsetY = { -it }),
                            exit = slideOutVertically(targetOffsetY = { -it })
                        ) {
                            ImagePreviewTopBar(
                                imageNode = imageNode,
                                showEditMenu = viewModel::isPhotoEditorMenuVisible,
                                showSlideshowMenu = viewModel::isSlideshowMenuVisible,
                                showForwardMenu = viewModel::isForwardMenuVisible,
                                showSaveToDeviceMenu = viewModel::isSaveToDeviceMenuVisible,
                                showManageLinkMenu = viewModel::isGetLinkMenuVisible,
                                showMoreMenu = viewModel::isMoreMenuVisible,
                                onClickBack = onClickBack,
                                onClickEdit = { onClickEdit(imageNode) },
                                onClickSlideshow = onClickSlideshow,
                                onClickForward = { onClickSendTo(imageNode) },
                                onClickSaveToDevice = onClickSaveToDevice,
                                onClickGetLink = { onClickGetLink(imageNode) },
                                onClickMore = {
                                    coroutineScope.launch {
                                        modalSheetState.show()
                                    }
                                },
                                modifier = Modifier
                                    .navigationBarsLandscapePadding()
                                    .statusBarsPadding(),
                            )
                        }
                    },
                    bottomAppBar = { currentImageNode, index ->
                        AnimatedVisibility(
                            visible = !inFullScreenMode,
                            enter = slideInVertically(initialOffsetY = { it }),
                            exit = slideOutVertically(targetOffsetY = { it })
                        ) {
                            val photoIndexText = stringResource(
                                R.string.wizard_steps_indicator,
                                index + 1,
                                imageNodes.size
                            ).takeIf { imageNodes.size > 1 }

                            ImagePreviewBottomBar(
                                imageName = currentImageNode.name,
                                imageIndex = photoIndexText.orEmpty(),
                                modifier = Modifier
                                    .windowInsetsPadding(WindowInsets.navigationBars),
                            )
                        }
                    },
                )

                if (modalSheetState.isVisible) {
                    Box(modifier = Modifier.safeDrawingPadding()) {
                        ImagePreviewBottomSheet(
                            modalSheetState = modalSheetState,
                            imageNode = currentImageNode,
                            isAvailableOffline = isCurrentImageNodeAvailableOffline,
                            accountType = accountType,
                            isBusinessAccountExpired = isBusinessAccountExpired,
                            isHiddenNodesEnabled = isHiddenNodesEnabled,
                            isHiddenNodesOnboarded = isHiddenNodesOnboarded,
                            showInfoMenu = viewModel::isInfoMenuVisible,
                            showFavouriteMenu = viewModel::isFavouriteMenuVisible,
                            showLabelMenu = viewModel::isLabelMenuVisible,
                            showDisputeMenu = viewModel::isDisputeMenuVisible,
                            showOpenWithMenu = viewModel::isOpenWithMenuVisible,
                            showForwardMenu = viewModel::isForwardMenuVisible,
                            showSaveToDeviceMenu = viewModel::isSaveToDeviceMenuVisible,
                            showImportMenu = viewModel::isImportMenuVisible,
                            showGetLinkMenu = viewModel::isGetLinkMenuVisible,
                            showSendToChatMenu = viewModel::isSendToChatMenuVisible,
                            showShareMenu = viewModel::isShareMenuVisible,
                            showRenameMenu = viewModel::isRenameMenuVisible,
                            showHideMenu = viewModel::isHideMenuVisible,
                            showUnhideMenu = viewModel::isUnhideMenuVisible,
                            forceHideHiddenMenus = viewModel::forceHideHiddenMenus,
                            showMoveMenu = viewModel::isMoveMenuVisible,
                            showCopyMenu = viewModel::isCopyMenuVisible,
                            showRestoreMenu = viewModel::isRestoreMenuVisible,
                            showRemoveMenu = viewModel::isRemoveMenuVisible,
                            showAvailableOfflineMenu = viewModel::isAvailableOfflineMenuVisible,
                            showRemoveOfflineMenu = viewModel::isRemoveOfflineMenuVisible,
                            showMoveToRubbishBin = viewModel::isMoveToRubbishBinMenuVisible,
                            showAddToAlbum = viewModel::isAddToAlbumMenuVisible,
                            downloadImage = viewModel::monitorImageResult,
                            getImageThumbnailPath = viewModel::getLowestResolutionImagePath,
                            onClickInfo = {
                                onClickInfo(currentImageNode)
                                hideBottomSheet(coroutineScope, modalSheetState)
                            },
                            onClickFavourite = {
                                onClickFavourite(currentImageNode)
                                hideBottomSheet(coroutineScope, modalSheetState)
                            },
                            onClickLabel = {
                                onClickLabel(currentImageNode)
                                hideBottomSheet(coroutineScope, modalSheetState)
                            },
                            onClickDispute = {},
                            onClickOpenWith = {
                                onClickOpenWith(currentImageNode)
                                hideBottomSheet(coroutineScope, modalSheetState)
                            },
                            onClickForward = {
                                onClickSendTo(currentImageNode)
                                hideBottomSheet(coroutineScope, modalSheetState)
                            },
                            onClickSaveToDevice = {
                                onClickSaveToDevice()
                                hideBottomSheet(coroutineScope, modalSheetState)
                            },
                            onClickImport = {
                                onClickImport(currentImageNode)
                                hideBottomSheet(coroutineScope, modalSheetState)
                            },
                            onSwitchAvailableOffline = { checked ->
                                onSwitchAvailableOffline?.invoke(checked, currentImageNode)
                                hideBottomSheet(coroutineScope, modalSheetState)
                            },
                            onClickGetLink = {
                                onClickGetLink(currentImageNode)
                                hideBottomSheet(coroutineScope, modalSheetState)
                            },
                            onClickRemoveLink = {
                                if (!currentImageNode.isTakenDown) {
                                    showRemoveLinkDialog = true
                                }
                                hideBottomSheet(coroutineScope, modalSheetState)
                            },
                            onClickSendToChat = {
                                onClickSendTo(currentImageNode)
                                hideBottomSheet(coroutineScope, modalSheetState)
                            },
                            onClickShare = {
                                onClickShare(currentImageNode)
                                hideBottomSheet(coroutineScope, modalSheetState)
                            },
                            onClickRename = {
                                onClickRename(currentImageNode)
                                hideBottomSheet(coroutineScope, modalSheetState)
                            },
                            onClickHide = {
                                Analytics.tracker.trackEvent(ImagePreviewHideNodeMenuToolBarEvent)
                                onClickHide(
                                    currentImageNode,
                                    accountType,
                                    isBusinessAccountExpired,
                                    isHiddenNodesOnboarded
                                )
                                hideBottomSheet(coroutineScope, modalSheetState)
                            },
                            onClickHideHelp = {
                                onClickHideHelp()
                                hideBottomSheet(coroutineScope, modalSheetState)
                            },
                            onClickUnhide = {
                                onClickUnhide(currentImageNode)
                                hideBottomSheet(coroutineScope, modalSheetState)
                            },
                            onClickMove = {
                                onClickMove(currentImageNode)
                                hideBottomSheet(coroutineScope, modalSheetState)
                            },
                            onClickCopy = {
                                onClickCopy(currentImageNode)
                                hideBottomSheet(coroutineScope, modalSheetState)
                            },
                            onClickRestore = {
                                onClickRestore(currentImageNode)
                                hideBottomSheet(coroutineScope, modalSheetState)
                            },
                            onClickRemove = {
                                showRemoveDialog = true
                                hideBottomSheet(coroutineScope, modalSheetState)
                            },
                            onClickMoveToRubbishBin = {
                                showMoveToRubbishBinDialog = true
                                hideBottomSheet(coroutineScope, modalSheetState)
                            },
                            onClickAddToAlbum = {
                                onClickAddToAlbum(currentImageNode)
                                hideBottomSheet(coroutineScope, modalSheetState)
                            },
                        )
                    }
                }
            },
        )

        StartTransferComponent(
            event = viewState.transferEvent,
            onConsumeEvent = viewModel::consumeTransferEvent,
            snackBarHostState = scaffoldState.snackbarHostState,
        )
    }
}

private fun hideBottomSheet(
    coroutineScope: CoroutineScope,
    modalSheetState: ModalBottomSheetState,
) {
    coroutineScope.launch {
        modalSheetState.hide()
    }
}

@Composable
private fun ImagePreviewContent(
    pagerState: PagerState,
    imageNodes: List<ImageNode>,
    currentImageNodeIndex: Int,
    currentImageNode: ImageNode,
    onImageTap: () -> Unit,
    onSwitchFullScreenMode: (Boolean) -> Unit,
    onFlick: (Float) -> Unit,
    topAppBar: @Composable (ImageNode) -> Unit,
    bottomAppBar: @Composable (ImageNode, Int) -> Unit,
    downloadImage: suspend (ImageNode) -> Flow<ImageResult>,
    getImagePath: suspend (ImageResult?) -> String?,
    getErrorImagePath: suspend (ImageResult?) -> String?,
    onClickVideoPlay: (ImageNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {

        HorizontalPager(
            modifier = Modifier.fillMaxSize(),
            state = pagerState,
            beyondViewportPageCount = 1,
            key = {
                imageNodes.getOrNull(it)?.id?.longValue ?: "${System.currentTimeMillis()}_$it"
            },
        ) { index ->
            val imageNode = imageNodes.getOrNull(index)
            if (imageNode != null) {
                val imageResultTriple by produceState<Triple<Int, String?, String?>>(
                    initialValue = Triple(0, null, null),
                    key1 = imageNode,
                ) {
                    downloadImage(imageNode).collectLatest { imageResult ->
                        value = Triple(
                            imageResult.getProgressPercentage() ?: 0,
                            getImagePath(imageResult),
                            getErrorImagePath(imageResult)
                        )
                    }
                }

                val (progress, imagePath, fallbackImagePath) = imageResultTriple

                val zoomableState = rememberZoomableState(
                    zoomSpec = ZoomSpec(maxZoomFactor = Int.MAX_VALUE.toFloat())
                )
                SideEffect {
                    if (index != pagerState.currentPage) {
                        coroutineScope.launch {
                            zoomableState.resetZoom()
                        }
                    }
                }
                val imageState = rememberZoomableImageState(zoomableState)

                ImagePreviewContent(
                    imageNode = imageNode,
                    progress = progress,
                    imagePath = imagePath,
                    errorImagePath = fallbackImagePath,
                    imageState = imageState,
                    onImageTap = onImageTap,
                    onSwitchFullScreenMode = onSwitchFullScreenMode,
                    onFlick = onFlick,
                    onClickVideoPlay = onClickVideoPlay,
                )
            }
        }

        Box(
            modifier = Modifier
                .wrapContentSize()
                .align(Alignment.TopCenter),
        ) {
            topAppBar(currentImageNode)
        }

        Box(
            modifier = Modifier
                .wrapContentSize()
                .align(Alignment.BottomCenter),
        ) {
            bottomAppBar(currentImageNode, currentImageNodeIndex)
        }
    }
}

@Composable
private fun HandleFlickStateEffect(
    flickState: FlickToDismissState,
    onSwitchFullScreenMode: (Boolean) -> Unit,
    onFlick: (Float) -> Unit = {},
) {
    val gestureState = flickState.gestureState
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    LaunchedEffect(gestureState) {
        when (gestureState) {
            is FlickToDismissState.GestureState.Idle -> return@LaunchedEffect
            is FlickToDismissState.GestureState.Resetting -> onSwitchFullScreenMode(false)
            is FlickToDismissState.GestureState.Dismissing -> {
                onSwitchFullScreenMode(true)
                delay(gestureState.animationDuration / 2)
                backDispatcher?.onBackPressed()
            }

            else -> onSwitchFullScreenMode(true)
        }
    }

    LaunchedEffect(flickState.offsetFraction) {
        onFlick(flickState.offsetFraction)
    }
}

@OptIn(ExperimentalTelephotoApi::class)
@Composable
private fun ImagePreviewContent(
    imageNode: ImageNode,
    progress: Int,
    imagePath: String?,
    errorImagePath: String?,
    onImageTap: () -> Unit,
    onClickVideoPlay: (ImageNode) -> Unit,
    onSwitchFullScreenMode: (Boolean) -> Unit,
    onFlick: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
    imageState: ZoomableImageState? = null,
) {
    val flickState = rememberFlickToDismissState(dismissThresholdRatio = 0.3f)
    HandleFlickStateEffect(
        flickState = flickState,
        onSwitchFullScreenMode = onSwitchFullScreenMode,
        onFlick = onFlick
    )
    FlickToDismiss(
        state = flickState,
        modifier = modifier
            .fillMaxSize(),
    ) {
        val isVideo = imageNode.type is VideoFileTypeInfo
        ImageContent(
            fullSizePath = imageNode.run {
                fullSizePath.takeIf {
                    imageNode.serializedData?.contains(
                        "local"
                    ) == true
                }
            } ?: imagePath,
            errorImagePath = imageNode.run {
                fullSizePath.takeIf {
                    imageNode.serializedData?.contains(
                        "local"
                    ) == true
                }
            } ?: errorImagePath,
            enableZoom = !isVideo,
            imageState = imageState ?: rememberZoomableImageState(),
            onImageTap = onImageTap,
        )
        if (isVideo) {
            IconButton(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(40.dp))
                    .background(Color.Black.copy(alpha = 0.5f)),
                onClick = { onClickVideoPlay(imageNode) }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_play),
                    contentDescription = "Image Preview play video",
                    tint = Color.White,
                )
            }
        }

        if (progress in 1 until 100) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .safeDrawingPadding()
                    .padding(end = 16.dp)
                    .width(25.dp),
                progress = progress.toFloat() / 100,
                color = MaterialTheme.colors.secondary,
                strokeWidth = 3.dp,
            )
        }
    }
}

@Composable
private fun ImageContent(
    fullSizePath: String?,
    errorImagePath: String?,
    imageState: ZoomableImageState,
    enableZoom: Boolean,
    onImageTap: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        var imagePath by remember(fullSizePath) {
            mutableStateOf(fullSizePath)
        }

        val request = ImageRequest.Builder(LocalContext.current)
            .data(imagePath)
            .listener(
                onError = { _, _ ->
                    // when some image full size picture decoder throw exception, use preview/thumbnail instead
                    // detail see package coil.decode [BitmapFactoryDecoder] 79 line
                    imagePath = errorImagePath
                }
            )
            .crossfade(true)
            .build()

        val isThumbnailFile = imagePath?.contains(CacheFolderConstant.THUMBNAIL_FOLDER) == true

        ZoomableAsyncImage(
            model = request,
            state = imageState,
            gesturesEnabled = enableZoom && !isThumbnailFile,
            contentDescription = "Image Preview",
            modifier = Modifier.fillMaxSize(),
            onClick = {
                onImageTap()
            },
            onDoubleClick = DoubleClickToZoomListener.cycle(maxZoomFactor = 3f),
        )
    }
}

@Composable
private fun ImagePreviewTopBar(
    imageNode: ImageNode,
    showEditMenu: suspend (ImageNode) -> Boolean,
    showSlideshowMenu: suspend (ImageNode) -> Boolean,
    showForwardMenu: suspend (ImageNode) -> Boolean,
    showSaveToDeviceMenu: suspend (ImageNode) -> Boolean,
    showManageLinkMenu: suspend (ImageNode) -> Boolean,
    showMoreMenu: suspend (ImageNode) -> Boolean,
    onClickBack: () -> Unit,
    onClickEdit: () -> Unit,
    onClickSlideshow: () -> Unit,
    onClickForward: () -> Unit,
    onClickSaveToDevice: () -> Unit,
    onClickGetLink: () -> Unit,
    onClickMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = Modifier.background(MaterialTheme.colors.white_alpha_070_grey_alpha_070),
    ) {
        TopAppBar(
            title = {},
            modifier = modifier,
            backgroundColor = Color.Transparent,
            navigationIcon = {
                IconButton(onClick = onClickBack) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back_white),
                        contentDescription = "Image Preview Back",
                        tint = MaterialTheme.colors.black_white,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_APP_BAR_BACK),
                    )
                }
            },
            actions = {
                val isSlideshowMenuVisible by produceState(false, imageNode) {
                    value = showSlideshowMenu(imageNode)
                }

                val isForwardMenuVisible by produceState(false, imageNode) {
                    value = showForwardMenu(imageNode)
                }

                val isSaveToDeviceMenuVisible by produceState(false, imageNode) {
                    value = showSaveToDeviceMenu(imageNode)
                }

                val isManageLinkMenuVisible by produceState(false, imageNode) {
                    value = showManageLinkMenu(imageNode)
                }

                val isMoreMenuVisible by produceState(false, imageNode) {
                    value = showMoreMenu(imageNode)
                }

                val isEditMenuVisible by produceState(false, imageNode) {
                    value = showEditMenu(imageNode)
                }

                if (isEditMenuVisible) {
                    IconButton(onClick = onClickEdit) {
                        Icon(
                            imageVector = IconPack.Medium.Thin.Outline.SlidersHorizontal01,
                            contentDescription = null,
                            tint = MaterialTheme.colors.black_white,
                            modifier = Modifier
                                .testTag(IMAGE_PREVIEW_APP_BAR_EDIT)
                                .size(22.dp),
                        )
                    }
                }

                if (isSlideshowMenuVisible) {
                    IconButton(onClick = onClickSlideshow) {
                        Icon(
                            imageVector = IconPack.Medium.Thin.Outline.PlaySquare,
                            contentDescription = null,
                            tint = MaterialTheme.colors.black_white,
                            modifier = Modifier.testTag(IMAGE_PREVIEW_APP_BAR_SLIDESHOW),
                        )
                    }
                }

                if (isForwardMenuVisible) {
                    IconButton(onClick = onClickForward) {
                        Icon(
                            painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.CornerUpRight),
                            contentDescription = null,
                            tint = MaterialTheme.colors.black_white,
                            modifier = Modifier.testTag(IMAGE_PREVIEW_APP_BAR_FORWARD),
                        )
                    }
                }

                if (isSaveToDeviceMenuVisible) {
                    IconButton(onClick = onClickSaveToDevice) {
                        Icon(
                            painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Download),
                            contentDescription = null,
                            tint = MaterialTheme.colors.black_white,
                            modifier = Modifier.testTag(IMAGE_PREVIEW_APP_BAR_SAVE_TO_DEVICE),
                        )
                    }
                }

                if (isManageLinkMenuVisible) {
                    IconButton(onClick = onClickGetLink) {
                        Icon(
                            painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Link01),
                            contentDescription = null,
                            tint = MaterialTheme.colors.black_white,
                            modifier = Modifier.testTag(IMAGE_PREVIEW_APP_BAR_MANAGE_LINK),
                        )
                    }
                }

                if (isMoreMenuVisible) {
                    IconButton(onClick = onClickMore) {
                        Icon(
                            painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.MoreVertical),
                            contentDescription = null,
                            tint = MaterialTheme.colors.black_white,
                            modifier = Modifier.testTag(IMAGE_PREVIEW_APP_BAR_MORE),
                        )
                    }
                }
            },
            elevation = 0.dp,
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun ImagePreviewBottomBar(
    modifier: Modifier = Modifier,
    imageName: String = "",
    imageIndex: String = "",
) {
    Box(
        modifier = Modifier.background(MaterialTheme.colors.white_alpha_070_grey_alpha_070)
    ) {
        BottomAppBar(
            backgroundColor = Color.Transparent,
            elevation = 0.dp,
            modifier = modifier,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MiddleEllipsisText(
                    text = imageName,
                    color = TextColor.Secondary,
                    modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_BAR_TEXT_IMAGE_NAME),
                )
                MiddleEllipsisText(
                    text = imageIndex,
                    color = TextColor.Secondary,
                    modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_BAR_TEXT_IMAGE_COUNT),
                )
            }
        }
    }
}