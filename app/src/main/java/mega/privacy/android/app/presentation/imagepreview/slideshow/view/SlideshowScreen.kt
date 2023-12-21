@file:OptIn(ExperimentalFoundationApi::class)

package mega.privacy.android.app.presentation.imagepreview.slideshow.view

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.yield
import mega.privacy.android.app.R.drawable
import mega.privacy.android.app.R.string
import mega.privacy.android.app.presentation.imagepreview.slideshow.SlideshowViewModel
import mega.privacy.android.app.presentation.imagepreview.slideshow.model.SlideshowMenuAction.SettingOptionsMenuAction
import mega.privacy.android.app.presentation.slideshow.view.PhotoBox
import mega.privacy.android.app.presentation.slideshow.view.PhotoState
import mega.privacy.android.app.presentation.slideshow.view.rememberPhotoState
import mega.privacy.android.core.ui.controls.appbar.AppBarType
import mega.privacy.android.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.core.ui.theme.extensions.black_white
import mega.privacy.android.core.ui.theme.extensions.white_alpha_070_grey_alpha_070
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.slideshow.SlideshowOrder
import mega.privacy.android.domain.entity.slideshow.SlideshowSpeed
import timber.log.Timber

@Composable
fun SlideshowScreen(
    viewModel: SlideshowViewModel = hiltViewModel(),
    onClickSettingMenu: () -> Unit,
) {
    val viewState by viewModel.state.collectAsStateWithLifecycle()
    val imageNodes = viewState.imageNodes
    if (imageNodes.isNotEmpty()) {
        val scaffoldState = rememberScaffoldState()
        val photoState = rememberPhotoState()
        val order = viewState.order ?: SlideshowOrder.Shuffle
        val speed = viewState.speed ?: SlideshowSpeed.Normal
        val repeat = viewState.repeat
        val isPlaying = viewState.isPlaying
        val pagerState = rememberPagerState(
            initialPage = 0,
            initialPageOffsetFraction = 0f
        ) {
            imageNodes.size
        }
        val onBackPressedDispatcher =
            LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

        Scaffold(
            scaffoldState = scaffoldState,
            topBar = {
                if (!isPlaying) {
                    SlideshowTopBar(
                        onClickBack = { onBackPressedDispatcher?.onBackPressed() },
                        onClickSettingMenu = onClickSettingMenu
                    )
                }
            },
            bottomBar = {
                if (!isPlaying) {
                    SlideshowBottomBar(
                        onPlayOrPauseSlideshow = {
                            photoState.resetScale()
                            viewModel.updateIsPlaying(isPlaying = true)
                        },
                    )
                }
            },
        ) { paddingValues ->
            SlideShowContent(
                paddingValues = paddingValues,
                pagerState = pagerState,
                imageNodes = imageNodes,
                downloadImage = viewModel::monitorImageResult,
                getImagePath = viewModel::getHighestResolutionImagePath,
                getErrorImagePath = viewModel::getFallbackImagePath,
                photoState = photoState,
                onTapImage = { viewModel.updateIsPlaying(false) },
            ) {
                LaunchedEffect(pagerState.canScrollForward) {
                    // Not repeat and the last one.
                    if (!pagerState.canScrollForward && !repeat && isPlaying) {
                        Timber.d("Slideshow canScrollForward")
                        viewModel.updateIsPlaying(false)
                    }
                }
            }
        }

        LaunchedEffect(isPlaying) {
            if (isPlaying) {
                while (true) {
                    yield()
                    delay(speed.duration * 1000L)
                    tween<Float>(600)
                    try {
                        pagerState.animateScrollToPage(
                            page = if (pagerState.canScrollForward) {
                                pagerState.currentPage + 1
                            } else {
                                0
                            },
                        )
                    } catch (e: Exception) {
                        Timber.d("Slideshow animateScrollToPage+$e")
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            // When order change, restart slideshow
            snapshotFlow { order }.distinctUntilChanged().collect {
                if (viewState.shouldPlayFromFirst) {
                    Timber.d("Slideshow shouldPlayFromFirst")
                    pagerState.animateScrollToPage(0)
                    viewModel.shouldPlayFromFirst(shouldPlayFromFirst = false)
                }
            }
        }

        LaunchedEffect(pagerState.currentPage) {
            // When move to next, reset scale
            if (photoState.isScaled) {
                Timber.d("Slideshow reset scaled")
                photoState.resetScale()
            }
        }

        LaunchedEffect(photoState.isScaled) {
            // Observe if scaling, then pause slideshow
            if (photoState.isScaled) {
                Timber.d("Slideshow is scaling")
                viewModel.updateIsPlaying(false)
            }
        }
    }
}

@Composable
private fun SlideshowBottomBar(
    onPlayOrPauseSlideshow: () -> Unit,
) {
    BottomAppBar(
        backgroundColor = MaterialTheme.colors.white_alpha_070_grey_alpha_070,
        elevation = 0.dp,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onPlayOrPauseSlideshow) {
                Icon(
                    painter = painterResource(id = drawable.ic_play),
                    contentDescription = null,
                    tint = MaterialTheme.colors.black_white,
                )
            }
        }
    }
}

@Composable
private fun SlideshowTopBar(
    onClickBack: () -> Unit,
    onClickSettingMenu: () -> Unit,
) {
    MegaAppBar(
        title = stringResource(string.action_slideshow),
        appBarType = AppBarType.BACK_NAVIGATION,
        elevation = 0.dp,
        onNavigationPressed = {
            onClickBack()
        },
        actions = listOf(
            SettingOptionsMenuAction
        ),
        onActionPressed = {
            onClickSettingMenu()
        }
    )
}

@Composable
private fun SlideShowContent(
    paddingValues: PaddingValues,
    pagerState: PagerState,
    photoState: PhotoState,
    imageNodes: List<ImageNode>,
    downloadImage: suspend (ImageNode) -> Flow<ImageResult>,
    getImagePath: suspend (ImageResult?) -> String?,
    getErrorImagePath: suspend (ImageResult?) -> String?,
    onTapImage: () -> Unit,
    handleEffectComposable: @Composable () -> Unit,
) {
    Box(modifier = Modifier.padding(paddingValues)) {
        HorizontalPager(
            modifier = Modifier
                .fillMaxSize(),
            state = pagerState,
            beyondBoundsPageCount = 5,
            key = { imageNodes.getOrNull(it)?.id?.longValue ?: -1L }
        ) { index ->

            val imageNode = imageNodes[index]
            val imageResultPair by produceState<Pair<String?, String?>>(
                initialValue = Pair(null, null)
            ) {
                downloadImage(imageNode).collectLatest { imageResult ->
                    value = Pair(
                        getImagePath(imageResult),
                        getErrorImagePath(imageResult)
                    )
                }
            }

            val (fullSizePath, errorImagePath) = imageResultPair

            ImageContent(
                photoState = photoState,
                onTapImage = onTapImage,
                fullSizePath = fullSizePath,
                errorImagePath = errorImagePath
            )

            handleEffectComposable()
        }
    }
}

@Composable
private fun ImageContent(
    photoState: PhotoState,
    onTapImage: () -> Unit,
    fullSizePath: String?,
    errorImagePath: String?,
) {
    PhotoBox(
        modifier = Modifier.fillMaxSize(),
        state = photoState,
        onTap = { onTapImage() }
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

        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth()
        )
    }
}