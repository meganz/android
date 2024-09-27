package mega.privacy.android.app.presentation.imagepreview.view

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel
import mega.privacy.android.app.presentation.imagepreview.slideshow.view.rememberPhotoState
import mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponent
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.mobile.analytics.event.ImagePreviewHideNodeMenuToolBarEvent

@OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
internal fun ImagePreviewScreen(
    onClickBack: () -> Unit,
    onClickVideoPlay: (ImageNode) -> Unit,
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
    onClickHide: (ImageNode, AccountDetail?, Boolean?) -> Unit = { _, _, _ -> },
    onClickHideHelp: () -> Unit = {},
    onClickUnHide: (ImageNode) -> Unit = {},
    onClickMove: (ImageNode) -> Unit = {},
    onClickCopy: (ImageNode) -> Unit = {},
    onClickRestore: (ImageNode) -> Unit = {},
    onClickRemove: (ImageNode) -> Unit = {},
    onClickMoveToRubbishBin: (ImageNode) -> Unit = {},
) {
    val viewState by viewModel.state.collectAsStateWithLifecycle()
    val imageNodes = viewState.imageNodes

    if (viewState.isInitialized && imageNodes.isEmpty()) {
        LaunchedEffect(Unit) {
            onClickBack()
        }
    }

    val isHiddenNodesEnabled by produceState(initialValue = false) {
        value = viewModel.isHiddenNodesEnabled()
    }

    val currentImageNodeIndex = viewState.currentImageNodeIndex
    val accountDetail = viewState.accountDetail
    val isHiddenNodesOnboarded = viewState.isHiddenNodesOnboarded
    viewState.currentImageNode?.let { currentImageNode ->
        val isCurrentImageNodeAvailableOffline = viewState.isCurrentImageNodeAvailableOffline
        var showRemoveLinkDialog by rememberSaveable { mutableStateOf(false) }
        var showMoveToRubbishBinDialog by rememberSaveable { mutableStateOf(false) }
        var showRemoveDialog by rememberSaveable { mutableStateOf(false) }

        val inFullScreenMode = viewState.inFullScreenMode
        val isMagnifierMode = viewState.isMagnifierMode

        val scaffoldState = rememberScaffoldState(snackbarHostState = snackbarHostState)
        val context = LocalContext.current
        val photoState = rememberPhotoState()
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

        fun hideSheet(action: () -> Unit): () -> Unit = {
            action()
            coroutineScope.launch {
                modalSheetState.hide()
            }
        }

        fun <T> hideSheet(action: (T) -> Unit): (T) -> Unit = { param ->
            action(param)
            coroutineScope.launch {
                modalSheetState.hide()
            }
        }

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
                text = pluralStringResource(
                    id = R.plurals.remove_links_warning_text,
                    count = 1
                ),
                confirmButtonText = stringResource(id = R.string.general_remove),
                cancelButtonText = stringResource(id = R.string.general_cancel),
                onConfirm = hideSheet {
                    viewModel.disableExport(currentImageNode)
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
                cancelButtonText = stringResource(id = R.string.general_cancel),
                onConfirm = hideSheet {
                    onClickMoveToRubbishBin(currentImageNode)
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
                cancelButtonText = stringResource(id = R.string.general_cancel),
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
            if (!isMagnifierMode) {
                onClickBack()
            } else {
                viewModel.switchMagnifierMode()
            }
        }

        MegaScaffold(
            modifier = Modifier
                .fillMaxSize()
                .semantics { testTagsAsResourceId = true },
            scaffoldState = scaffoldState,
            content = { innerPadding ->
                ImagePreviewContent(
                    modifier = Modifier
                        .background(
                            color = Color.Black.takeIf { inFullScreenMode || isMagnifierMode }
                                ?: MaterialTheme.colors.surface,
                        )
                        .padding(innerPadding),
                    pagerState = pagerState,
                    isMagnifierMode = isMagnifierMode,
                    imageNodes = imageNodes,
                    currentImageNodeIndex = currentImageNodeIndex,
                    currentImageNode = currentImageNode,
                    photoState = photoState,
                    downloadImage = viewModel::monitorImageResult,
                    getImagePath = viewModel::getHighestResolutionImagePath,
                    getErrorImagePath = viewModel::getFallbackImagePath,
                    onImageTap = { viewModel.switchFullScreenMode() },
                    onClickVideoPlay = onClickVideoPlay,
                    onCloseMagnifier = viewModel::switchMagnifierMode,
                    topAppBar = { imageNode ->
                        AnimatedVisibility(
                            visible = !inFullScreenMode && !isMagnifierMode,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            val appBarBackgroundColor = MaterialTheme.colors.surface
                            Box(
                                modifier = Modifier
                                    .background(appBarBackgroundColor)
                                    .statusBarsPadding()
                            ) {
                                ImagePreviewTopBar(
                                    imageNode = imageNode,
                                    showSlideshowMenu = viewModel::isSlideshowMenuVisible,
                                    showForwardMenu = viewModel::isForwardMenuVisible,
                                    showSaveToDeviceMenu = viewModel::isSaveToDeviceMenuVisible,
                                    showManageLinkMenu = viewModel::isGetLinkMenuVisible,
                                    showMagnifierMenu = viewModel::isMagnifierMenuVisible,
                                    showSendToMenu = viewModel::isSendToChatMenuVisible,
                                    showMoreMenu = viewModel::isMoreMenuVisible,
                                    onClickBack = onClickBack,
                                    onClickSlideshow = onClickSlideshow,
                                    onClickForward = { onClickSendTo(imageNode) },
                                    onClickSaveToDevice = onClickSaveToDevice,
                                    onClickGetLink = { onClickGetLink(imageNode) },
                                    onClickMagnifier = {
                                        coroutineScope.launch {
                                            photoState.animateToInitialState()
                                        }
                                        viewModel.switchMagnifierMode()
                                    },
                                    onClickSendTo = { onClickSendTo(imageNode) },
                                    onClickMore = {
                                        coroutineScope.launch {
                                            modalSheetState.show()
                                        }
                                    },
                                    backgroundColour = appBarBackgroundColor,
                                )
                            }
                        }
                    },
                    bottomAppBar = { currentImageNode, index ->
                        AnimatedVisibility(
                            visible = !inFullScreenMode && !isMagnifierMode,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            val barBackgroundColor = MaterialTheme.colors.surface
                            Box(
                                modifier = Modifier
                                    .background(barBackgroundColor)
                                    .navigationBarsPadding()
                            ) {
                                val photoIndexText = stringResource(
                                    R.string.wizard_steps_indicator,
                                    index + 1,
                                    imageNodes.size
                                ).takeIf { imageNodes.size > 1 }

                                ImagePreviewBottomBar(
                                    imageName = currentImageNode.name,
                                    imageIndex = photoIndexText.orEmpty(),
                                    backgroundColour = barBackgroundColor
                                )
                            }
                        }
                    }
                )

                if (!inFullScreenMode && !isMagnifierMode && modalSheetState.isVisible) {
                    ImagePreviewBottomSheet(
                        modalSheetState = modalSheetState,
                        imageNode = currentImageNode,
                        isAvailableOffline = isCurrentImageNodeAvailableOffline,
                        accountDetail = accountDetail,
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
                        showUnHideMenu = viewModel::isUnhideMenuVisible,
                        forceHideHiddenMenus = viewModel::forceHideHiddenMenus,
                        showMoveMenu = viewModel::isMoveMenuVisible,
                        showCopyMenu = viewModel::isCopyMenuVisible,
                        showRestoreMenu = viewModel::isRestoreMenuVisible,
                        showRemoveMenu = viewModel::isRemoveMenuVisible,
                        showAvailableOfflineMenu = viewModel::isAvailableOfflineMenuVisible,
                        showRemoveOfflineMenu = viewModel::isRemoveOfflineMenuVisible,
                        showMoveToRubbishBin = viewModel::isMoveToRubbishBinMenuVisible,
                        downloadImage = viewModel::monitorImageResult,
                        getImageThumbnailPath = viewModel::getLowestResolutionImagePath,
                        onClickInfo = hideSheet {
                            onClickInfo(currentImageNode)
                        },
                        onClickFavourite = hideSheet {
                            onClickFavourite(currentImageNode)
                        },
                        onClickLabel = hideSheet {
                            onClickLabel(currentImageNode)
                        },
                        onClickDispute = {},
                        onClickOpenWith = hideSheet {
                            onClickOpenWith(currentImageNode)
                        },
                        onClickForward = hideSheet {
                            onClickSendTo(currentImageNode)
                        },
                        onClickSaveToDevice = hideSheet {
                            onClickSaveToDevice()
                        },
                        onClickImport = hideSheet {
                            onClickImport(currentImageNode)
                        },
                        onSwitchAvailableOffline = hideSheet { checked ->
                            onSwitchAvailableOffline?.invoke(checked, currentImageNode)
                        },
                        onClickGetLink = hideSheet {
                            onClickGetLink(currentImageNode)
                        },
                        onClickRemoveLink = hideSheet {
                            if (!currentImageNode.isTakenDown) {
                                showRemoveLinkDialog = true
                            }
                        },
                        onClickSendToChat = hideSheet {
                            onClickSendTo(currentImageNode)
                        },
                        onClickShare = hideSheet {
                            onClickShare(currentImageNode)
                        },
                        onClickRename = hideSheet {
                            onClickRename(currentImageNode)
                        },
                        onClickHide = hideSheet {
                            Analytics.tracker.trackEvent(ImagePreviewHideNodeMenuToolBarEvent)
                            onClickHide(currentImageNode, accountDetail, isHiddenNodesOnboarded)
                        },
                        onClickHideHelp = hideSheet {
                            onClickHideHelp()
                        },
                        onClickUnHide = hideSheet {
                            onClickUnHide(currentImageNode)
                        },
                        onClickMove = hideSheet {
                            onClickMove(currentImageNode)
                        },
                        onClickCopy = hideSheet {
                            onClickCopy(currentImageNode)
                        },
                        onClickRestore = hideSheet {
                            onClickRestore(currentImageNode)
                        },
                        onClickRemove = hideSheet {
                            showRemoveDialog = true
                        },
                        onClickMoveToRubbishBin = hideSheet {
                            showMoveToRubbishBinDialog = true
                        },
                    )
                }

            },
        )
        StartTransferComponent(
            event = viewState.downloadEvent,
            onConsumeEvent = viewModel::consumeDownloadEvent,
            snackBarHostState = scaffoldState.snackbarHostState
        )
    }
}