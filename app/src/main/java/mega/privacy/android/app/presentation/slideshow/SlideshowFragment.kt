@file:OptIn(ExperimentalFoundationApi::class)

package mega.privacy.android.app.presentation.slideshow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.view.MenuProvider
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.yield
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.imageviewer.ImageViewerViewModel
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.slideshow.model.SlideshowItem
import mega.privacy.android.app.presentation.slideshow.view.PhotoBox
import mega.privacy.android.app.presentation.slideshow.view.PhotoState
import mega.privacy.android.app.presentation.slideshow.view.rememberPhotoState
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.grey_alpha_070
import mega.privacy.android.core.ui.theme.white
import mega.privacy.android.core.ui.theme.white_alpha_070
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.slideshow.SlideshowOrder
import mega.privacy.android.domain.entity.slideshow.SlideshowSpeed
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.mobile.analytics.event.SlideShowScreenEvent
import timber.log.Timber
import javax.inject.Inject

/**
 * Slideshow fragment
 */
@AndroidEntryPoint
class SlideshowFragment : Fragment() {
    private val slideshowViewModel: SlideshowViewModel by viewModels()
    private val imageViewerViewModel: ImageViewerViewModel by activityViewModels()

    @Inject
    lateinit var getThemeMode: GetThemeMode

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val mode by getThemeMode()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                AndroidTheme(isDark = mode.isDarkMode()) {
                    SlideshowBody()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Analytics.tracker.trackEvent(SlideShowScreenEvent)
        Firebase.crashlytics.log("Screen view: ${SlideShowScreenEvent.eventName}")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObserver()
        setupMenu()
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.fragment_image_slideshow, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                return when (menuItem.itemId) {
                    R.id.action_options -> {
                        getNavController()?.navigate(
                            SlideshowFragmentDirections.actionNewSlideshowToSlideshowSettings()
                        )
                        true
                    }

                    else -> true
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun getNavController(): NavController? =
        activity?.let {
            it.supportFragmentManager
                .findFragmentById(R.id.images_nav_host_fragment) as NavHostFragment
        }?.navController

    private fun setupObserver() {
        imageViewerViewModel.images.observe(viewLifecycleOwner) { items ->
            items?.let {
                if (it.isNotEmpty()) {
                    slideshowViewModel.setData(it)
                }
            }
        }
        imageViewerViewModel.onShowToolbar().observe(viewLifecycleOwner) {
            WindowCompat.setDecorFitsSystemWindows(requireActivity().window, it.show)
        }
    }

    override fun onStop() {
        if (activity?.isChangingConfigurations != true && activity?.isFinishing != true) {
            slideshowViewModel.updateIsPlaying(false)
        }
        super.onStop()
    }

    override fun onDestroyView() {
        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, false)
        super.onDestroyView()
    }

    @Composable
    private fun SlideshowBody() {
        val slideshowViewState by slideshowViewModel.state.collectAsStateWithLifecycle()
        val scaffoldState = rememberScaffoldState()
        val photoState = rememberPhotoState()
        val items = slideshowViewState.slideshowItems
        val order = slideshowViewState.order ?: SlideshowOrder.Shuffle
        val speed = slideshowViewState.speed ?: SlideshowSpeed.Normal
        val repeat = slideshowViewState.repeat
        val isPlaying = slideshowViewState.isPlaying
        val pagerState = rememberPagerState(
            initialPage = 0,
            initialPageOffsetFraction = 0f
        ) {
            items.size
        }

        SlideshowCompose(
            scaffoldState = scaffoldState,
            playItems = items,
            pagerState = pagerState,
            photoState = photoState,
            isPlaying = isPlaying,
            onPlayIconClick = {
                photoState.resetScale()
                slideshowViewModel.updateIsPlaying(!isPlaying)
            },
            onImageTap = {
                slideshowViewModel.updateIsPlaying(false)
            }
        ) {
            LaunchedEffect(pagerState.canScrollForward) {
                // Not repeat and the last one.
                if (!pagerState.canScrollForward && !repeat && isPlaying) {
                    Timber.d("Slideshow canScrollForward")
                    slideshowViewModel.updateIsPlaying(false)
                }
            }
        }

        LaunchedEffect(isPlaying) {
            if (isPlaying) {
                this@SlideshowFragment.view?.keepScreenOn = true
                imageViewerViewModel.showToolbar(false)
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
            } else {
                this@SlideshowFragment.view?.keepScreenOn = false
                imageViewerViewModel.showToolbar(true)
            }
        }

        LaunchedEffect(Unit) {
            // When order change, restart slideshow
            snapshotFlow { order }.distinctUntilChanged().collect {
                if (slideshowViewState.shouldPlayFromFirst) {
                    Timber.d("Slideshow shouldPlayFromFirst")
                    pagerState.animateScrollToPage(0)
                    slideshowViewModel.shouldPlayFromFirst(shouldPlayFromFirst = false)
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
                slideshowViewModel.updateIsPlaying(false)
            }
        }
    }

    @Composable
    private fun SlideshowCompose(
        scaffoldState: ScaffoldState = rememberScaffoldState(),
        playItems: List<SlideshowItem>,
        pagerState: PagerState,
        photoState: PhotoState,
        isPlaying: Boolean,
        onPlayIconClick: () -> Unit,
        onImageTap: ((Offset) -> Unit),
        handleEffectComposable: @Composable () -> Unit,
    ) {
        Scaffold(
            scaffoldState = scaffoldState,
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                HorizontalPager(
                    modifier = Modifier
                        .fillMaxSize(),
                    state = pagerState,
                    beyondBoundsPageCount = 5,
                    key = { playItems.getOrNull(it)?.photo?.id ?: -1L }
                ) { index ->

                    val slideshowItem = playItems[index]
                    val imageState =
                        produceState<String?>(initialValue = null) {
                            runCatching {
                                slideshowViewModel.downloadFullSizeImage(
                                    slideshowItem = slideshowItem
                                ).collectLatest { imageResult ->
                                    value = imageResult.previewUri ?: imageResult.thumbnailUri
                                }
                            }.onFailure { exception ->
                                Timber.d("Failed to load image: $exception")
                            }
                        }

                    PhotoBox(
                        modifier = Modifier.fillMaxSize(),
                        state = photoState,
                        onTap = onImageTap
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageState.value)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    handleEffectComposable()
                }

                if (!isPlaying) {
                    Row(
                        modifier = Modifier
                            .height(72.dp)
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                if (MaterialTheme.colors.isLight)
                                    white_alpha_070
                                else
                                    grey_alpha_070
                            ),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onPlayIconClick) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_play),
                                contentDescription = null,
                                modifier = Modifier,
                                tint = if (MaterialTheme.colors.isLight)
                                    black
                                else
                                    white,
                            )
                        }
                    }
                }
            }
        }
    }
}

