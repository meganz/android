@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)

package mega.privacy.android.app.presentation.imagepreview.view

import android.content.Context
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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel
import mega.privacy.android.app.presentation.slideshow.view.PhotoBox
import mega.privacy.android.app.presentation.slideshow.view.PhotoState
import mega.privacy.android.app.presentation.slideshow.view.rememberPhotoState
import mega.privacy.android.core.ui.theme.extensions.black_white
import mega.privacy.android.core.ui.theme.extensions.white_alpha_070_grey_alpha_070
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.node.ImageNode

@Composable
fun ImagePreviewScreen(
    viewModel: ImagePreviewViewModel = viewModel(),
    onBackClicked: () -> Unit,
    onSaveToDeviceClicked: () -> Unit,
) {
    val viewState by viewModel.state.collectAsStateWithLifecycle()
    val imageNodes = viewState.imageNodes
    if (imageNodes.isNotEmpty()) {
        val currentImageNodeId = viewState.currentImageNodeId
        val initialPage = remember {
            imageNodes.withIndex().first {
                currentImageNodeId.longValue == it.value.id.longValue
            }.index
        }
        val inFullScreenMode = viewState.inFullScreenMode
        val scaffoldState = rememberScaffoldState()
        val photoState = rememberPhotoState()
        val pagerState = rememberPagerState(
            initialPage = initialPage,
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
            snapshotFlow { pagerState.currentPage }.collect { page ->
                imageNodes.getOrNull(page)?.id?.let { viewModel.setCurrentImageNodeId(it) }
            }
        }
        Scaffold(
            scaffoldState = scaffoldState,
            content = { innerPadding ->
                ImagePreviewContent(
                    innerPadding = innerPadding,
                    getInfoText = viewModel::getInfoText,
                    modalSheetState = modalSheetState,
                    pagerState = pagerState,
                    imageNodes = imageNodes,
                    photoState = photoState,
                    downloadImage = viewModel::monitorImageResult,
                    onImageTap = { viewModel.switchFullScreenMode() },
                    topAppBar = { _ ->
                        AnimatedVisibility(
                            visible = !inFullScreenMode,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            ImagePreviewTopBar(
                                showSlideshowMenu = true,
                                showSaveToDeviceMenu = true,
                                showLinkMenu = true,
                                showForwardMenu = true,
                                onBackClicked = onBackClicked,
                                onSlideshowClicked = {},
                                onSaveToDeviceClicked = onSaveToDeviceClicked,
                                onLinkClicked = {},
                                onForwardClicked = {},
                                onMoreClicked = {
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
            },
        )
    }
}

@Composable
private fun ImagePreviewContent(
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues,
    pagerState: PagerState,
    imageNodes: List<ImageNode>,
    photoState: PhotoState,
    onImageTap: () -> Unit,
    topAppBar: @Composable (ImageNode) -> Unit,
    bottomAppBar: @Composable (ImageNode, Int) -> Unit,
    downloadImage: suspend (ImageNode) -> Flow<ImageResult>,
    getInfoText: (ImageNode, Context) -> String,
    modalSheetState: ModalBottomSheetState,
) {
    val context = LocalContext.current
    Box(modifier = modifier.background(color = Color.Black)) {
        HorizontalPager(
            modifier = Modifier
                .fillMaxSize(),
            state = pagerState,
            beyondBoundsPageCount = 5,
            key = { imageNodes.getOrNull(it)?.id?.longValue ?: -1L }
        ) { index ->
            val currentImageNode = imageNodes[index]
            val currentImageNodeInfo = remember(index) {
                getInfoText(
                    currentImageNode,
                    context,
                )
            }

            ImagePreviewBottomSheet(
                modalSheetState = modalSheetState,
                imageThumbnailPath = currentImageNode.thumbnailPath,
                imageName = currentImageNode.name,
                imageInfo = currentImageNodeInfo,
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ImageContent(
                            imageNode = currentImageNode,
                            downloadImage = downloadImage,
                            photoState = photoState,
                            onImageTap = onImageTap
                        )
                    }
                    Box(
                        modifier = Modifier
                            .wrapContentSize()
                            .align(Alignment.TopCenter)
                    ) {
                        topAppBar(currentImageNode)
                    }
                    Box(
                        modifier = Modifier
                            .wrapContentSize()
                            .align(Alignment.BottomCenter)
                    ) {
                        bottomAppBar(currentImageNode, index)
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageContent(
    imageNode: ImageNode,
    downloadImage: suspend (ImageNode) -> Flow<ImageResult>,
    photoState: PhotoState,
    onImageTap: () -> Unit,
) {
    val imageState by produceState<String?>(initialValue = null) {
        downloadImage(imageNode).collectLatest { imageResult ->
            value = imageResult.fullSizeUri ?: imageResult.previewUri
                    ?: imageResult.thumbnailUri
        }
    }

    PhotoBox(
        modifier = Modifier.fillMaxSize(),
        state = photoState,
        onTap = {
            onImageTap()
        }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageState)
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
    showLinkMenu: Boolean,
    showForwardMenu: Boolean,
    onBackClicked: () -> Unit,
    onSlideshowClicked: () -> Unit,
    onSaveToDeviceClicked: () -> Unit,
    onLinkClicked: () -> Unit,
    onForwardClicked: () -> Unit,
    onMoreClicked: () -> Unit,
) {
    TopAppBar(
        title = {},
        backgroundColor = MaterialTheme.colors.white_alpha_070_grey_alpha_070,
        navigationIcon = {
            IconButton(onClick = onBackClicked) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back_white),
                    contentDescription = "Image Preview Back",
                    tint = MaterialTheme.colors.black_white,
                )
            }
        },
        actions = {
            if (showSlideshowMenu) {
                IconButton(onClick = onSlideshowClicked) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_slideshow),
                        contentDescription = null,
                        tint = MaterialTheme.colors.black_white,
                    )
                }
            }

            if (showSaveToDeviceMenu) {
                IconButton(onClick = onSaveToDeviceClicked) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_download_white),
                        contentDescription = null,
                        tint = MaterialTheme.colors.black_white,
                    )
                }
            }

            if (showLinkMenu) {
                IconButton(onClick = onLinkClicked) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_link),
                        contentDescription = null,
                        tint = MaterialTheme.colors.black_white,
                    )
                }
            }

            if (showForwardMenu) {
                IconButton(onClick = onForwardClicked) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_send_to_contact),
                        contentDescription = null,
                        tint = MaterialTheme.colors.black_white,
                    )
                }
            }

            IconButton(onClick = onMoreClicked) {
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
            Text(imageName)
            Text(imageIndex)
        }
    }
}
