@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)

package mega.privacy.android.app.presentation.imagepreview.view

import com.google.android.exoplayer2.ui.R as RExoPlayer
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.BottomAppBar
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.TopAppBar
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel
import mega.privacy.android.app.presentation.slideshow.view.PhotoBox
import mega.privacy.android.app.presentation.slideshow.view.PhotoState
import mega.privacy.android.app.presentation.slideshow.view.rememberPhotoState
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.MegaNodeUtil.getInfoText
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.controls.text.MiddleEllipsisText
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.extensions.black_white
import mega.privacy.android.core.ui.theme.extensions.white_alpha_070_grey_alpha_070
import mega.privacy.android.core.ui.theme.teal_200
import mega.privacy.android.core.ui.theme.teal_300
import mega.privacy.android.core.ui.theme.white
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.node.ImageNode
import nz.mega.sdk.MegaNode

@Composable
fun ImagePreviewScreen(
    viewModel: ImagePreviewViewModel = viewModel(),
    onClickBack: () -> Unit,
    onClickVideoPlay: (ImageNode) -> Unit,
    onClickSlideshow: () -> Unit,
    onClickInfo: (ImageNode) -> Unit,
    onClickFavourite: (ImageNode) -> Unit = {},
    onClickLabel: (ImageNode) -> Unit = {},
    onClickOpenWith: (ImageNode) -> Unit = {},
    onClickSaveToDevice: (ImageNode) -> Unit = {},
    onSwitchAvailableOffline: ((checked: Boolean, ImageNode) -> Unit)? = null,
    onClickGetLink: (ImageNode) -> Unit = {},
    onClickSendTo: (ImageNode) -> Unit = {},
    onClickShare: (ImageNode) -> Unit = {},
    onClickRename: (ImageNode) -> Unit = {},
    onClickMove: (ImageNode) -> Unit = {},
    onClickCopy: (ImageNode) -> Unit = {},
    onClickMoveToRubbishBin: (ImageNode) -> Unit = {},
) {
    val viewState by viewModel.state.collectAsStateWithLifecycle()
    val imageNodes = viewState.imageNodes
    if (imageNodes.isNotEmpty()) {
        val currentImageNodeIndex = viewState.currentImageNodeIndex
        viewState.currentImageNode?.let { currentImageNode ->
            val isCurrentImageNodeAvailableOffline = viewState.isCurrentImageNodeAvailableOffline
            val context = LocalContext.current
            var showRemoveLinkDialog by rememberSaveable { mutableStateOf(false) }
            val currentImageNodeInfo = remember(currentImageNodeIndex) {
                MegaNode.unserialize(currentImageNode.serializedData).getInfoText(context)
            }
            val labelColorResId = remember(currentImageNodeIndex, currentImageNode.label) {
                MegaNodeUtil.getNodeLabelColor(currentImageNode.label)
            }
            val labelColorText = remember(currentImageNodeIndex, currentImageNode.label) {
                MegaNodeUtil.getNodeLabelText(currentImageNode.label, context)
            }
            val imageResult by produceState<ImageResult?>(
                initialValue = null,
                key1 = currentImageNodeIndex
            ) {
                viewModel.monitorImageResult(currentImageNode).collectLatest { imageResult ->
                    value = imageResult
                }
            }

            val inFullScreenMode = viewState.inFullScreenMode
            val systemUiController = rememberSystemUiController()
            LaunchedEffect(systemUiController, inFullScreenMode) {
                systemUiController.setStatusBarColor(
                    color = if (inFullScreenMode) Color.Black else Color.Transparent,
                    darkIcons = inFullScreenMode
                )
            }

            val scaffoldState = rememberScaffoldState()
            val isLight = MaterialTheme.colors.isLight
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

            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.currentPage }.distinctUntilChanged().collect { page ->
                    imageNodes.getOrNull(page)?.let {
                        viewModel.setCurrentImageNodeIndex(page)
                        viewModel.setCurrentImageNode(it)
                        viewModel.setCurrentImageNodeAvailableOffline(it)
                    }

                }
            }

            if (viewState.transferMessage.isNotEmpty()) {
                LaunchedEffect(Unit) {
                    scaffoldState.snackbarHostState.showSnackbar(
                        message = viewState.transferMessage,
                    )
                    viewModel.clearTransferMessage()
                }
            }

            if (viewState.resultMessage.isNotEmpty()) {
                LaunchedEffect(Unit) {
                    scaffoldState.snackbarHostState.showSnackbar(
                        message = viewState.resultMessage,
                    )
                    viewModel.clearResultMessage()
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
                    onConfirm = {
                        viewModel.disableExport(currentImageNode)
                        showRemoveLinkDialog = false
                    },
                    onDismiss = {
                        showRemoveLinkDialog = false
                    },
                )
            }

            Scaffold(
                scaffoldState = scaffoldState,
                snackbarHost = { snackBarHostState ->
                    SnackbarHost(
                        hostState = snackBarHostState,
                        snackbar = { snackBarData ->
                            Snackbar(
                                snackbarData = snackBarData,
                                actionOnNewLine = true,
                                backgroundColor = black.takeIf { isLight } ?: white,
                                actionColor = teal_200.takeIf { isLight } ?: teal_300,
                            )
                        }
                    )
                },
                content = { innerPadding ->
                    ImagePreviewBottomSheet(
                        modalSheetState = modalSheetState,
                        imageThumbnailPath = imageResult?.getLowestResolutionAvailableUri(),
                        imageName = currentImageNode.name,
                        imageInfo = currentImageNodeInfo,
                        isFavourite = currentImageNode.isFavourite,
                        isExported = currentImageNode.exportedData != null,
                        showRemoveLink = currentImageNode.exportedData != null,
                        showLabel = currentImageNode.label != MegaNode.NODE_LBL_UNKNOWN,
                        labelColor = colorResource(id = labelColorResId),
                        labelColorText = labelColorText,
                        isAvailableOffline = isCurrentImageNodeAvailableOffline,
                        onClickInfo = {
                            onClickInfo(currentImageNode)
                        },
                        onClickFavourite = {
                            onClickFavourite(currentImageNode)
                        },
                        onClickLabel = {
                            onClickLabel(currentImageNode)
                        },
                        onClickOpenWith = {
                            onClickOpenWith(currentImageNode)
                            hideBottomSheet(coroutineScope, modalSheetState)
                        },
                        onClickSaveToDevice = {
                            onClickSaveToDevice(currentImageNode)
                            hideBottomSheet(coroutineScope, modalSheetState)
                        },
                        onSwitchAvailableOffline = { checked ->
                            onSwitchAvailableOffline?.invoke(checked, currentImageNode)
                            hideBottomSheet(coroutineScope, modalSheetState)
                        },
                        onClickGetLink = {
                            onClickGetLink(currentImageNode)
                        },
                        onClickRemoveLink = {
                            if (!currentImageNode.isTakenDown) {
                                showRemoveLinkDialog = true
                            }
                            hideBottomSheet(coroutineScope, modalSheetState)
                        },
                        onClickSendTo = {
                            onClickSendTo(currentImageNode)
                            hideBottomSheet(coroutineScope, modalSheetState)
                        },
                        onClickShare = {
                            onClickShare(currentImageNode)
                        },
                        onClickRename = {
                            onClickRename(currentImageNode)
                        },
                        onClickMove = {
                            onClickMove(currentImageNode)
                        },
                        onClickCopy = {
                            onClickCopy(currentImageNode)
                        },
                        onClickMoveToRubbishBin = {
                            onClickMoveToRubbishBin(currentImageNode)
                            hideBottomSheet(coroutineScope, modalSheetState)
                        },
                    ) {
                        ImagePreviewContent(
                            modifier = Modifier.background(
                                color = Color.Black.takeIf { inFullScreenMode }
                                    ?: MaterialTheme.colors.surface,
                            ),
                            innerPadding = innerPadding,
                            pagerState = pagerState,
                            imageNodes = imageNodes,
                            currentImageNodeIndex = currentImageNodeIndex,
                            currentImageNode = currentImageNode,
                            photoState = photoState,
                            downloadImage = viewModel::monitorImageResult,
                            onImageTap = { viewModel.switchFullScreenMode() },
                            onClickVideoPlay = onClickVideoPlay,
                            topAppBar = { imageNode ->
                                AnimatedVisibility(
                                    visible = !inFullScreenMode,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    ImagePreviewTopBar(
                                        showSlideshowMenu = viewState.showSlideshowOption
                                                && viewModel.isSlideshowOptionVisible(imageNode),
                                        showSaveToDeviceMenu = viewModel.isSaveToDeviceOptionVisible(
                                            imageNode
                                        ),
                                        showManageLinkMenu = viewModel.isGetLinkOptionVisible(
                                            imageNode
                                        ),
                                        showSendToMenu = viewModel.isSendToOptionVisible(imageNode),
                                        onClickBack = onClickBack,
                                        onClickSlideshow = onClickSlideshow,
                                        onClickSaveToDevice = { onClickSaveToDevice(imageNode) },
                                        onClickGetLink = { onClickGetLink(imageNode) },
                                        onClickSendTo = { onClickSendTo(imageNode) },
                                        onClickMore = {
                                            coroutineScope.launch {
                                                modalSheetState.show()
                                            }
                                        },
                                    )
                                }
                            },
                            bottomAppBar = { currentImageNode, index ->
                                AnimatedVisibility(
                                    visible = !inFullScreenMode,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    val photoIndexText = stringResource(
                                        R.string.wizard_steps_indicator,
                                        index + 1,
                                        imageNodes.size
                                    )

                                    ImagePreviewBottomBar(
                                        imageName = currentImageNode.name,
                                        imageIndex = photoIndexText,
                                    )
                                }
                            }
                        )
                    }
                },
            )
        }
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
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues,
    pagerState: PagerState,
    imageNodes: List<ImageNode>,
    currentImageNodeIndex: Int,
    currentImageNode: ImageNode?,
    photoState: PhotoState,
    onImageTap: () -> Unit,
    topAppBar: @Composable (ImageNode) -> Unit,
    bottomAppBar: @Composable (ImageNode, Int) -> Unit,
    downloadImage: suspend (ImageNode) -> Flow<ImageResult>,
    onClickVideoPlay: (ImageNode) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        HorizontalPager(
            modifier = Modifier
                .fillMaxSize(),
            state = pagerState,
            beyondBoundsPageCount = 5,
            key = { imageNodes.getOrNull(it)?.id?.longValue ?: -1L }
        ) { index ->
            val imageNode = imageNodes[index]

            val imageResult by produceState<ImageResult?>(initialValue = null) {
                downloadImage(imageNode).collectLatest { imageResult ->
                    value = imageResult
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                val isVideo = imageNode.type is VideoFileTypeInfo
                ImageContent(
                    fullSizePath = imageResult?.getHighestResolutionAvailableUri(),
                    photoState = photoState,
                    onImageTap = onImageTap,
                    enableZoom = !isVideo
                )
                if (isVideo) {
                    IconButton(
                        modifier = Modifier.align(Alignment.Center),
                        onClick = { onClickVideoPlay(imageNode) }
                    ) {
                        Icon(
                            painter = painterResource(id = RExoPlayer.drawable.exo_icon_play),
                            contentDescription = "Image Preview play video",
                            tint = Color.White,
                        )
                    }
                }

                val progress = imageResult?.getProgressPercentage() ?: 0L
                if (progress in 1 until 100) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        progress = progress.toFloat(),
                        color = MaterialTheme.colors.secondary,
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .wrapContentSize()
                .align(Alignment.TopCenter)
        ) {
            currentImageNode?.let { topAppBar(it) }
        }
        Box(
            modifier = Modifier
                .wrapContentSize()
                .align(Alignment.BottomCenter)
        ) {
            currentImageNode?.let { bottomAppBar(it, currentImageNodeIndex) }
        }
    }
}

@Composable
private fun ImageContent(
    fullSizePath: String?,
    photoState: PhotoState,
    onImageTap: () -> Unit,
    enableZoom: Boolean,
) {
    PhotoBox(
        modifier = Modifier.fillMaxSize(),
        state = photoState,
        enabled = enableZoom,
        onTap = {
            onImageTap()
        }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(fullSizePath)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ImagePreviewTopBar(
    modifier: Modifier = Modifier,
    showSlideshowMenu: Boolean,
    showSaveToDeviceMenu: Boolean,
    showManageLinkMenu: Boolean,
    showSendToMenu: Boolean,
    onClickBack: () -> Unit,
    onClickSlideshow: () -> Unit,
    onClickSaveToDevice: () -> Unit,
    onClickGetLink: () -> Unit,
    onClickSendTo: () -> Unit,
    onClickMore: () -> Unit,
) {
    TopAppBar(
        title = {},
        backgroundColor = MaterialTheme.colors.white_alpha_070_grey_alpha_070,
        navigationIcon = {
            IconButton(onClick = onClickBack) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back_white),
                    contentDescription = "Image Preview Back",
                    tint = MaterialTheme.colors.black_white,
                )
            }
        },
        actions = {
            if (showSlideshowMenu) {
                IconButton(onClick = onClickSlideshow) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_slideshow),
                        contentDescription = null,
                        tint = MaterialTheme.colors.black_white,
                    )
                }
            }

            if (showSaveToDeviceMenu) {
                IconButton(onClick = onClickSaveToDevice) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_download_white),
                        contentDescription = null,
                        tint = MaterialTheme.colors.black_white,
                    )
                }
            }

            if (showManageLinkMenu) {
                IconButton(onClick = onClickGetLink) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_link),
                        contentDescription = null,
                        tint = MaterialTheme.colors.black_white,
                    )
                }
            }

            if (showSendToMenu) {
                IconButton(onClick = onClickSendTo) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_send_to_contact),
                        contentDescription = null,
                        tint = MaterialTheme.colors.black_white,
                    )
                }
            }

            IconButton(onClick = onClickMore) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_dots_vertical_white),
                    contentDescription = null,
                    tint = MaterialTheme.colors.black_white,
                )
            }
        },
        elevation = 0.dp,
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun ImagePreviewBottomBar(
    modifier: Modifier = Modifier,
    imageName: String = "",
    imageIndex: String = "",
) {
    BottomAppBar(
        backgroundColor = MaterialTheme.colors.white_alpha_070_grey_alpha_070,
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MiddleEllipsisText(imageName)
            MiddleEllipsisText(imageIndex)
        }
    }
}
