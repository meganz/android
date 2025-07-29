package mega.privacy.android.app.presentation.photos

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.extensions.navigateToAppSettings
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.hidenode.HiddenNodesOnboardingActivity
import mega.privacy.android.app.presentation.photos.albums.AlbumScreenWrapperActivity
import mega.privacy.android.app.presentation.photos.albums.AlbumsViewModel
import mega.privacy.android.app.presentation.photos.albums.actionMode.AlbumsActionModeCallback
import mega.privacy.android.app.presentation.photos.albums.add.AddToAlbumActivity
import mega.privacy.android.app.presentation.photos.albums.albumcontent.AlbumContentFragment
import mega.privacy.android.app.presentation.photos.albums.model.AlbumsViewState
import mega.privacy.android.app.presentation.photos.albums.model.UIAlbum
import mega.privacy.android.app.presentation.photos.albums.photosselection.AlbumFlow
import mega.privacy.android.app.presentation.photos.compose.main.PhotosScreen
import mega.privacy.android.app.presentation.photos.model.PhotosTab
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.app.presentation.photos.model.TimeBarTab
import mega.privacy.android.app.presentation.photos.search.PhotosSearchActivity
import mega.privacy.android.app.presentation.photos.timeline.actionMode.TimelineActionModeCallback
import mega.privacy.android.app.presentation.photos.timeline.model.ApplyFilterMediaType
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource
import mega.privacy.android.app.presentation.photos.timeline.model.TimelineViewState
import mega.privacy.android.app.presentation.photos.timeline.photosfilter.PhotosFilterFragment
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.TimelineViewModel
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.getCurrentSort
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.setCurrentSort
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.shouldEnableCUPage
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.showingFilterPage
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.showingSortByDialog
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.updateFilterState
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.zoomIn
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.zoomOut
import mega.privacy.android.app.presentation.photos.view.showSortByDialog
import mega.privacy.android.app.presentation.settings.camerauploads.SettingsCameraUploadsActivity
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.app.utils.permission.PermissionUtils.getImagePermissionByVersion
import mega.privacy.android.app.utils.permission.PermissionUtils.getNotificationsPermission
import mega.privacy.android.app.utils.permission.PermissionUtils.getPartialMediaPermission
import mega.privacy.android.app.utils.permission.PermissionUtils.getVideoPermissionByVersion
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as SharedR
import mega.privacy.mobile.analytics.event.PhotoScreenEvent
import javax.inject.Inject

/** A temporary bridge to support compatibility between view and compose architecture. */
class PhotosViewComposeCoordinator {

    /**
     * The current LazyGridState
     */
    var lazyGridState: LazyGridState? = null
}

/**
 * PhotosFragment
 */
@AndroidEntryPoint
class PhotosFragment : Fragment() {

    private val photosViewModel: PhotosViewModel by activityViewModels()
    private val photoDownloaderViewModel: PhotoDownloaderViewModel by viewModels()
    internal val timelineViewModel: TimelineViewModel by activityViewModels()
    internal val albumsViewModel: AlbumsViewModel by activityViewModels()

    internal lateinit var managerActivity: ManagerActivity
    internal var menu: Menu? = null

    // Action mode
    private var timelineActionMode: ActionMode? = null
    private var albumsActionMode: ActionMode? = null
    private lateinit var timelineActionModeCallback: TimelineActionModeCallback
    private lateinit var albumsActionModeCallback: AlbumsActionModeCallback

    private val cameraUploadsPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            timelineViewModel.handleCameraUploadsPermissionsResult()
        }

    private val photosSearchLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::handlePhotosSearchResult,
        )

    /**
     * Retrieves the App Theme
     */
    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase
    private val viewComposeCoordinator = PhotosViewComposeCoordinator()

    /**
     * Retrieves the value of a specific Feature Flag
     */
    @Inject
    lateinit var getFeatureFlagUseCase: GetFeatureFlagValueUseCase

    private val cameraUploadsPermissions: Array<String> by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                getNotificationsPermission(),
                getImagePermissionByVersion(),
                getVideoPermissionByVersion(),
                getPartialMediaPermission(),
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
    }

    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        managerActivity = activity as ManagerActivity
        timelineActionModeCallback = TimelineActionModeCallback(this)
        albumsActionModeCallback = AlbumsActionModeCallback(this)

        activity?.invalidateMenu()
        initializeCameraUploads()
        timelineViewModel.syncCameraUploadsStatus()
    }

    private fun initializeCameraUploads() {
        if (arguments?.getBoolean(FIRST_LOGIN_KEY) == true) {
            timelineViewModel.setInitialPreferences()
            arguments?.putBoolean(FIRST_LOGIN_KEY, false)
        }
        MegaApplication.getInstance().sendSignalPresenceActivity()
    }

    /**
     * onCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        timelineViewModel.showingFilterPage(false)
        return ComposeView(requireContext()).apply {
            setContent {
                val mode by monitorThemeModeUseCase()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                OriginalTheme(isDark = mode.isDarkMode()) {
                    PhotosScreen(
                        viewComposeCoordinator = viewComposeCoordinator,
                        photosViewModel = photosViewModel,
                        timelineViewModel = timelineViewModel,
                        albumsViewModel = albumsViewModel,
                        photoDownloaderViewModel = photoDownloaderViewModel,
                        onEnableCameraUploads = ::enableCameraUploads,
                        onNavigateAlbumContent = ::openAlbum,
                        onNavigateAlbumPhotosSelection = ::openAlbumPhotosSelection,
                        onZoomIn = ::handleZoomIn,
                        onZoomOut = ::handleZoomOut,
                        onNavigateCameraUploadsSettings = ::openCameraUploadsSettings,
                        onChangeCameraUploadsPermissions = ::changeCameraUploadsPermissions,
                    )
                }
            }
        }
    }

    /**
     * onResume
     */
    override fun onResume() {
        timelineViewModel.resetCUButtonAndProgress()
        albumsViewModel.revalidateInput()
        Analytics.tracker.trackEvent(PhotoScreenEvent)
        Firebase.crashlytics.log("Screen: ${PhotoScreenEvent.eventName}")
        super.onResume()
        checkCameraUploadsPermissions()
    }

    private fun checkCameraUploadsPermissions(showAction: Boolean = false) {
        val hasPermissions = PermissionUtils.hasPermissions(context, *cameraUploadsPermissions)
        timelineViewModel.setCameraUploadsLimitedAccess(isLimitedAccess = !hasPermissions)
        timelineViewModel.setCameraUploadsWarningMenu(isVisible = !hasPermissions)

        if (!hasPermissions && showAction) {
            timelineViewModel.showCameraUploadsChangePermissionsMessage(true)
        }
    }

    private fun changeCameraUploadsPermissions() {
        cameraUploadsPermissionsLauncher.launch(cameraUploadsPermissions)
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        if (arguments?.getBoolean(FIRST_LOGIN_KEY) == true) {
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

                            val isShowMenu =
                                timelineViewModel.state.value.selectedTimeBarTab == TimeBarTab.All
                                        || timelineViewModel.state.value.enableCameraUploadPageShowing
                            photosViewModel.setMenuShowing(isShowMenu)
                        }

                        PhotosTab.Albums -> {
                            photosViewModel.setMenuShowing(false)
                            if (timelineViewModel.state.value.enableCameraUploadPageShowing) {
                                managerActivity.refreshPhotosFragment()
                            }

                        }
                    }

                    if (!timelineViewModel.state.value.loadPhotosDone) {
                        photosViewModel.setMenuShowing(false)
                    }

                    handleMenuIcons(isShowing = isMenuShowing)
                    handleFilterIcons(timelineViewModel.state.value)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    timelineViewModel.state.collect { state ->
                        handleOptionsMenu(state)
                        handleActionMode(state)
                        handleActionsForCameraUploads(state)
                        handleCameraUploadsMenu(state)
                        handleFilterIcons(state)
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
     * Configures the Camera Uploads-related Menu Actions are shown
     *
     * @param timelineViewState The Timeline State
     */
    private fun handleCameraUploadsMenu(timelineViewState: TimelineViewState) {
        this.menu?.apply {
            // Conditions for showing the CU Warning Menu Icon
            val isCuWarningStatusVisible =
                timelineViewState.showCameraUploadsWarning && photosViewModel.state.value.selectedTab != PhotosTab.Albums
            // Conditions for showing the CU Default Menu Icon
            val isCuDefaultStatusVisible =
                timelineViewState.enableCameraUploadButtonShowing && !isCuWarningStatusVisible
            // Conditions for showing the CU Paused Menu Icon
            val isCuPausedStatusVisible =
                timelineViewState.showCameraUploadsPaused && !isCuDefaultStatusVisible
            // Conditions for showing the CU Complete Menu Icon
            val isCuCompleteStatusVisible =
                timelineViewState.showCameraUploadsComplete && !isCuDefaultStatusVisible && !isCuPausedStatusVisible

            findItem(R.id.action_cu_status_warning)?.isVisible = isCuWarningStatusVisible
            findItem(R.id.action_cu_status_default)?.isVisible = isCuDefaultStatusVisible
            findItem(R.id.action_cu_status_paused)?.isVisible = isCuPausedStatusVisible
            findItem(R.id.action_cu_status_complete)?.isVisible = isCuCompleteStatusVisible
        }
    }

    private fun handleFilterIcons(timelineViewState: TimelineViewState) {
        this.menu?.findItem(R.id.action_photos_filter)?.isVisible =
            timelineViewState.applyFilterMediaType != ApplyFilterMediaType.ALL_MEDIA_IN_CD_AND_CU &&
                    photosViewModel.state.value.selectedTab != PhotosTab.Albums
    }

    /**
     * Checks whether a rationale is needed for Media Permissions
     *
     * @return Boolean value
     */
    private fun shouldShowMediaPermissionsRationale() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            shouldShowRequestPermissionRationale(READ_MEDIA_IMAGES) && shouldShowRequestPermissionRationale(
                READ_MEDIA_VIDEO
            ) || shouldShowRequestPermissionRationale(READ_MEDIA_VISUAL_USER_SELECTED)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            shouldShowRequestPermissionRationale(READ_MEDIA_IMAGES) && shouldShowRequestPermissionRationale(
                READ_MEDIA_VIDEO
            )
        } else shouldShowRequestPermissionRationale(READ_EXTERNAL_STORAGE)

    private fun handleActionMode(state: TimelineViewState) {
        if (state.selectedPhotoCount > 0) {
            if (timelineActionMode == null) {
                enterActionMode()
            }
            timelineActionMode?.title = state.selectedPhotoCount.toString()
            managerActivity.hideAdsView()
            managerActivity.showHideBottomNavigationView(true)
        } else {
            timelineActionMode?.finish()
            timelineActionMode = null
            managerActivity.showHideBottomNavigationView(false)
            managerActivity.handleShowingAds()
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
        if (state.shouldTriggerMediaPermissionsDeniedLogic) {
            timelineViewModel.stopCameraUploads()
            if (!shouldShowMediaPermissionsRationale()) {
                requireContext().navigateToAppSettings()
            }
            timelineViewModel.setTriggerMediaPermissionsDeniedLogicState(shouldTrigger = false)
        }
    }

    private fun handleAlbumsActionMode(state: AlbumsViewState) {
        if (state.selectedAlbumIds.isNotEmpty()) {
            if (albumsActionMode == null) {
                enterAlbumsActionMode()
            }
            albumsActionMode?.title = state.selectedAlbumIds.size.toString()
            if (managerActivity.drawerItem == DrawerItem.PHOTOS) {
                managerActivity.hideAdsView()
                managerActivity.showHideBottomNavigationView(true)
            }
        } else {
            albumsActionMode?.finish()
            albumsActionMode = null
            if (managerActivity.drawerItem == DrawerItem.PHOTOS) {
                managerActivity.showHideBottomNavigationView(false)
                managerActivity.handleShowingAds()
            }
        }
    }

    private fun handleOptionsMenu(state: TimelineViewState) {
        val isShowMenu =
            state.selectedTimeBarTab == TimeBarTab.All
                    || timelineViewModel.state.value.enableCameraUploadPageShowing
        photosViewModel.setMenuShowing(isShowMenu)

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

    /**
     * onCreateOptionsMenu
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (!managerActivity.isInPhotosPage) {
            return
        }
        inflater.inflate(R.menu.fragment_photos_toolbar, menu)
        super.onCreateOptionsMenu(menu, inflater)
        this.menu = menu
        handleMenuIcons(isShowing = photosViewModel.state.value.isMenuShowing)
        handleFilterIcons(timelineViewModel.state.value)
    }

    private fun handleMenuIcons(isShowing: Boolean) {
        handleCameraUploadsMenu(timelineViewModel.state.value)

        this.menu?.findItem(R.id.action_photos_filter_secondary)?.isVisible = isShowing
        this.menu?.findItem(R.id.action_photos_sortby)?.isVisible = isShowing
        this.menu?.findItem(R.id.action_zoom_in)?.isVisible = isShowing
        this.menu?.findItem(R.id.action_zoom_out)?.isVisible = isShowing
        this.menu?.findItem(R.id.action_cu_settings)?.isVisible =
            isShowing && !timelineViewModel.state.value.enableCameraUploadButtonShowing
    }

    private fun openCameraUploadsSettings() {
        val context = context ?: return
        startActivity(Intent(context, SettingsCameraUploadsActivity::class.java))
    }

    /**
     * onPrepareOptionsMenu
     */
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val zoomInValid = timelineViewModel.state.value.enableZoomIn
        val zoomOutValid = timelineViewModel.state.value.enableZoomOut
        val enableSortOption = timelineViewModel.state.value.enableSortOption
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

        menu.findItem(R.id.action_zoom_in)?.let {
            it.isEnabled = zoomInValid
            it.title = SpannableString(it.title.toString()).apply {
                if (zoomInValid) return@apply

                val color = if (Util.isDarkMode(requireContext())) {
                    Color.argb(38, 255, 255, 255)
                } else {
                    Color.argb(38, 0, 0, 0)
                }

                setSpan(
                    ForegroundColorSpan(color),
                    0,
                    it.title?.length ?: 0,
                    0,
                )
            }
        }

        menu.findItem(R.id.action_zoom_out)?.let {
            it.isEnabled = zoomOutValid
            it.title = SpannableString(it.title.toString()).apply {
                if (zoomOutValid) return@apply

                val color = if (Util.isDarkMode(requireContext())) {
                    Color.argb(38, 255, 255, 255)
                } else {
                    Color.argb(38, 0, 0, 0)
                }

                setSpan(
                    ForegroundColorSpan(color),
                    0,
                    it.title?.length ?: 0,
                    0,
                )
            }
        }
    }

    /**
     * onOptionsItemSelected
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_photos_search -> {
                searchPhotos()
                true
            }

            R.id.action_zoom_in -> { // +
                handleZoomIn()
                true
            }

            R.id.action_zoom_out -> { // -
                handleZoomOut()
                true
            }

            R.id.action_photos_filter -> {
                openFilterFragment()
                true
            }

            R.id.action_photos_filter_secondary -> {
                openFilterFragment()
                true
            }

            R.id.action_photos_sortby -> {
                timelineViewModel.showingSortByDialog(true)
                showSortByDialog(
                    context = managerActivity,
                    checkedItem = timelineViewModel.getCurrentSort().ordinal,
                    onClickListener = { _, i ->
                        timelineViewModel.setCurrentSort(Sort.entries[i])
                        timelineViewModel.sortByOrder()
                    },
                    onDismissListener = {
                        timelineViewModel.showingSortByDialog(false)
                    }
                )
                true
            }

            R.id.action_cu_status_default -> {
                openCameraUploadsSettings()
                true
            }

            R.id.action_cu_status_paused -> {
                timelineViewModel.setCameraUploadsMessage(
                    getString(SharedR.string.camera_uploads_phone_not_charging_message),
                )
                true
            }

            R.id.action_cu_status_complete -> {
                timelineViewModel.setCameraUploadsMessage(
                    message = getString(R.string.photos_camera_uploads_updated),
                )
                true
            }

            R.id.action_cu_status_warning -> {
                if (timelineViewModel.state.value.isCameraUploadsBannerImprovementEnabled) {
                    timelineViewModel.setCameraUploadsLimitedAccess(true)
                } else {
                    checkCameraUploadsPermissions(showAction = true)
                }
                true
            }

            R.id.action_cu_settings -> {
                openCameraUploadsSettings()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleZoomOut() {
        with(timelineViewModel) {
            zoomOut()
            handleAndUpdatePhotosUIState(
                state.value.photos,
                state.value.currentShowingPhotos
            )
        }
    }

    private fun handleZoomIn() {
        with(timelineViewModel) {
            zoomIn()
            handleAndUpdatePhotosUIState(
                state.value.photos,
                state.value.currentShowingPhotos
            )
        }
    }

    private fun openFilterFragment() {
        timelineViewModel.updateFilterState(
            showFilterDialog = true,
            scrollStartIndex = viewComposeCoordinator.lazyGridState?.firstVisibleItemIndex ?: 0,
            scrollStartOffset = viewComposeCoordinator.lazyGridState?.firstVisibleItemScrollOffset
                ?: 0,
        )
        managerActivity.skipToFilterFragment(PhotosFilterFragment())
    }

    private fun searchPhotos() {
        val intent = Intent(requireActivity(), PhotosSearchActivity::class.java)
        photosSearchLauncher.launch(intent)
        activity?.overridePendingTransition(0, 0)
    }

    private val albumPhotosSelectionLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::handleAlbumPhotosSelectionResult,
        )

    private fun openAlbumPhotosSelection(albumId: AlbumId) {
        val intent = AlbumScreenWrapperActivity.createAlbumPhotosSelectionScreen(
            context = requireContext(),
            albumId = albumId,
            albumFlow = AlbumFlow.Creation,
        )
        albumPhotosSelectionLauncher.launch(intent)
    }

    private fun handleAlbumPhotosSelectionResult(result: ActivityResult) {
        val numPhotos = result.data?.getIntExtra(AlbumScreenWrapperActivity.NUM_PHOTOS, 0) ?: 0
        if (numPhotos == 0) return

        val albumId = result.data?.getLongExtra(AlbumScreenWrapperActivity.ALBUM_ID, -1) ?: -1
        val uiAlbum = albumsViewModel.state.value.let { state ->
            state.currentUIAlbum ?: state.findUIAlbum(AlbumId(albumId))
        } ?: return

        openAlbum(uiAlbum)
    }


    private fun enterActionMode() {
        timelineActionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(
            timelineActionModeCallback
        )
    }

    private fun enterAlbumsActionMode() {
        albumsActionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(
            albumsActionModeCallback
        )
    }

    /**
     * Enables the Camera Uploads feature
     */
    private fun enableCameraUploads() {
        timelineViewModel.enableCU()
        managerActivity.refreshPhotosFragment()
    }

    /**
     * When the user logs in, declines the Onboarding screens and backs out from the
     * Enable Camera Uploads screen (through Gesture Navigation or the Back Button in Toolbar), this
     * function gets called to skip the Initial Camera Uploads setup
     */
    private fun skipInitialCUSetup() {
        with(managerActivity) {
            isFirstNavigationLevel = false
            setFirstLogin(false)
            refreshPhotosFragment()
        }
    }

    /**
     * Checks if the Enable Camera uploads page is shown
     * @return Boolean value
     */
    fun isEnableCameraUploadsViewShown(): Boolean =
        timelineViewModel.state.value.enableCameraUploadPageShowing
                && timelineViewModel.state.value.currentMediaSource != TimelinePhotosSource.CLOUD_DRIVE

    /**
     * onBackPressed
     */
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

    /**
     * Checks if the Photos screen displays the Timeline page
     *
     * @return true if the Timeline is currently shown
     */
    fun isInTimeline(): Boolean = photosViewModel.state.value.selectedTab == PhotosTab.Timeline

    /**
     * Refreshes the current View
     */
    fun refreshViewLayout() {
        handleMenuIcons(!timelineViewModel.state.value.enableCameraUploadPageShowing)
    }

    /**
     * Opens a specific Album
     *
     * @param album The Album to be opened
     */
    private fun openAlbum(album: UIAlbum) {
        viewLifecycleOwner.lifecycleScope.launch {
            Handler(Looper.getMainLooper()).post {
                val fragment = AlbumContentFragment.getInstance(album.id)
                managerActivity.skipToAlbumContentFragment(fragment)
            }
        }
    }

    /**
     * Navigates to the Album Get Link Screen
     */
    fun openAlbumGetLinkScreen() {
        val albumId = albumsViewModel.state.value.selectedAlbumIds.elementAt(0)
        val album = albumsViewModel.getUserAlbum(albumId) ?: return
        val hasSensitiveElement = if (!album.isExported) {
            albumsViewModel.hasSensitiveElement(albumId)
        } else {
            false
        }
        val intent = AlbumScreenWrapperActivity.createAlbumGetLinkScreen(
            context = requireContext(),
            albumId = albumId,
            hasSensitiveElement = hasSensitiveElement,
        )
        startActivity(intent)
        activity?.overridePendingTransition(0, 0)
    }

    /**
     * Navigates to the Album Get Multiple Links Screen
     */
    fun openAlbumGetMultipleLinksScreen() {
        val albumIds = albumsViewModel.state.value.selectedAlbumIds
        val albums = albumIds.mapNotNull(albumsViewModel::getUserAlbum)
        val hasSensitiveElement = albums.any { album ->
            if (!album.isExported) {
                albumsViewModel.hasSensitiveElement(album.id)
            } else {
                false
            }
        }

        val intent = AlbumScreenWrapperActivity.createAlbumGetMultipleLinksScreen(
            context = requireContext(),
            albumIds = albumIds,
            hasSensitiveElement = hasSensitiveElement,
        )
        startActivity(intent)
        activity?.overridePendingTransition(0, 0)
    }

    /**
     * Check if all of the selected albums are exported
     */
    fun isAllSelectedAlbumExported(): Boolean {
        val selectedAlbumIds = albumsViewModel.state.value.selectedAlbumIds
        val allAlbums = albumsViewModel.state.value.albums

        val allExportedSelectedAlbums = allAlbums.filter {
            it.id is Album.UserAlbum && it.id.id in selectedAlbumIds && it.id.isExported
        }

        return allExportedSelectedAlbums.size == selectedAlbumIds.size
    }

    /**
     * Checks if the account has Photos or not
     * @return Boolean value
     */
    fun doesAccountHavePhotos(): Boolean = timelineViewModel.state.value.photos.isNotEmpty()

    /**
     * Handles the procedure of hiding a Node
     */
    fun handleHideNodeClick() {
        val state = timelineViewModel.state.value
        val isPaid = state.accountType?.isPaid ?: false
        val isHiddenNodesOnboarded = state.isHiddenNodesOnboarded
        val isBusinessAccountExpired = state.isBusinessAccountExpired

        if (!isPaid || isBusinessAccountExpired) {
            val intent = HiddenNodesOnboardingActivity.createScreen(
                context = requireContext(),
                isOnboarding = false,
            )
            hiddenNodesOnboardingLauncher.launch(intent)
            activity?.overridePendingTransition(0, 0)
        } else if (isHiddenNodesOnboarded) {
            timelineViewModel.hideOrUnhideNodes(
                hide = true,
                handles = timelineViewModel.selectedPhotosIds.toList(),
            )
            val size = timelineViewModel.selectedPhotosIds.size
            val message =
                resources.getQuantityString(R.plurals.hidden_nodes_result_message, size, size)
            Util.showSnackbar(requireActivity(), message)
        } else {
            showHiddenNodesOnboarding()
        }
    }

    private var tempSelectedNodeHandles: List<Long> = listOf()

    private fun showHiddenNodesOnboarding() {
        tempSelectedNodeHandles = timelineViewModel.selectedPhotosIds.toList()
        timelineViewModel.setHiddenNodesOnboarded()

        val intent = HiddenNodesOnboardingActivity.createScreen(
            context = requireContext(),
            isOnboarding = true,
        )
        hiddenNodesOnboardingLauncher.launch(intent)
        activity?.overridePendingTransition(0, 0)
    }

    private val hiddenNodesOnboardingLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::handleHiddenNodesOnboardingResult,
        )

    private val addToAlbumLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::handleAddToAlbumResult,
        )

    private fun handleHiddenNodesOnboardingResult(result: ActivityResult) {
        if (result.resultCode != Activity.RESULT_OK) return

        timelineViewModel.hideOrUnhideNodes(
            hide = true,
            handles = tempSelectedNodeHandles,
        )
        val selectedSize = tempSelectedNodeHandles.size

        val message =
            resources.getQuantityString(
                R.plurals.hidden_nodes_result_message,
                selectedSize,
                selectedSize,
            )
        Util.showSnackbar(requireActivity(), message)
        tempSelectedNodeHandles = listOf()
    }

    private fun handlePhotosSearchResult(result: ActivityResult) {
        if (result.resultCode != Activity.RESULT_OK) return

        val type = result.data?.getStringExtra("type") ?: return
        val id = result.data?.getLongExtra("id", -1L)

        val uiAlbum = if (type != "custom") {
            albumsViewModel.state.value.findSystemAlbum(type)
        } else if (id != null && id != -1L) {
            albumsViewModel.state.value.findUIAlbum(AlbumId(id))
        } else {
            null
        }
        openAlbum(uiAlbum ?: return)

        photosViewModel.onTabSelected(PhotosTab.Albums)
    }

    fun openAddToAlbum(nodeIds: List<NodeId>, viewType: Int) {
        val intent = Intent(requireContext(), AddToAlbumActivity::class.java).apply {
            val ids = nodeIds.map { it.longValue }.toTypedArray()
            putExtra("ids", ids)
            putExtra("type", viewType)
        }
        addToAlbumLauncher.launch(intent)
    }

    private fun handleAddToAlbumResult(result: ActivityResult) {
        if (result.resultCode != Activity.RESULT_OK) return
        val message = result.data?.getStringExtra("message") ?: return

        Util.showSnackbar(requireActivity(), message)
    }

    /**
     * onPause
     */
    override fun onPause() {
        albumsViewModel.updateAlbumDeletedMessage(message = "")
        super.onPause()
    }

    companion object {

        /**
         * Creates a new instance of [PhotosFragment]
         *
         * @param isFirstLogin true if the User logs in for the first time
         * @return The new instance of [PhotosFragment]
         */
        fun newInstance(isFirstLogin: Boolean): PhotosFragment {
            val fragment = PhotosFragment()
            fragment.arguments = bundleOf(FIRST_LOGIN_KEY to isFirstLogin)

            return fragment
        }

        private const val FIRST_LOGIN_KEY = "PHOTOS_FRAGMENT_FIRST_LOGIN_ARGUMENT"
    }
}
