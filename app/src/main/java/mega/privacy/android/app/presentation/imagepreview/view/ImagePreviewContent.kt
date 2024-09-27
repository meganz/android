package mega.privacy.android.app.presentation.imagepreview.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.imagepreview.slideshow.view.PhotoState
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.shared.original.core.ui.theme.grey_100

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ImagePreviewContent(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    imageNodes: List<ImageNode>,
    currentImageNodeIndex: Int,
    currentImageNode: ImageNode,
    photoState: PhotoState,
    isMagnifierMode: Boolean,
    onImageTap: () -> Unit,
    topAppBar: @Composable (ImageNode) -> Unit,
    bottomAppBar: @Composable (ImageNode, Int) -> Unit,
    downloadImage: suspend (ImageNode) -> Flow<ImageResult>,
    getImagePath: suspend (ImageResult?) -> String?,
    getErrorImagePath: suspend (ImageResult?) -> String?,
    onClickVideoPlay: (ImageNode) -> Unit,
    onCloseMagnifier: () -> Unit,
) {
    var isDraggingMagnifier by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (!isMagnifierMode) {
            HorizontalPager(
                modifier = Modifier.fillMaxSize(),
                state = pagerState,
                beyondBoundsPageCount = minOf(5, imageNodes.size),
                key = { imageNodes.getOrNull(it)?.id?.longValue ?: -1L },
            ) { index ->
                val imageNode = imageNodes.getOrNull(index)
                if (imageNode != null) {
                    val imageResultTriple by produceState<Triple<ImageResult?, String?, String?>>(
                        initialValue = Triple(null, null, null)
                    ) {
                        downloadImage(imageNode).collectLatest { imageResult ->
                            value = Triple(
                                imageResult,
                                getImagePath(imageResult),
                                getErrorImagePath(imageResult)
                            )
                        }
                    }

                    val (imageResult, imagePath, errorImagePath) = imageResultTriple

                    ImagePreviewContent(
                        imageNode = imageNode,
                        photoState = photoState,
                        imageResult = imageResult,
                        imagePath = imagePath,
                        errorImagePath = errorImagePath,
                        isMagnifierMode = false,
                        onImageTap = onImageTap,
                        onClickVideoPlay = onClickVideoPlay,
                        onDragMagnifier = {}
                    )
                }
            }
        } else {
            val imageResultTriple by produceState<Triple<ImageResult?, String?, String?>>(
                initialValue = Triple(null, null, null)
            ) {
                downloadImage(currentImageNode).collectLatest { imageResult ->
                    value = Triple(
                        imageResult,
                        getImagePath(imageResult),
                        getErrorImagePath(imageResult)
                    )
                }
            }

            val (imageResult, imagePath, errorImagePath) = imageResultTriple

            ImagePreviewContent(
                imageNode = currentImageNode,
                photoState = photoState,
                imageResult = imageResult,
                imagePath = imagePath,
                errorImagePath = errorImagePath,
                isMagnifierMode = true,
                onImageTap = onImageTap,
                onClickVideoPlay = onClickVideoPlay,
                onDragMagnifier = { isDraggingMagnifier = it },
            )
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

        if (isMagnifierMode) {
            AnimatedVisibility(
                visible = !isDraggingMagnifier,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Box(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.systemBars)
                            .size(40.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(grey_100)
                            .clickable { onCloseMagnifier() },
                        contentAlignment = Alignment.Center,
                        content = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = "",
                                modifier = Modifier.size(12.dp),
                                tint = Color.Unspecified,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ImagePreviewContent(
    modifier: Modifier = Modifier,
    photoState: PhotoState,
    imageNode: ImageNode,
    imageResult: ImageResult?,
    imagePath: String?,
    errorImagePath: String?,
    isMagnifierMode: Boolean,
    onImageTap: () -> Unit,
    onClickVideoPlay: (ImageNode) -> Unit,
    onDragMagnifier: (Boolean) -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
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
            photoState = photoState,
            onImageTap = onImageTap,
            enableZoom = !isVideo,
            isMagnifierMode = isMagnifierMode,
            onDragMagnifier = onDragMagnifier,
        )
        if (isVideo) {
            IconButton(
                modifier = Modifier.align(Alignment.Center),
                onClick = { onClickVideoPlay(imageNode) }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_play),
                    contentDescription = "Image Preview play video",
                    tint = Color.White,
                )
            }
        }

        val progress = imageResult?.getProgressPercentage() ?: 0L
        if (progress in 1 until 100) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.BottomEnd),
                progress = progress.toFloat() / 100,
                color = MaterialTheme.colors.secondary,
                strokeWidth = 2.dp,
            )
        }
    }
}