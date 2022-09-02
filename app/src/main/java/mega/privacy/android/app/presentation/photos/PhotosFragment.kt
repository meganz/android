package mega.privacy.android.app.presentation.photos

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.fragments.managerFragments.cu.album.AlbumContentFragment
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.photos.albums.AlbumsViewModel
import mega.privacy.android.app.presentation.photos.albums.model.AlbumsViewState
import mega.privacy.android.app.presentation.photos.albums.view.AlbumsView
import mega.privacy.android.app.presentation.photos.model.PhotosTab
import mega.privacy.android.app.presentation.photos.timeline.actionMode.TimelineActionModeCallback
import mega.privacy.android.app.presentation.photos.timeline.model.TimeBarTab
import mega.privacy.android.app.presentation.photos.timeline.model.TimelineViewState
import mega.privacy.android.app.presentation.photos.timeline.photosfilter.PhotosFilterFragment
import mega.privacy.android.app.presentation.photos.timeline.view.EmptyState
import mega.privacy.android.app.presentation.photos.timeline.view.EnableCU
import mega.privacy.android.app.presentation.photos.timeline.view.PhotosGridView
import mega.privacy.android.app.presentation.photos.timeline.view.TimelineView
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.TimelineViewModel
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.onCardClick
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.onTimeBarTabSelected
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.setCUUploadVideos
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.setCUUseCellularConnection
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.setShowProgressBar
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.showEnableCUPage
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.showingFilterPage
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.showingSortByDialog
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.skipCUSetup
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.updateFilterState
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.updateProgress
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.zoomIn
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.zoomOut
import mega.privacy.android.app.presentation.photos.view.PhotosBodyView
import mega.privacy.android.app.presentation.photos.view.showSortByDialog
import mega.privacy.android.app.presentation.theme.AndroidTheme
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetThemeMode
import timber.log.Timber
import javax.inject.Inject

/**
 * PhotosFragment
 */
@OptIn(ExperimentalPagerApi::class)
@AndroidEntryPoint
class PhotosFragment : Fragment() {

    private val photosViewModel: PhotosViewModel by viewModels()
    internal val timelineViewModel: TimelineViewModel by activityViewModels()
    private val albumsViewModel: AlbumsViewModel by activityViewModels()

    internal lateinit var managerActivity: ManagerActivity
    private var menu: Menu? = null

    // Action mode
    private var actionMode: ActionMode? = null
    private lateinit var actionModeCallback: TimelineActionModeCallback

    @Inject
    lateinit var getThemeMode: GetThemeMode
    lateinit var pagerState: PagerState
    lateinit var lazyGridState: LazyGridState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        managerActivity = activity as ManagerActivity
        actionModeCallback = TimelineActionModeCallback(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        timelineViewModel.showingFilterPage(false)
        return ComposeView(requireContext()).apply {
            setContent {
                val mode by getThemeMode()
                    .collectAsState(initial = ThemeMode.System)
                AndroidTheme(isDark = mode.isDarkMode()) {
                    PhotosBody()
                }
            }
        }
    }

    override fun onResume() {
        timelineViewModel.resetCUButtonAndProgress()
        registerCUUpdateReceiver()
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        if (managerActivity.firstLogin) {
            timelineViewModel.showEnableCUPage(true)
        }
        setUpFlow()
    }

    private fun setUpFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                photosViewModel.state.collect { (_, selectedTab, isMenuShowing) ->
                    when (selectedTab) {
                        PhotosTab.Timeline -> {
                            if (timelineViewModel.state.value.enableCameraUploadPageShowing) {
                                managerActivity.refreshPhotosFragment()
                            }
                            when (timelineViewModel.state.value.selectedTimeBarTab) {
                                TimeBarTab.All -> {
                                    photosViewModel.setMenuShowing(true)
                                }
                                else -> {
                                    photosViewModel.setMenuShowing(false)
                                }
                            }
                        }
                        PhotosTab.Albums -> {
                            photosViewModel.setMenuShowing(false)
                            if (timelineViewModel.state.value.enableCameraUploadPageShowing) {
                                managerActivity.refreshPhotosFragment()
                            }

                        }
                    }

                    if (
                        timelineViewModel.state.value.enableCameraUploadPageShowing
                        && timelineViewModel.state.value.currentShowingPhotos.isNotEmpty()
                        || managerActivity.firstLogin
                    ) {
                        photosViewModel.setMenuShowing(false)
                    }

                    handleMenuIcons(isShowing = isMenuShowing)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                timelineViewModel.state.collect { state ->
                    state.selectedPhoto?.let {
                        openPhoto(it).also {
                            timelineViewModel.onNavigateToSelectedPhoto()
                        }
                    }
                    handleOptionsMenu(state)
                    handleActionMode(state)
                }
            }
        }
    }

    private fun handleActionMode(state: TimelineViewState) {
        if (state.selectedPhotoCount > 0) {
            if (actionMode == null) {
                enterActionMode()
            }
            actionMode?.title = state.selectedPhotoCount.toString()
            managerActivity.showHideBottomNavigationView(true)
        } else {
            actionMode?.finish()
            actionMode = null
            managerActivity.showHideBottomNavigationView(false)
        }
    }

    private fun handleOptionsMenu(state: TimelineViewState) {
        when (state.selectedTimeBarTab) {
            TimeBarTab.All -> {
                photosViewModel.setMenuShowing(true)
            }
            else -> {
                photosViewModel.setMenuShowing(false)
            }
        }
        menu?.findItem(R.id.action_zoom_in)?.let {
            it.isEnabled = state.enableZoomIn
            it.icon.alpha = if (state.enableZoomIn) 255 else 125
        }
        menu?.findItem(R.id.action_zoom_out)?.let {
            it.isEnabled = state.enableZoomOut
            it.icon.alpha = if (state.enableZoomOut) 255 else 125
        }
        menu?.findItem(R.id.action_photos_sortby)?.let {
            it.isEnabled = state.enableSortOption
            val color = if (Util.isDarkMode(requireContext())) {
                Color.argb(38, 255, 255, 255)
            } else {
                Color.argb(38, 0, 0, 0)
            }
            it.isEnabled = state.enableSortOption
            val title = it.title.toString()
            val s = SpannableString(title)
            if (!state.enableSortOption) {
                s.setSpan(
                    ForegroundColorSpan(color),
                    0,
                    s.length,
                    0
                )
                it.title = s
            } else {
                it.title = s
            }
        }
    }

    @Composable
    private fun PhotosBody() {
        val photosViewState by photosViewModel.state.collectAsState()
        val timelineViewState by timelineViewModel.state.collectAsState()
        val albumsViewState by albumsViewModel.state.collectAsState()

        pagerState = rememberPagerState()
        lazyGridState =
            rememberSaveable(
                timelineViewState.scrollStartIndex,
                timelineViewState.scrollStartOffset,
                saver = LazyGridState.Saver
            ) {
                LazyGridState(
                    timelineViewState.scrollStartIndex,
                    timelineViewState.scrollStartOffset
                )
            }

        LaunchedEffect(pagerState) {
            if (managerActivity.fromAlbumContent) {
                managerActivity.fromAlbumContent = false
                photosViewModel.onTabSelected(PhotosTab.Albums)
                pagerState.scrollToPage(PhotosTab.Albums.ordinal)
            }
            snapshotFlow { pagerState.currentPage }.collect { page ->
                photosViewModel.onTabSelected(selectedTab = photosViewState.tabs[page])
                pagerState.scrollToPage(PhotosTab.values()[page].ordinal)
            }
        }

        PhotosBodyView(
            tabs = photosViewState.tabs,
            selectedTab = photosViewState.selectedTab,
            pagerState = pagerState,
            onTabSelected = this::onTabSelected,
            timelineView = { timelineView(timelineViewState = timelineViewState) },
            albumsView = { albumsView(albumsViewState = albumsViewState) },
            timelineViewState = timelineViewState,
        )
    }


    @Composable
    private fun timelineView(timelineViewState: TimelineViewState) = TimelineView(
        timelineViewState = timelineViewState,
        downloadPhotoCover = photosViewModel::downloadPhotoCover,
        lazyGridState = lazyGridState,
        onTextButtonClick = this::enableCameraUploadClick,
        onFABClick = this::openFilterFragment,
        onCardClick = timelineViewModel::onCardClick,
        onTimeBarTabSelected = timelineViewModel::onTimeBarTabSelected,
        enableCUView = { enableCUView(timelineViewState = timelineViewState) },
        photosGridView = { photosGridView(timelineViewState = timelineViewState) },
        emptyView = {
            EmptyState(
                timelineViewState = timelineViewState,
                onFABClick = this::openFilterFragment,
                setEnableCUPage = timelineViewModel::showEnableCUPage,
            )
        }
    )


    @Composable
    private fun albumsView(albumsViewState: AlbumsViewState) = AlbumsView(
        albumsViewState = albumsViewState,
        openAlbum = this::openAlbum,
    )

    @Composable
    private fun enableCUView(timelineViewState: TimelineViewState) = EnableCU(
        timelineViewState = timelineViewState,
        onUploadVideosChanged = timelineViewModel::setCUUploadVideos,
        onUseCellularConnectionChanged = timelineViewModel::setCUUseCellularConnection,
        enableCUClick = this::enableCameraUploadButtonClick,
    )

    @Composable
    private fun photosGridView(timelineViewState: TimelineViewState) = PhotosGridView(
        timelineViewState = timelineViewState,
        downloadPhotoCover = photosViewModel::downloadPhotoCover,
        lazyGridState = lazyGridState,
        onClick = timelineViewModel::onClick,
        onLongPress = timelineViewModel::onLongPress,
    )

    private fun onTabSelected(tab: PhotosTab) {
        if (photosViewModel.state.value.selectedTab != tab) {
            photosViewModel.onTabSelected(selectedTab = tab)
            lifecycleScope.launch {
                pagerState.scrollToPage(tab.ordinal)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (!managerActivity.isInPhotosPage) {
            return
        }
        inflater.inflate(R.menu.fragment_photos_toolbar, menu)
        super.onCreateOptionsMenu(menu, inflater)
        this.menu = menu
        handleMenuIcons(isShowing = photosViewModel.state.value.isMenuShowing)
    }

    private fun handleMenuIcons(isShowing: Boolean) {
        this.menu?.findItem(R.id.action_zoom_in)?.isVisible = isShowing
        this.menu?.findItem(R.id.action_zoom_out)?.isVisible = isShowing
        this.menu?.findItem(R.id.action_photos_filter)?.isVisible = isShowing
        this.menu?.findItem(R.id.action_photos_sortby)?.isVisible = isShowing
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val zoomInValid = timelineViewModel.state.value.enableZoomIn
        val zoomOutValid = timelineViewModel.state.value.enableZoomOut
        val enableSortOption = timelineViewModel.state.value.enableSortOption
        menu.findItem(R.id.action_zoom_in)?.let {
            it.isEnabled = zoomInValid
            it.icon.alpha = if (zoomInValid) 255 else 125
        }
        menu.findItem(R.id.action_zoom_out)?.let {
            it.isEnabled = zoomOutValid
            it.icon.alpha = if (zoomOutValid) 255 else 125
        }
        menu.findItem(R.id.action_photos_sortby)?.let {
            val color = if (Util.isDarkMode(requireContext())) {
                Color.argb(38, 255, 255, 255)
            } else {
                Color.argb(38, 0, 0, 0)
            }
            it.isEnabled = enableSortOption
            val title = it.title.toString()
            val s = SpannableString(title)
            if (!enableSortOption) {
                s.setSpan(
                    ForegroundColorSpan(color),
                    0,
                    s.length,
                    0
                )
                it.title = s
            } else {
                it.title = s
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_zoom_in -> { // +
                timelineViewModel.zoomIn()
                true
            }
            R.id.action_zoom_out -> { // -
                timelineViewModel.zoomOut()
                true
            }
            R.id.action_photos_filter -> {
                openFilterFragment()
                true
            }
            R.id.action_photos_sortby -> {
                timelineViewModel.showingSortByDialog(true)
                showSortByDialog(managerActivity)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openFilterFragment() {
        timelineViewModel.updateFilterState(
            showFilterDialog = true,
            scrollStartIndex = lazyGridState.firstVisibleItemIndex,
            scrollStartOffset = lazyGridState.firstVisibleItemScrollOffset
        )
        managerActivity.skipToFilterFragment(PhotosFilterFragment())
    }

    private fun enterActionMode() {
        actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(
            actionModeCallback
        )
    }

    private fun openPhoto(photo: Photo) {
        val nodeId = photo.id
        val workaroundArray = LongArray(1)
        workaroundArray[0] = nodeId
        val intent = ImageViewerActivity.getIntentForChildren(
            requireContext(),
            workaroundArray,
            nodeId
        )

        startActivity(intent)
        managerActivity.overridePendingTransition(0, 0)
    }

    fun enableCameraUpload() {
        timelineViewModel.enableCU(requireContext())
        managerActivity.setToolbarTitle()
    }

    fun enableCameraUploadButtonClick() {
        if (managerActivity.firstLogin) {
            timelineViewModel.setInitialPreferences()
            managerActivity.firstLogin = false
        }

        MegaApplication.getInstance().sendSignalPresenceActivity()

        val permissions =
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (PermissionUtils.hasPermissions(context, *permissions)) {
            managerActivity.checkIfShouldShowBusinessCUAlert()
        } else {
            PermissionUtils.requestPermission(
                managerActivity,
                Constants.REQUEST_CAMERA_ON_OFF_FIRST_TIME,
                *permissions
            )
        }
    }

    fun onStoragePermissionRefused() {
        Util.showSnackbar(context, getString(R.string.on_refuse_storage_permission))
        skipCUSetup()
    }

    private fun skipCUSetup() {
        timelineViewModel.skipCUSetup()
        managerActivity.isFirstNavigationLevel = false
        if (managerActivity.firstLogin) {
            managerActivity.skipInitialCUSetup()
        } else {
            timelineViewModel.showEnableCUPage(false)
            managerActivity.refreshPhotosFragment()
        }
    }

    fun enableCameraUploadClick() {
        MegaApplication.getInstance().sendSignalPresenceActivity()
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (PermissionUtils.hasPermissions(context, *permissions)) {
            timelineViewModel.showEnableCUPage(true)
            managerActivity.refreshPhotosFragment()
        } else {
            PermissionUtils.requestPermission(
                managerActivity,
                Constants.REQUEST_CAMERA_ON_OFF,
                *permissions
            )
        }
    }

    fun isEnablePhotosViewShown(): Boolean =
        timelineViewModel.state.value.enableCameraUploadPageShowing

    fun shouldUpdateTitle(): Boolean =
        isEnablePhotosViewShown() &&
                photosViewModel.state.value.selectedTab !== PhotosTab.Albums &&
                (timelineViewModel.state.value.currentShowingPhotos.isNotEmpty() ||
                        managerActivity.firstLogin)


    fun setDefaultView() {}

    fun checkScroll() {}

    fun onBackPressed(): Int {
        if (
            timelineViewModel.state.value.enableCameraUploadPageShowing
            && timelineViewModel.state.value.currentShowingPhotos.isNotEmpty()
            || managerActivity.firstLogin
        ) {
            skipCUSetup()
            return 1
        }

        return 0
    }

    fun switchToAlbum() {
        lifecycleScope.launch { pagerState.scrollToPage(PhotosTab.Albums.ordinal) }
    }

    fun refreshViewLayout() {
        handleMenuIcons(!timelineViewModel.state.value.enableCameraUploadPageShowing)
    }

    fun loadPhotos() {}

    fun openAlbum(album: Album) {
        when (album) {
            is Album.FavouriteAlbum -> {
                managerActivity.skipToAlbumContentFragment(AlbumContentFragment.getInstance())
            }
        }
    }

    /**
     * Register Camera Upload Broadcast
     */
    private fun registerCUUpdateReceiver() {
        val filter = IntentFilter(BroadcastConstants.ACTION_UPDATE_CU)
        requireContext().registerReceiver(cuUpdateReceiver, filter)
    }

    /**
     * Camera Upload Broadcast to recieve upload progress and pending file
     */
    private val cuUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val progress = intent.getIntExtra(BroadcastConstants.PROGRESS, 0)
            val pending = intent.getIntExtra(BroadcastConstants.PENDING_TRANSFERS, 0)

            updateProgressBarAndTextUI(progress, pending)
        }
    }

    private fun updateProgressBarAndTextUI(progress: Int, pending: Int) {
        val visible = pending > 0
        if (timelineViewModel.state.value.selectedPhotoCount > 0 || !timelineViewModel.isInAllView()) {
            timelineViewModel.setShowProgressBar(false)
        } else {
            // Check to avoid keeping setting same visibility
            timelineViewModel.updateProgress(
                pending, visible, progress.toFloat() / 100
            )

            Timber.d("CU Upload Progress: Pending: {$pending}, Progress: {$progress}")
        }
    }
}