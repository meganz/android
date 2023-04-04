package mega.privacy.android.app.presentation.slideshow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.yield
import mega.privacy.android.app.R
import mega.privacy.android.app.imageviewer.ImageViewerViewModel
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.photos.PhotosViewModel
import mega.privacy.android.app.presentation.photos.model.PhotoDownload
import mega.privacy.android.app.presentation.slideshow.view.PhotoBox
import mega.privacy.android.app.presentation.slideshow.view.PhotoState
import mega.privacy.android.app.presentation.slideshow.view.rememberPhotoState
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.grey_alpha_070
import mega.privacy.android.core.ui.theme.white
import mega.privacy.android.core.ui.theme.white_alpha_070
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.slideshow.SlideshowOrder
import mega.privacy.android.domain.entity.slideshow.SlideshowSpeed
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

/**
 * Slideshow fragment
 */
@AndroidEntryPoint
class SlideshowFragment : Fragment() {
    private val slideshowViewModel: SlideshowViewModel by viewModels()
    private val imageViewerViewModel: ImageViewerViewModel by activityViewModels()
    private val photosViewModel: PhotosViewModel by viewModels()

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
                    SlideshowBody(
                        photoDownload = photosViewModel::downloadPhoto
                    )
                }
            }
        }
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
    }

    @OptIn(ExperimentalPagerApi::class)
    @Composable
    private fun SlideshowBody(
        photoDownload: PhotoDownload,
    ) {
        val slideshowViewState by slideshowViewModel.state.collectAsStateWithLifecycle()
        val scrollState = rememberScaffoldState()
        val pagerState = rememberPagerState()
        val items = slideshowViewState.items
        val order = slideshowViewState.order ?: SlideshowOrder.Shuffle
        val speed = slideshowViewState.speed ?: SlideshowSpeed.Normal
        val repeat = slideshowViewState.repeat
        var isPlaying by remember {
            mutableStateOf(true)
        }

        val playItems = remember(items) {
            when (order) {
                SlideshowOrder.Shuffle -> items.shuffled()
                SlideshowOrder.Newest -> items.sortedByDescending { it.modificationTime }
                SlideshowOrder.Oldest -> items.sortedBy { it.modificationTime }
            }
        }
        val photoState = rememberPhotoState()
        LaunchedEffect(isPlaying) {
            if (isPlaying) {
                while (true) {
                    yield()
                    delay(speed.duration * 1000L)
                    tween<Float>(600)
                    if (isPlaying) {
                        pagerState.animateScrollToPage(
                            page = (pagerState.currentPage + 1) % (pagerState.pageCount)
                        )
                    }
                }
            }
        }

        // When move to next, reset scale
        LaunchedEffect(pagerState, repeat) {
            snapshotFlow { pagerState.currentPage }.distinctUntilChanged().collect { page ->
                photoState.resetScale()

                imageViewerViewModel.images.value?.let { sourceImages ->
                    // handle repeat playing
                    if (page == sourceImages.size.minus(1) && repeat.not()) {
                        isPlaying = false
                        // Make sure the last picture completely showed
                        pagerState.animateScrollToPage(
                            page = pagerState.pageCount - 1
                        )
                    }

                    if (page == pagerState.pageCount.minus(1)) {
                        slideshowViewModel.playSlideshowItems(sourceImages)
                    }
                }
            }
        }

        // Observe if scaling, then pause slideshow
        LaunchedEffect(photoState.isScaled) {
            snapshotFlow { photoState.isScaled }.collect { isScaled ->
                if (isScaled) {
                    isPlaying = false
                }
            }
        }

        SlideshowCompose(
            scrollState = scrollState,
            playItems = playItems,
            pagerState = pagerState,
            photoState = photoState,
            photoDownload = photoDownload,
            isPlaying = isPlaying,
            onClick = { isPlaying = !isPlaying }
        )
    }

    @OptIn(ExperimentalPagerApi::class)
    @Composable
    private fun SlideshowCompose(
        scrollState: ScaffoldState = rememberScaffoldState(),
        playItems: List<Photo>,
        pagerState: PagerState,
        photoState: PhotoState,
        photoDownload: PhotoDownload,
        isPlaying: Boolean,
        onClick: () -> Unit,
    ) {

        var showBottomPanel by remember {
            mutableStateOf(true)
        }

        Scaffold(
            scaffoldState = scrollState,
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                HorizontalPager(
                    modifier = Modifier.fillMaxSize(),
                    count = playItems.size,
                    state = pagerState,
                    key = { playItems[it].id }
                ) { index ->

                    val imageState = produceState<String?>(initialValue = null) {

                        val photo = playItems[index]
                        val isPreview = true
                        photoDownload(
                            isPreview,
                            photo,
                        ) { downloadSuccess ->
                            if (downloadSuccess) {
                                value = photo.previewFilePath
                            }
                        }
                    }

                    PhotoBox(
                        state = photoState,
                        onTap = {
                            if (showBottomPanel.not()) {
                                showBottomPanel = true
                                imageViewerViewModel.showToolbar(showBottomPanel)
                            }
                        }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageState.value)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            placeholder = painterResource(id = R.drawable.ic_image_thumbnail),
                            error = painterResource(id = R.drawable.ic_image_thumbnail),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (showBottomPanel) {
                    LaunchedEffect(true) {
                        delay(5000L)
                        showBottomPanel = false
                        imageViewerViewModel.showToolbar(showBottomPanel)
                    }
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
                        IconButton(onClick = onClick) {
                            Icon(
                                painter = if (isPlaying)
                                    painterResource(id = R.drawable.ic_pause)
                                else
                                    painterResource(id = R.drawable.ic_play),
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

