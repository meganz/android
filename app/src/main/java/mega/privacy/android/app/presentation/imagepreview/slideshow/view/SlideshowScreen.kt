@file:OptIn(ExperimentalFoundationApi::class)

package mega.privacy.android.app.presentation.imagepreview.slideshow.view

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import me.saket.telephoto.zoomable.DoubleClickToZoomListener
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.ZoomableImageState
import me.saket.telephoto.zoomable.ZoomableState
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R.drawable
import mega.privacy.android.app.R.string
import mega.privacy.android.app.presentation.imagepreview.slideshow.SlideshowViewModel
import mega.privacy.android.app.presentation.imagepreview.slideshow.model.SlideshowMenuAction.SettingOptionsMenuAction
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.slideshow.SlideshowOrder
import mega.privacy.android.domain.entity.slideshow.SlideshowSpeed
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.theme.extensions.black_white
import mega.privacy.android.shared.original.core.ui.theme.extensions.white_alpha_070_grey_alpha_070
import mega.privacy.mobile.analytics.event.SlideShowScreenEvent
import timber.log.Timber

@Composable
fun SlideshowScreen(
    onClickSettingMenu: () -> Unit,
    onClickBack: () -> Unit,
    viewModel: SlideshowViewModel = hiltViewModel(),
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
) {
    val viewState by viewModel.state.collectAsStateWithLifecycle()
    val imageNodes = viewState.imageNodes

    if (viewState.isInitialized && imageNodes.isEmpty()) {
        LaunchedEffect(Unit) {
            onClickBack()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Analytics.tracker.trackEvent(SlideShowScreenEvent)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (imageNodes.isNotEmpty()) {
        val zoomableStateMap = remember { mutableMapOf<NodeId, ZoomableState?>() }

        val scaffoldState = rememberScaffoldState()
        val coroutineScope = rememberCoroutineScope()
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

        MegaScaffold(
            modifier = Modifier.systemBarsPadding(),
            scaffoldState = scaffoldState,
            topBar = {
                if (!isPlaying) {
                    SlideshowTopBar(
                        onClickBack = onClickBack,
                        onClickSettingMenu = onClickSettingMenu
                    )
                }
            },
            bottomBar = {
                if (!isPlaying) {
                    SlideshowBottomBar(
                        onPlayOrPauseSlideshow = {
                            coroutineScope.launch {
                                val page = pagerState.currentPage
                                for (candidatePage in page - 1..page + 1) {
                                    viewState.imageNodes.getOrNull(candidatePage)?.let { node ->
                                        zoomableStateMap[node.id]?.resetZoom()
                                    }
                                }
                                viewModel.updateIsPlaying(isPlaying = true)
                            }
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
                onTapImage = { viewModel.updateIsPlaying(false) },
                onImageZooming = { viewModel.updateIsPlaying(false) },
                onCacheImageState = { node, zoomState ->
                    zoomableStateMap[node.id] = zoomState
                }
            )
        }

        LaunchedEffect(isPlaying) {
            if (isPlaying) {
                while (true) {
                    yield()
                    delay(speed.duration.inWholeMilliseconds)
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
                    pagerState.animateScrollToPage(0)
                    viewModel.shouldPlayFromFirst(shouldPlayFromFirst = false)
                }
            }
        }

        LaunchedEffect(pagerState.currentPage) {
            val page = pagerState.currentPage

            for (candidatePage in page - 1..page + 1) {
                viewState.imageNodes.getOrNull(candidatePage)?.let { node ->
                    coroutineScope.launch {
                        zoomableStateMap[node.id]?.resetZoom()
                    }
                }
            }
        }

        LaunchedEffect(pagerState.canScrollForward) {
            // Not repeat and the last one.
            if (!pagerState.canScrollForward && !repeat && isPlaying) {
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
    imageNodes: List<ImageNode>,
    downloadImage: suspend (ImageNode) -> Flow<ImageResult>,
    getImagePath: suspend (ImageResult?) -> String?,
    getErrorImagePath: suspend (ImageResult?) -> String?,
    onTapImage: () -> Unit,
    onImageZooming: (ZoomableState) -> Unit,
    onCacheImageState: (ImageNode, ZoomableState) -> Unit,
) {

    Box(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
    ) {
        HorizontalPager(
            modifier = Modifier
                .fillMaxSize(),
            state = pagerState,
            beyondBoundsPageCount = minOf(3, imageNodes.size),
            key = { imageNodes.getOrNull(it)?.id?.longValue ?: -1L }
        ) { index ->

            val imageNode = imageNodes[index]
            val imageResultPair by produceState<Pair<String?, String?>>(
                initialValue = Pair(null, null),
                key1 = imageNode
            ) {
                downloadImage(imageNode).collectLatest { imageResult ->
                    value = Pair(
                        getImagePath(imageResult),
                        getErrorImagePath(imageResult)
                    )
                }
            }

            val (fullSizePath, errorImagePath) = imageResultPair

            val zoomableState = rememberZoomableState(
                zoomSpec = ZoomSpec(maxZoomFactor = Int.MAX_VALUE.toFloat())
            )
            val imageState = rememberZoomableImageState(zoomableState)
            onCacheImageState(imageNode, imageState.zoomableState)

            LaunchedEffect(zoomableState.zoomFraction) {
                val fraction = zoomableState.zoomFraction
                if (fraction != null && fraction > 0.0f) {
                    onImageZooming(zoomableState)
                }
            }

            ImageContent(
                imageState = imageState,
                onTapImage = onTapImage,
                fullSizePath = if (imageNode.serializedData == "localFile") imageNode.fullSizePath else fullSizePath,
                errorImagePath = if (imageNode.serializedData == "localFile") imageNode.fullSizePath else errorImagePath,
            )
        }
    }
}

@Composable
private fun ImageContent(
    imageState: ZoomableImageState,
    onTapImage: () -> Unit,
    fullSizePath: String?,
    errorImagePath: String?,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
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

        ZoomableAsyncImage(
            model = request,
            state = imageState,
            contentDescription = "Image Preview",
            modifier = Modifier
                .fillMaxSize(),
            onClick = {
                onTapImage()
            },
            onDoubleClick = DoubleClickToZoomListener.cycle(maxZoomFactor = 3f),
        )
    }
}