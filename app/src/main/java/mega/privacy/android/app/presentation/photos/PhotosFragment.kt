package mega.privacy.android.app.presentation.photos

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import collectAsStateWithLifecycle
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.extensions.navigateToAppSettings
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.account.CameraUploadsBusinessAlertDialog
import mega.privacy.android.app.presentation.extensions.getQuantityStringOrDefault
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.photos.albums.AlbumDynamicContentFragment
import mega.privacy.android.app.presentation.photos.albums.AlbumsViewModel
import mega.privacy.android.app.presentation.photos.albums.actionMode.AlbumsActionModeCallback
import mega.privacy.android.app.presentation.photos.albums.model.AlbumsViewState
import mega.privacy.android.app.presentation.photos.albums.model.UIAlbum
import mega.privacy.android.app.presentation.photos.albums.photosselection.AlbumFlow
import mega.privacy.android.app.presentation.photos.albums.photosselection.AlbumPhotosSelectionActivity
import mega.privacy.android.app.presentation.photos.albums.view.AlbumsView
import mega.privacy.android.app.presentation.photos.model.FilterMediaType
import mega.privacy.android.app.presentation.photos.model.PhotosTab
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.app.presentation.photos.model.TimeBarTab
import mega.privacy.android.app.presentation.photos.timeline.actionMode.TimelineActionModeCallback
import mega.privacy.android.app.presentation.photos.timeline.model.TimelineViewState
import mega.privacy.android.app.presentation.photos.timeline.photosfilter.PhotosFilterFragment
import mega.privacy.android.app.presentation.photos.timeline.view.EmptyState
import mega.privacy.android.app.presentation.photos.timeline.view.EnableCU
import mega.privacy.android.app.presentation.photos.timeline.view.PhotosGridView
import mega.privacy.android.app.presentation.photos.timeline.view.TimelineView
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.TimelineViewModel
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.getCurrentSort
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.setCUUploadVideos
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.setCUUseCellularConnection
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.setCurrentSort
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.setShowProgressBar
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.shouldEnableCUPage
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.showingFilterPage
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.showingSortByDialog
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.updateFilterState
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.updateProgress
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.zoomIn
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.zoomOut
import mega.privacy.android.app.presentation.photos.view.PhotosBodyView
import mega.privacy.android.app.presentation.photos.view.showSortByDialog
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.JobUtil.stopCameraUploadSyncHeartbeatWorkers
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.app.utils.permission.PermissionUtils.getImagePermissionByVersion
import mega.privacy.android.app.utils.permission.PermissionUtils.getNotificationsPermission
import mega.privacy.android.app.utils.permission.PermissionUtils.getVideoPermissionByVersion
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetFeatureFlagValue
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
    internal val albumsViewModel: AlbumsViewModel by activityViewModels()

    internal lateinit var managerActivity: ManagerActivity
    private var menu: Menu? = null

    // Action mode
    private var actionMode: ActionMode? = null
    private lateinit var actionModeCallback: TimelineActionModeCallback
    private lateinit var albumsActionModeCallback: AlbumsActionModeCallback

    private val cameraUploadsPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            with(timelineViewModel) {
                if (isEnableCameraUploadsViewShown() && doesAccountHavePhotos()) {
                    shouldEnableCUPage(false)
                    managerActivity.refreshPhotosFragment()
                }
                handlePermissionsResult(permissions)
            }
        }

    @Inject
    lateinit var getThemeMode: GetThemeMode
    lateinit var pagerState: PagerState
    lateinit var timelineLazyGridState: LazyGridState
    lateinit var albumsLazyGridState: LazyGridState

    @Inject
    lateinit var getFeatureFlag: GetFeatureFlagValue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        managerActivity = activity as ManagerActivity
        actionModeCallback = TimelineActionModeCallback(this)
        albumsActionModeCallback = AlbumsActionModeCallback(this)
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
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
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
            timelineViewModel.shouldEnableCUPage(true)
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
                launch {
                    timelineViewModel.state.collect { state ->
                        state.selectedPhoto?.let {
                            openPhoto(it).also {
                                timelineViewModel.onNavigateToSelectedPhoto()
                            }
                        }
                        handleOptionsMenu(state)
                        handleActionMode(state)
                        handleActionsForCameraUploads(state)
                    }
                }

                launch {
                    albumsViewModel.state.collect { state ->
                        handleAlbumsActionMode(state)
                    }
                }
            }
        }
    }

    /**
     * Checks whether a rationale is needed for Media Permissions
     *
     * @return Boolean value
     */
    private fun shouldShowMediaPermissionsRationale() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            shouldShowRequestPermissionRationale(READ_MEDIA_IMAGES) && shouldShowRequestPermissionRationale(
                READ_MEDIA_VIDEO
            )
        } else shouldShowRequestPermissionRationale(READ_EXTERNAL_STORAGE)

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

    /**
     * Decide actions for each Camera Uploads state
     * @param state [TimelineViewState]
     */
    private fun handleActionsForCameraUploads(state: TimelineViewState) {
        if (state.shouldTriggerCameraUploads) {
            enableCameraUploads()
            timelineViewModel.setTriggerCameraUploadsState(shouldTrigger = false)
        }
        if (state.shouldShowBusinessAccountSuspendedPrompt) {
            requireContext().sendBroadcast(Intent(Constants.BROADCAST_ACTION_INTENT_BUSINESS_EXPIRED))
            timelineViewModel.setBusinessAccountSuspendedPromptState(shouldShow = false)
        }
        if (state.shouldTriggerMediaPermissionsDeniedLogic) {
            stopCameraUploadSyncHeartbeatWorkers(requireContext())
            if (!shouldShowMediaPermissionsRationale()) {
                requireContext().navigateToAppSettings()
            }
            timelineViewModel.setTriggerMediaPermissionsDeniedLogicState(shouldTrigger = false)
        }
    }

    private fun handleAlbumsActionMode(state: AlbumsViewState) {
        if (state.selectedAlbumIds.isNotEmpty()) {
            if (actionMode == null) {
                enterAlbumsActionMode()
            }
            actionMode?.title = state.selectedAlbumIds.size.toString()
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
            it.icon?.alpha = if (state.enableZoomIn) 255 else 125
        }
        menu?.findItem(R.id.action_zoom_out)?.let {
            it.isEnabled = state.enableZoomOut
            it.icon?.alpha = if (state.enableZoomOut) 255 else 125
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
        val photosViewState by photosViewModel.state.collectAsStateWithLifecycle()
        val timelineViewState by timelineViewModel.state.collectAsStateWithLifecycle()
        val albumsViewState by albumsViewModel.state.collectAsStateWithLifecycle()

        if (!this::pagerState.isInitialized) {
            pagerState =
                if (managerActivity.fromAlbumContent)
                    rememberPagerState(initialPage = PhotosTab.Albums.ordinal)
                else
                    rememberPagerState(initialPage = photosViewState.selectedTab.ordinal)
        }
        timelineLazyGridState =
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

        albumsLazyGridState = rememberLazyGridState()

        if (managerActivity.fromAlbumContent) {
            managerActivity.fromAlbumContent = false
            photosViewModel.onTabSelected(PhotosTab.Albums)
        }

        LaunchedEffect(pagerState.currentPage) {
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
            timelineLazyGridState = timelineLazyGridState,
            albumsLazyGridState = albumsLazyGridState,
            timelineView = { timelineView(timelineViewState = timelineViewState) },
            albumsView = { albumsView(albumsViewState = albumsViewState, timelineViewState) },
            timelineViewState = timelineViewState,
            albumsViewState = albumsViewState,
        )

        CameraUploadsBusinessAlertDialog(
            show = timelineViewState.shouldShowBusinessAccountPrompt,
            onConfirm = {
                enableCameraUploads()
                timelineViewModel.setBusinessAccountPromptState(shouldShow = false)
            },
            onDeny = { timelineViewModel.setBusinessAccountPromptState(shouldShow = false) },
        )
    }

    @Composable
    private fun timelineView(timelineViewState: TimelineViewState) = TimelineView(
        timelineViewState = timelineViewState,
        photoDownload = photosViewModel::downloadPhoto,
        lazyGridState = timelineLazyGridState,
        onTextButtonClick = this::onCameraUploadsButtonClicked,
        onFABClick = this::openFilterFragment,
        onCardClick = timelineViewModel::onCardClick,
        onTimeBarTabSelected = timelineViewModel::onTimeBarTabSelected,
        enableCUView = { enableCUView(timelineViewState = timelineViewState) },
        photosGridView = { photosGridView(timelineViewState = timelineViewState) },
        emptyView = {
            EmptyState(
                timelineViewState = timelineViewState,
                onFABClick = this::openFilterFragment,
                setEnableCUPage = timelineViewModel::shouldEnableCUPage,
            )
        }
    )


    @Composable
    private fun albumsView(
        albumsViewState: AlbumsViewState,
        timelineViewState: TimelineViewState,
    ) =
        AlbumsView(
            albumsViewState = albumsViewState,
            openAlbum = this::openAlbum,
            downloadPhoto = photosViewModel::downloadPhoto,
            onDialogPositiveButtonClicked = albumsViewModel::createNewAlbum,
            setDialogInputPlaceholder = albumsViewModel::setPlaceholderAlbumTitle,
            setInputValidity = albumsViewModel::setNewAlbumNameValidity,
            openPhotosSelectionActivity = this::openAlbumPhotosSelection,
            setIsAlbumCreatedSuccessfully = albumsViewModel::setIsAlbumCreatedSuccessfully,
            allPhotos = timelineViewState.photos,
            clearAlbumDeletedMessage = { albumsViewModel.updateAlbumDeletedMessage(message = "") },
            onAlbumSelection = { album ->
                if (album.id in albumsViewState.selectedAlbumIds) {
                    albumsViewModel.unselectAlbum(album)
                } else {
                    albumsViewModel.selectAlbum(album)
                }
            },
            closeDeleteAlbumsConfirmation = {
                albumsViewModel.closeDeleteAlbumsConfirmation()
                albumsViewModel.clearAlbumSelection()
            },
            deleteAlbums = ::deleteAlbums,
            lazyGridState = albumsLazyGridState,
        ) {
            getFeatureFlag(AppFeatures.UserAlbums)
        }

    @Composable
    private fun enableCUView(timelineViewState: TimelineViewState) = EnableCU(
        timelineViewState = timelineViewState,
        onUploadVideosChanged = timelineViewModel::setCUUploadVideos,
        onUseCellularConnectionChanged = timelineViewModel::setCUUseCellularConnection,
        enableCUClick = this::onCameraUploadsButtonClicked,
    )

    @Composable
    private fun photosGridView(timelineViewState: TimelineViewState) = PhotosGridView(
        timelineViewState = timelineViewState,
        downloadPhoto = photosViewModel::downloadPhoto,
        lazyGridState = timelineLazyGridState,
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
            it.icon?.alpha = if (zoomInValid) 255 else 125
        }
        menu.findItem(R.id.action_zoom_out)?.let {
            it.isEnabled = zoomOutValid
            it.icon?.alpha = if (zoomOutValid) 255 else 125
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
                showSortByDialog(
                    context = managerActivity,
                    checkedItem = timelineViewModel.getCurrentSort().ordinal,
                    onClickListener = { _, i ->
                        timelineViewModel.setCurrentSort(Sort.values()[i])
                        timelineViewModel.sortByOrder()
                    },
                    onDismissListener = {
                        timelineViewModel.showingSortByDialog(false)
                    }
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openFilterFragment() {
        timelineViewModel.updateFilterState(
            showFilterDialog = true,
            scrollStartIndex = timelineLazyGridState.firstVisibleItemIndex,
            scrollStartOffset = timelineLazyGridState.firstVisibleItemScrollOffset
        )
        managerActivity.skipToFilterFragment(PhotosFilterFragment())
    }

    private val albumPhotosSelectionLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::handleAlbumPhotosSelectionResult,
        )

    private fun openAlbumPhotosSelection(albumId: AlbumId) {
        val intent =
            AlbumPhotosSelectionActivity.create(requireContext(), albumId, AlbumFlow.Creation)
        albumPhotosSelectionLauncher.launch(intent)
        managerActivity.overridePendingTransition(0, 0)

    }

    private fun handleAlbumPhotosSelectionResult(result: ActivityResult) {
        val message =
            result.data?.getStringExtra(AlbumPhotosSelectionActivity.MESSAGE) // Added 5 items to "Color ️‍🌈"
        message?.let {
            if (message.isNotEmpty()) {
                albumsViewModel.setSnackBarMessage(snackBarMessage = message)
                albumsViewModel.getCurrentUIAlbum()?.let { UIAlbum ->
                    openAlbum(album = UIAlbum, resetMessage = false)
                }
            }
        }
    }


    private fun enterActionMode() {
        actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(
            actionModeCallback
        )
    }

    private fun enterAlbumsActionMode() {
        actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(
            albumsActionModeCallback
        )
    }

    private fun openPhoto(photo: Photo) {
        val intent = ImageViewerActivity.getIntentForTimeline(
            requireContext(),
            currentNodeHandle = photo.id,
        )

        startActivity(intent)
        managerActivity.overridePendingTransition(0, 0)
    }

    /**
     * Enables the Camera Uploads feature
     */
    fun enableCameraUploads() {
        timelineViewModel.enableCU(requireContext())
        managerActivity.refreshPhotosFragment()
    }

    /**
     * Performs actions when the Button to enable Camera Uploads has been clicked
     */
    private fun onCameraUploadsButtonClicked() {
        if (managerActivity.firstLogin) {
            timelineViewModel.setInitialPreferences()
            managerActivity.firstLogin = false
        }
        MegaApplication.getInstance().sendSignalPresenceActivity()

        // Check and request the needed permissions
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                getNotificationsPermission(),
                getImagePermissionByVersion(),
                getVideoPermissionByVersion()
            )
        } else {
            arrayOf(
                getImagePermissionByVersion(),
                getVideoPermissionByVersion()
            )
        }

        if (PermissionUtils.hasPermissions(context, *permissions)) {
            with(timelineViewModel) {
                if (isEnableCameraUploadsViewShown()) {
                    shouldEnableCUPage(false)
                    managerActivity.refreshPhotosFragment()
                }
                handleEnableCameraUploads()
            }
        } else cameraUploadsPermissionsLauncher.launch(permissions)
    }

    /**
     * When the user logs in, declines the Onboarding screens and backs out from the
     * Enable Camera Uploads screen (through Gesture Navigation or the Back Button in Toolbar), this
     * function gets called to skip the Initial Camera Uploads setup
     */
    private fun skipInitialCUSetup() {
        with(managerActivity) {
            isFirstNavigationLevel = false
            firstLogin = false
            refreshPhotosFragment()
        }
    }

    /**
     * Checks if the Enable Camera uploads page is shown
     * @return Boolean value
     */
    fun isEnableCameraUploadsViewShown(): Boolean =
        timelineViewModel.state.value.enableCameraUploadPageShowing

    fun onBackPressed(): Int {
        return if (isEnableCameraUploadsViewShown()) {
            skipInitialCUSetup()
            if (doesAccountHavePhotos()) {
                timelineViewModel.shouldEnableCUPage(false)
                1
            } else {
                0
            }
        } else {
            0
        }
    }

    fun switchToAlbum() {
        lifecycleScope.launch { pagerState.scrollToPage(PhotosTab.Albums.ordinal) }
    }

    fun refreshViewLayout() {
        handleMenuIcons(!timelineViewModel.state.value.enableCameraUploadPageShowing)
    }

    fun openAlbum(album: UIAlbum, resetMessage: Boolean = true) {
        resetAlbumContentState(album = album, resetMessage = resetMessage)
        activity?.lifecycleScope?.launch {
            managerActivity.skipToAlbumContentFragment(
                AlbumDynamicContentFragment.getInstance(
                    doesAccountHavePhotos()
                )
            )
        }
    }

    private fun resetAlbumContentState(album: UIAlbum, resetMessage: Boolean = true) {
        albumsViewModel.setCurrentAlbum(album.id)
        albumsViewModel.setCurrentSort(Sort.DEFAULT)
        albumsViewModel.setCurrentMediaType(FilterMediaType.DEFAULT)
        if (resetMessage) albumsViewModel.setSnackBarMessage("")
    }

    /**
     * Checks if the account has Photos or not
     * @return Boolean value
     */
    fun doesAccountHavePhotos(): Boolean = timelineViewModel.state.value.photos.isNotEmpty()

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

    private fun deleteAlbums(albumIds: List<AlbumId>) {
        albumsViewModel.deleteAlbums(albumIds)

        val albums = albumsViewModel.state.value.albums
        val message = context?.getQuantityStringOrDefault(
            R.plurals.photos_album_deleted_message,
            quantity = albumIds.size,
            albumIds.size.takeIf { it > 1 } ?: albums.find {
                it.id is Album.UserAlbum && it.id.id == albumIds.firstOrNull()
            }?.title,
        ).orEmpty()
        albumsViewModel.updateAlbumDeletedMessage(message)
    }

    override fun onPause() {
        albumsViewModel.updateAlbumDeletedMessage(message = "")
        super.onPause()
    }

    override fun onDestroy() {
        requireContext().unregisterReceiver(cuUpdateReceiver)
        super.onDestroy()
    }
}
