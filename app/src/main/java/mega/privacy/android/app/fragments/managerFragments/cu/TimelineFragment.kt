package mega.privacy.android.app.fragments.managerFragments.cu

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.components.GestureScaleListener.GestureScaleCallback
import mega.privacy.android.app.components.dragger.DragThumbnailGetter
import mega.privacy.android.app.components.dragger.DragToExitSupport
import mega.privacy.android.app.components.scrollBar.FastScroller
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.databinding.FragmentTimelineBinding
import mega.privacy.android.app.featuretoggle.PhotosFilterAndSortToggle
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.fragments.homepage.ActionModeCallback
import mega.privacy.android.app.fragments.homepage.ActionModeViewModel
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.ItemOperationViewModel
import mega.privacy.android.app.fragments.homepage.getRoundingParams
import mega.privacy.android.app.fragments.homepage.photos.ScaleGestureHandler
import mega.privacy.android.app.fragments.homepage.photos.ZoomViewModel
import mega.privacy.android.app.fragments.managerFragments.cu.TimelineViewModel.Companion.ALL_VIEW
import mega.privacy.android.app.fragments.managerFragments.cu.TimelineViewModel.Companion.DAYS_INDEX
import mega.privacy.android.app.fragments.managerFragments.cu.TimelineViewModel.Companion.DAYS_VIEW
import mega.privacy.android.app.fragments.managerFragments.cu.TimelineViewModel.Companion.FILTER_ALL_PHOTOS
import mega.privacy.android.app.fragments.managerFragments.cu.TimelineViewModel.Companion.FILTER_CAMERA_UPLOADS
import mega.privacy.android.app.fragments.managerFragments.cu.TimelineViewModel.Companion.FILTER_CLOUD_DRIVE
import mega.privacy.android.app.fragments.managerFragments.cu.TimelineViewModel.Companion.FILTER_VIDEOS_ONLY
import mega.privacy.android.app.fragments.managerFragments.cu.TimelineViewModel.Companion.MONTHS_INDEX
import mega.privacy.android.app.fragments.managerFragments.cu.TimelineViewModel.Companion.MONTHS_VIEW
import mega.privacy.android.app.fragments.managerFragments.cu.TimelineViewModel.Companion.SPAN_CARD_LANDSCAPE
import mega.privacy.android.app.fragments.managerFragments.cu.TimelineViewModel.Companion.SPAN_CARD_PORTRAIT
import mega.privacy.android.app.fragments.managerFragments.cu.TimelineViewModel.Companion.VIEW_TYPE
import mega.privacy.android.app.fragments.managerFragments.cu.TimelineViewModel.Companion.YEARS_INDEX
import mega.privacy.android.app.fragments.managerFragments.cu.TimelineViewModel.Companion.YEARS_VIEW
import mega.privacy.android.app.gallery.adapter.GalleryAdapter
import mega.privacy.android.app.gallery.adapter.GalleryCardAdapter
import mega.privacy.android.app.gallery.data.GalleryCard
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.data.GalleryItemSizeConfig
import mega.privacy.android.app.gallery.data.MediaCardType
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.interfaces.showTransfersSnackBar
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.modalbottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.ColorUtils.DARK_IMAGE_ALPHA
import mega.privacy.android.app.utils.ColorUtils.setImageViewAlphaIfDark
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.PHOTO_SYNC_ADAPTER
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.StyleUtils
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.ZoomUtil
import mega.privacy.android.app.utils.ZoomUtil.PHOTO_ZOOM_LEVEL
import mega.privacy.android.app.utils.callManager
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiJava


/**
 * TimelineFragment is a sub fragment of PhotosFragment. Its sibling is AlbumsFragment
 *
 * TimelineFragment's logic is pretty much similar to previous PhotosFragment
 */
@AndroidEntryPoint
class TimelineFragment : BaseFragment(), PhotosTabCallback,
    GestureScaleCallback,
    GalleryCardAdapter.Listener {

    private lateinit var mManagerActivity: ManagerActivity

    // View type panel
    private lateinit var viewTypePanel: View
    private lateinit var yearsButton: TextView
    private lateinit var monthsButton: TextView
    private lateinit var daysButton: TextView
    private lateinit var allButton: TextView

    // List view
    private lateinit var listView: RecyclerView
    private lateinit var gridAdapter: GalleryAdapter
    private lateinit var cardAdapter: GalleryCardAdapter
    private lateinit var layoutManager: GridLayoutManager
    private lateinit var scroller: FastScroller
    private lateinit var scaleGestureHandler: ScaleGestureHandler

    private lateinit var menu: Menu

    // Action mode
    private var actionMode: ActionMode? = null
    private lateinit var actionModeCallback: ActionModeCallback

    // View model
    private val viewModel by viewModels<TimelineViewModel>()
    private val zoomViewModel by viewModels<ZoomViewModel>()
    private val actionModeViewModel by viewModels<ActionModeViewModel>()
    private val itemOperationViewModel by viewModels<ItemOperationViewModel>()

    private var showBottomNav = true

    private var selectedView = ALL_VIEW
    private var adapterType = 0


    private lateinit var binding: FragmentTimelineBinding

    private lateinit var photosFragment: PhotosFragment

    /**
     * Current order.
     */
    private var order = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mManagerActivity = activity as ManagerActivity
        order = viewModel.getOrder()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentTimelineBinding.inflate(inflater, container, false)

        if (mManagerActivity.firstLogin || viewModel.isEnableCUShown()) {
            viewModel.setEnableCUShown(true)
            createCameraUploadsViewForFirstLogin()
        } else {
            showPhotosGrid()
            setupBinding()
        }

        adapterType = PHOTO_SYNC_ADAPTER
        listView = binding.cuList
        scroller = binding.scroller
        viewTypePanel = mManagerActivity.findViewById(R.id.cu_view_type)
        yearsButton = viewTypePanel.findViewById(R.id.years_button)
        monthsButton = viewTypePanel.findViewById(R.id.months_button)
        daysButton = viewTypePanel.findViewById(R.id.days_button)
        allButton = viewTypePanel.findViewById(R.id.all_button)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        selectedView = savedInstanceState?.getInt(VIEW_TYPE) ?: ALL_VIEW
        photosFragment = parentFragment as PhotosFragment
        initAfterViewCreated()
        setInteractListener()
        subscribeBaseObservers()
        setHasOptionsMenu(true)
    }

    /**
     * Set listener for user interaction
     */
    private fun setInteractListener() {
        binding.enableCuButton.setOnClickListener {
            enableCameraUploadClick()
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

    override fun onResume() {
        super.onResume()
        registerCUUpdateReceiver()
        viewModel.checkAndUpdateCamSyncEnabledStatus()
    }

    override fun onPause() {
        requireContext().unregisterReceiver(cuUpdateReceiver)
        super.onPause()
    }


    override fun onBackPressed() = when {
        mManagerActivity.isFirstNavigationLevel -> {
            if (selectedView != ALL_VIEW) {
                mManagerActivity.enableHideBottomViewOnScroll(false)
                mManagerActivity.showBottomView()
            }
            0
        }

        isEnablePhotosFragmentShown() -> {
            skipCUSetup()
            1
        }

        else -> {
            mManagerActivity.invalidateOptionsMenu()
            mManagerActivity.setToolbarTitle()
            1
        }
    }

    /**
     * handle Storage Permission when got refused
     */
    fun onStoragePermissionRefused() {
        Util.showSnackbar(context, getString(R.string.on_refuse_storage_permission))
        skipCUSetup()
    }

    /**
     * Skip CU Set up logic and UI
     */
    private fun skipCUSetup() {
        viewModel.setEnableCUShown(false)
        viewModel.setCamSyncEnabled(false)
        mManagerActivity.isFirstNavigationLevel = false
        if (mManagerActivity.isFirstLogin) {
            mManagerActivity.skipInitialCUSetup()
        } else {
            mManagerActivity.refreshTimelineFragment()
        }
    }

    /**
     * Request CameraUploadPermission
     */
    private fun requestCameraUploadPermission(permissions: Array<String>, requestCode: Int) {
        requestPermission(mManagerActivity, requestCode, *permissions)
    }

    /**
     * Enable Camera Upload
     */
    fun enableCameraUpload() {
        viewModel.enableCu(
            binding.fragmentPhotosFirstLogin.cellularConnectionSwitch.isChecked,
            binding.fragmentPhotosFirstLogin.uploadVideosSwitch.isChecked,
            requireContext(),
        )
        mManagerActivity.isFirstLogin = false
        viewModel.setEnableCUShown(false)
        callManager {
            it.refreshTimelineFragment()
        }
    }

    /**
     * Refresh view and layout after CU enabled or disabled.
     */
    fun refreshViewLayout() {
        if (isEnablePhotosFragmentShown()) {
            showEnablePage()
            createCameraUploadsViewForFirstLogin()
        } else {
            showPhotosGrid()
        }
        initAfterViewCreated()
    }

    /**
     * Show photos view.
     */
    private fun showPhotosGrid() {
        binding.fragmentPhotosFirstLogin.root.visibility = View.GONE
        binding.fragmentPhotosGrid.visibility = View.VISIBLE
    }

    /**
     * Show enable CU page.
     */
    private fun showEnablePage() {
        binding.fragmentPhotosFirstLogin.root.visibility = View.VISIBLE
        binding.fragmentPhotosGrid.visibility = View.GONE
    }

    private fun createCameraUploadsViewForFirstLogin() {
        viewModel.setInitialPreferences()
        binding.fragmentPhotosFirstLogin.camSyncScrollView.setOnScrollChangeListener { _, _, _, _, _ ->
            mManagerActivity
                .changeAppBarElevation(
                    binding.fragmentPhotosFirstLogin.camSyncScrollView.canScrollVertically(
                        Constants.SCROLLING_UP_DIRECTION
                    )
                )
        }
        binding.fragmentPhotosFirstLogin.enableButton.setOnClickListener {
            MegaApplication.getInstance().sendSignalPresenceActivity()
            val permissions =
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (hasPermissions(context, *permissions)) {
                mManagerActivity.checkIfShouldShowBusinessCUAlert()
            } else {
                requestCameraUploadPermission(
                    permissions,
                    Constants.REQUEST_CAMERA_ON_OFF_FIRST_TIME
                )
            }
        }
    }

    /**
     * Init UI and view model when view is created or refreshed.
     */
    private fun initAfterViewCreated() {
        if (viewModel.isEnableCUShown()) {
            mManagerActivity.updateCUViewTypes(View.GONE)
            binding.fragmentPhotosFirstLogin.uploadVideosSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    mManagerActivity.showSnackbar(
                        Constants.DISMISS_ACTION_SNACKBAR,
                        StringResourcesUtils.getString(R.string.video_quality_info),
                        MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                    )
                }
                binding.fragmentPhotosFirstLogin.qualityText.visibility =
                    if (isChecked) View.VISIBLE else View.GONE
            }
            handlePhotosMenuUpdate(false)
            return
        }

        val currentZoom = PHOTO_ZOOM_LEVEL
        zoomViewModel.setCurrentZoom(currentZoom)
        zoomViewModel.setZoom(currentZoom)
        viewModel.mZoom = currentZoom

        setupOtherViews()
        setupListView()
        setupTimePanel()
        setupListAdapter(currentZoom, viewModel.items.value)
        subscribeObservers()
    }

    /**
     * Set up Binding
     */
    private fun setupBinding() {
        binding.apply {
            viewModel = this@TimelineFragment.viewModel
            lifecycleOwner = viewLifecycleOwner
        }
    }

    /**
     * Set up other views
     */
    private fun setupOtherViews() {
        binding.emptyEnableCuButton.setOnClickListener { enableCameraUploadClick() }
        setImageViewAlphaIfDark(context, binding.emptyHintImage, DARK_IMAGE_ALPHA)
        setEmptyState()
    }

    private fun setEmptyState() {
        binding.emptyHintText.text = HtmlCompat.fromHtml(
            TextUtil.formatEmptyScreenText(
                context,
                StringResourcesUtils.getString(when (getCurrentFilter()) {
                    FILTER_CAMERA_UPLOADS -> R.string.photos_empty
                    FILTER_CLOUD_DRIVE -> R.string.homepage_empty_hint_photos
                    FILTER_VIDEOS_ONLY -> R.string.homepage_empty_hint_video
                    else -> R.string.photos_empty
                })
            ),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        binding.emptyHintImage.setImageResource(
            when (getCurrentFilter()) {
                FILTER_CAMERA_UPLOADS -> R.drawable.ic_zero_data_cu
                FILTER_CLOUD_DRIVE -> R.drawable.ic_zero_no_images
                FILTER_VIDEOS_ONLY -> R.drawable.ic_no_videos
                else -> R.drawable.ic_zero_data_cu
            }
        )
    }

    /**
     * handle enable Camera Upload click UI and logic
     */
    fun enableCameraUploadClick() {
        ((context as Activity).application as MegaApplication).sendSignalPresenceActivity()
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (hasPermissions(context, *permissions)) {
            viewModel.setEnableCUShown(true)
            mManagerActivity.refreshTimelineFragment()
        } else {
            requestCameraUploadPermission(permissions, Constants.REQUEST_CAMERA_ON_OFF)
        }
    }

    /**
     * Subscribe Observers
     */
    private fun subscribeObservers() {
        viewModel.items.observe(viewLifecycleOwner) { galleryItems ->
            // On enable CU page, don't update layout and view.
            if (isEnablePhotosFragmentShown() || !mManagerActivity.isInPhotosPage) return@observe

            // Order changed.
            if (order != viewModel.getOrder()) {
                setupListAdapter(
                    currentZoom = getCurrentZoom(),
                    data = galleryItems,
                )
                order = viewModel.getOrder()
            }

            actionModeViewModel.setNodesData(galleryItems.filter { nodeItem -> nodeItem.type != MediaCardType.Header })

            updateOptionsButtons()

            updateEnableCUButtons(
                gridAdapterHasData = galleryItems.isNotEmpty(),
                cuEnabled = viewModel.isCUEnabled()
            )
            binding.emptyHint.visibility = if (galleryItems.isEmpty()) View.VISIBLE else View.GONE
            listView.visibility = if (galleryItems.isEmpty()) View.GONE else View.VISIBLE
            binding.scroller.visibility = if (galleryItems.isEmpty()) View.GONE else View.VISIBLE
            mManagerActivity.updateCUViewTypes(
                if (
                    galleryItems.isEmpty() ||
                    photosFragment.tabIndex != 0 ||
                    actionMode != null
                ) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
            )
        }

        viewModel.camSyncEnabled().observe(viewLifecycleOwner) { isEnabled ->
            if (!viewModel.isEnableCUShown()) {
                updateEnableCUButtons(cuEnabled = isEnabled)
            } else {
                hideCUProgress()
            }
        }
    }

    /**
     * Updates CU enable buttons visibility depending on if CU is enabled/disabled
     * and if the view contains some node.
     *
     * @param cuEnabled True if CU is enabled, false otherwise.
     */
    private fun updateEnableCUButtons(
        gridAdapterHasData: Boolean = gridAdapterHasData(),
        cuEnabled: Boolean,
    ) {
        binding.emptyEnableCuButton.visibility =
            if (!cuEnabled && !gridAdapterHasData) View.VISIBLE else View.GONE
        binding.enableCuButton.visibility = (
                if (selectedView == ALL_VIEW && !cuEnabled
                    && gridAdapterHasData && actionMode == null
                ) View.VISIBLE else View.GONE
                )
        if (!cuEnabled) {
            hideCUProgress()
        }
    }

    /**
     * this method handle is show menu.
     *
     * @return false, when no photo here or not in all view, then will hide the menu.
     * Otherwise, true, show menu.
     */
    private fun isShowMenu() =
        gridAdapterHasData() && selectedView == ALL_VIEW && !viewModel.isEnableCUShown()

    /**
     * Check is enable PhotosFragment showing
     *
     * @return True, show it if in TimelineFragment, otherwise,falsem hide.
     */
    fun isEnablePhotosFragmentShown() = viewModel.isEnableCUShown()

    /**
     * First make all the buttons unselected,
     * then apply selected style for the selected button regarding to the selected view.
     */
    fun updateViewSelected() {
        setViewTypeButtonStyle(allButton, false)
        setViewTypeButtonStyle(daysButton, false)
        setViewTypeButtonStyle(monthsButton, false)
        setViewTypeButtonStyle(yearsButton, false)

        when (selectedView) {
            DAYS_VIEW -> setViewTypeButtonStyle(daysButton, true)
            MONTHS_VIEW -> setViewTypeButtonStyle(monthsButton, true)
            YEARS_VIEW -> setViewTypeButtonStyle(yearsButton, true)
            else -> setViewTypeButtonStyle(allButton, true)
        }

        updateFastScrollerVisibility()
        binding.enableCuButton.visibility = (
                if (selectedView == ALL_VIEW && gridAdapterHasData() && !viewModel.isCUEnabled()
                ) View.VISIBLE else View.GONE
                )
        if (selectedView != ALL_VIEW) {
            hideCUProgress()
        }
    }

    fun setHideBottomViewScrollBehaviour() {
        if (!isInActionMode()) {
            mManagerActivity.showBottomView()
            mManagerActivity.enableHideBottomViewOnScroll(selectedView != ALL_VIEW)
        }
    }

    fun updateOptionsButtons() {
        if (viewModel.items.value?.isEmpty() == true
            || mManagerActivity.fromAlbumContent
            || photosFragment.tabIndex == PhotosPagerAdapter.ALBUM_INDEX
            || viewModel.isEnableCUShown()
        ) {
            handleOptionsMenuUpdate(shouldShow = false)
        } else {
            handleOptionsMenuUpdate(shouldShow = shouldShowZoomMenuItem())
        }
    }

    fun whenStartActionMode() {
        if (!mManagerActivity.isInPhotosPage) return
        mManagerActivity.showHideBottomNavigationView(true)
        animateBottomView(true)
        binding.cuUiLayout.visibility = View.GONE
        with(photosFragment) {
            shouldShowTabLayout(false)
            shouldEnableViewPager(false)
        }

    }

    fun whenEndActionMode() {
        if (!mManagerActivity.isInPhotosPage) return
        // Because when end action mode, destroy action mode will be trigger. So no need to invoke  animateBottomView()
        // But still need to check viewPanel visibility. If no items, no need to show viewPanel, otherwise, should show.
        mManagerActivity.showHideBottomNavigationView(!showBottomNav)
        if (viewModel.items.value != null && viewModel.items.value!!.isNotEmpty()) {
            animateBottomView(false)
        } else {
            animateBottomView(true)
        }
        binding.cuUiLayout.visibility = View.VISIBLE
        with(photosFragment) {
            shouldShowTabLayout(true)
            shouldEnableViewPager(true)
        }
    }

    override fun checkScroll() {
        if (!this::binding.isInitialized || !listViewInitialized()) return

        val isScrolled = listView.canScrollVertically(Constants.SCROLLING_UP_DIRECTION)
        mManagerActivity.changeAppBarElevation(binding.cuProgressText.isVisible || isScrolled)
    }

    /**
     * Set default View in All View for TimelineFragment
     */
    fun setDefaultView() {
        newViewClicked(ALL_VIEW)
    }

    /**
     * When zoom changes,handle zoom
     */
    private fun handleZoomChange(zoom: Int, needReload: Boolean) {
        PHOTO_ZOOM_LEVEL = zoom
        handleZoomAdapterLayoutChange(zoom)
        if (needReload) {
            loadPhotos()
        }
    }

    /**
     * Load Photos
     */
    fun loadPhotos() {
        viewModel.loadPhotos(true)
    }

    /**
     * Handle logic when zoom adapterLayout change
     */
    private fun handleZoomAdapterLayoutChange(zoom: Int) {
        if (!viewModel.isEnableCUShown()) {
            viewModel.mZoom = zoom
            PHOTO_ZOOM_LEVEL = zoom
            if (layoutManagerInitialized()) {
                val state = layoutManager.onSaveInstanceState()
                setupListAdapter(zoom, viewModel.items.value)
                layoutManager.onRestoreInstanceState(state)
            }
        }
    }

    /**
     * Handle menus
     */
    fun handleOnCreateOptionsMenu() {
        handleOptionsMenuUpdate(isShowMenu())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (!isInPhotosPage()) {
            return
        }
        super.onCreateOptionsMenu(menu, inflater)
        if (PhotosFilterAndSortToggle.enabled) {
            inflater.inflate(R.menu.fragment_photos_toolbar, menu)
        } else {
            inflater.inflate(R.menu.fragment_images_toolbar, menu)

        }
        this.menu = menu
        handleOnCreateOptionsMenu()
        handleZoomMenuItemStatus()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (!isInPhotosPage()) {
            true
        } else {
            if (PhotosFilterAndSortToggle.enabled) {
                when (item.itemId) {
                    R.id.action_zoom_in -> {
                        zoomIn()
                    }
                    R.id.action_zoom_out -> {
                        zoomOut()
                    }
                    R.id.action_photos_filter -> {
                        createFilterDialog(mManagerActivity)
                    }
                    R.id.action_photos_sortby -> {
                        createSortByDialog(mManagerActivity)
                    }
                }
            } else {
                when (item.itemId) {
                    R.id.action_zoom_in -> {
                        zoomIn()
                    }
                    R.id.action_zoom_out -> {
                        zoomOut()
                    }
                    R.id.action_menu_sort_by -> {
                        mManagerActivity.showNewSortByPanel(Constants.ORDER_CAMERA)
                    }
                }
            }
            return super.onOptionsItemSelected(item)
        }
    }

    /**
     * Check if in PhotosFragment
     */
    fun isInPhotosPage(): Boolean {
        return activity as ManagerActivity? != null && (activity as ManagerActivity?)!!.isInPhotosPage
    }

    /**
     * Handle Photos Menu Update
     */
    private fun handlePhotosMenuUpdate(isShowMenu: Boolean) {
        if (!isInPhotosPage()) {
            return
        }
        handleOptionsMenuUpdate(isShowMenu)
    }

    fun getOrder() = viewModel.getOrder()

    override fun onDestroyView() {
        viewModel.cancelSearch()
        super.onDestroyView()
    }

    /**
     * Hides the CU progress bar.
     */
    fun hideCUProgress() {
        binding.cuProgressBar.visibility = View.GONE
        binding.cuProgressText.visibility = View.GONE
        checkScroll()
    }

    /**
     * handle progressBar process and text UI which is the number of pending files
     */
    fun updateProgressBarAndTextUI(progress: Int, pending: Int) {
        val visible = pending > 0
        val visibility = if (visible) View.VISIBLE else View.GONE
        if (isInActionMode() || selectedView != ALL_VIEW) {
            binding.cuProgressText.visibility = View.GONE
            binding.cuProgressBar.visibility = View.GONE
        } else {
            // Check to avoid keeping setting same visibility
            if (binding.cuProgressBar.visibility != visibility) {
                binding.cuProgressText.visibility = visibility
                binding.cuProgressBar.visibility = visibility
            }
            binding.cuProgressText.text = StringResourcesUtils
                .getQuantityString(R.plurals.cu_upload_progress, pending, pending)
            binding.cuProgressBar.progress = progress
        }
    }

    fun animateBottomView(hide: Boolean) {
        val deltaY =
            viewTypePanel.height.toFloat() + resources.getDimensionPixelSize(R.dimen.cu_view_type_button_vertical_margin)

        if (hide) {
            viewTypePanel
                .animate()
                .translationYBy(deltaY)
                .setDuration(Constants.ANIMATION_DURATION)
                .withEndAction { viewTypePanel.visibility = View.GONE }
                .start()
        } else {
            viewTypePanel
                .animate()
                .translationYBy(-deltaY)
                .setDuration(Constants.ANIMATION_DURATION)
                .withStartAction { viewTypePanel.visibility = View.VISIBLE }
                .start()
        }
    }

    private fun getNodeCount() = viewModel.getRealPhotoCount()

    private fun updateUiWhenAnimationEnd() {
        viewModel.items.value?.let {
            val newList = ArrayList(it)
            if (isGridAdapterInitialized()) {
                gridAdapter.submitList(newList)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(VIEW_TYPE, selectedView)
    }

    private fun subscribeBaseObservers() {
        zoomViewModel.zoom.observe(viewLifecycleOwner) { zoom: Int ->
            val needReload = ZoomUtil.needReload(getCurrentZoom(), zoom)
            zoomViewModel.setCurrentZoom(zoom)
            handleZoomChange(zoom, needReload)
        }

        viewModel.dateCards.observe(viewLifecycleOwner, ::showCards)

        viewModel.refreshCards.observe(viewLifecycleOwner) {
            if (it && selectedView != ALL_VIEW) {
                showCards(viewModel.dateCards.value)
                viewModel.refreshCompleted()
            }
        }

        setupNavigation()
        setupActionMode()

        DragToExitSupport.observeDragSupportEvents(
            viewLifecycleOwner,
            listView,
            Constants.VIEWER_FROM_PHOTOS
        )
    }

    private fun setupActionMode() {
        actionModeCallback =
            ActionModeCallback(mManagerActivity, actionModeViewModel, megaApi)
        observeItemLongClick()
        observeSelectedItems()
        observeAnimatedItems()
        observeActionBarMessage()
        observeActionModeDestroy()
    }

    private fun setupNavigation() {
        itemOperationViewModel.openItemEvent.observe(viewLifecycleOwner, EventObserver {
            openPhoto(it as GalleryItem)
        })

        itemOperationViewModel.showNodeItemOptionsEvent.observe(viewLifecycleOwner, EventObserver {
            doIfOnline {
                callManager { manager ->
                    manager.showNodeOptionsPanel(
                        it.node,
                        NodeOptionsBottomSheetDialogFragment.SEARCH_MODE
                    )
                }
            }
        })
    }

    /**
     * Set recycle view and its inner layout depends on card view selected and zoom level.
     *
     * @param currentZoom Zoom level.
     */
    fun setupListAdapter(currentZoom: Int, data: List<GalleryItem>?) {
        val isPortrait =
            resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val spanCount = getSpanCount(selectedView, isPortrait)

        val params = listView.layoutParams as ViewGroup.MarginLayoutParams

        layoutManager = GridLayoutManager(context, spanCount)
        listView.layoutManager = layoutManager

        if (selectedView == ALL_VIEW) {
            val imageMargin = ZoomUtil.getMargin(context, currentZoom)
            ZoomUtil.setMargin(context, params, currentZoom)
            val gridWidth =
                ZoomUtil.getItemWidth(context, outMetrics, currentZoom, spanCount, isPortrait)
            val icSelectedWidth = ZoomUtil.getSelectedFrameWidth(context, currentZoom)
            val icSelectedMargin = ZoomUtil.getSelectedFrameMargin(context, currentZoom)
            val itemSizeConfig = GalleryItemSizeConfig(
                currentZoom, gridWidth,
                icSelectedWidth, imageMargin,
                resources.getDimensionPixelSize(R.dimen.cu_fragment_selected_padding),
                icSelectedMargin,
                resources.getDimensionPixelSize(
                    R.dimen.cu_fragment_selected_round_corner_radius
                )
            )

            gridAdapter =
                GalleryAdapter(actionModeViewModel, itemOperationViewModel, itemSizeConfig)

            layoutManager.apply {
                spanSizeLookup = gridAdapter.getSpanSizeLookup(spanCount)
                val itemDimen =
                    ZoomUtil.getItemWidth(context, outMetrics, currentZoom, spanCount, isPortrait)
                gridAdapter.setItemDimen(itemDimen)
            }

            gridAdapter.submitList(data)
            listView.adapter = gridAdapter
        } else {
            val cardMargin =
                resources.getDimensionPixelSize(if (isPortrait) R.dimen.card_margin_portrait else R.dimen.card_margin_landscape)

            val cardWidth: Int =
                (outMetrics.widthPixels - cardMargin * spanCount * 2 - cardMargin * 2) / spanCount

            cardAdapter =
                GalleryCardAdapter(selectedView, cardWidth, cardMargin, this)
            cardAdapter.setHasStableIds(true)
            params.leftMargin = cardMargin
            params.rightMargin = cardMargin
            listView.adapter = cardAdapter

            listView.layoutParams = params
        }

        // Set fast scroller after adapter is set.
        scroller.setRecyclerView(listView)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListView() {
        scaleGestureHandler = ScaleGestureHandler(
            context,
            this
        )
        with(listView) {
            itemAnimator = Util.noChangeRecyclerViewItemAnimator()
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    callManager { manager ->
                        manager.changeAppBarElevation(recyclerView.canScrollVertically(-1))
                    }
                }
            })
            clipToPadding = false
            setHasFixedSize(true)
            setOnTouchListener(scaleGestureHandler)
        }
    }

    override fun onCardClicked(card: GalleryCard) {
        when (selectedView) {
            DAYS_VIEW -> {
                handleZoomMenuItemStatus()
                newViewClicked(ALL_VIEW)
                val photoPosition = gridAdapter.getNodePosition(card.id)
                layoutManager.scrollToPosition(photoPosition)
            }
            MONTHS_VIEW -> {
                newViewClicked(DAYS_VIEW)
                layoutManager.scrollToPosition(viewModel.monthClicked(card))
            }
            YEARS_VIEW -> {
                newViewClicked(MONTHS_VIEW)
                layoutManager.scrollToPosition(viewModel.yearClicked(card))
            }
        }

        showViewTypePanel()
    }

    /**
     * Display the view type buttons panel with animation effect, after a card is clicked.
     */
    private fun showViewTypePanel() {
        val params = viewTypePanel.layoutParams as ViewGroup.MarginLayoutParams
        params.setMargins(
            0, 0, 0,
            resources.getDimensionPixelSize(R.dimen.cu_view_type_button_vertical_margin)
        )
        viewTypePanel.animate().translationY(0f).setDuration(175)
            .withStartAction { viewTypePanel.visibility = View.VISIBLE }
            .withEndAction { viewTypePanel.layoutParams = params }.start()
    }

    private fun openPhoto(nodeItem: GalleryItem) {
        listView.findViewHolderForLayoutPosition(nodeItem.index)
            ?.itemView?.findViewById<ImageView>(R.id.thumbnail)?.also {
                val nodeHandle = nodeItem.node?.handle ?: MegaApiJava.INVALID_HANDLE
                val childrenNodes = viewModel.getItemsHandle()
                val intent = ImageViewerActivity.getIntentForChildren(
                    requireContext(),
                    childrenNodes,
                    nodeHandle
                )

                (listView.adapter as? DragThumbnailGetter)?.let { getter ->
                    DragToExitSupport.putThumbnailLocation(
                        intent,
                        listView,
                        nodeItem.index,
                        Constants.VIEWER_FROM_PHOTOS,
                        getter
                    )
                }

                startActivity(intent)
                mManagerActivity.overridePendingTransition(0, 0)
            }
    }

    fun gridAdapterHasData() = viewModel.items.value?.isNotEmpty() ?: false

    fun layoutManagerInitialized() = this::layoutManager.isInitialized

    fun listViewInitialized() = this::listView.isInitialized

    private fun observeItemLongClick() =
        actionModeViewModel.longClick.observe(viewLifecycleOwner, EventObserver {
            if (zoomViewModel.getCurrentZoom() != ZoomUtil.ZOOM_OUT_2X) {
                doIfOnline { actionModeViewModel.enterActionMode(it) }

            }
        })

    private fun observeSelectedItems() =
        actionModeViewModel.selectedNodes.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                if (actionMode != null) {
                    actionMode?.apply {
                        finish()
                    }
                    whenEndActionMode()
                }
            } else {
                actionModeCallback.nodeCount = getNodeCount()

                if (actionMode == null) {
                    callManager { manager ->
                        manager.hideKeyboardSearch()
                    }

                    actionMode = (activity as AppCompatActivity).startSupportActionMode(
                        actionModeCallback
                    )
                    whenStartActionMode()
                } else {
                    actionMode?.invalidate()  // Update the action items based on the selected nodes
                }

                actionMode?.title = it.size.toString()
            }
        }

    private fun observeAnimatedItems() {
        var animatorSet: AnimatorSet? = null

        actionModeViewModel.animNodeIndices.observe(viewLifecycleOwner) {
            animatorSet?.run {
                // End the started animation if any, or the view may show messy as its property
                // would be wrongly changed by multiple animations running at the same time
                // via contiguous quick clicks on the item
                if (isStarted) {
                    end()
                }
            }

            // Must create a new AnimatorSet, or it would keep all previous
            // animation and play them together
            animatorSet = AnimatorSet()
            val animatorList = mutableListOf<Animator>()

            animatorSet?.addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator) {
                }

                override fun onAnimationEnd(animation: Animator) {
                    updateUiWhenAnimationEnd()
                }

                override fun onAnimationCancel(animation: Animator) {
                }

                override fun onAnimationStart(animation: Animator) {
                }
            })

            it.forEach { pos ->
                listView.findViewHolderForAdapterPosition(pos)?.let { viewHolder ->
                    val itemView = viewHolder.itemView
                    // Draw the green outline for the thumbnail view at once
                    val thumbnailView =
                        itemView.findViewById<SimpleDraweeView>(R.id.thumbnail)
                    thumbnailView.hierarchy.roundingParams = getRoundingParams(context)

                    val imageView = itemView.findViewById<ImageView>(
                        R.id.icon_selected
                    )

                    imageView?.run {
                        setImageResource(R.drawable.ic_select_folder)
                        visibility = View.VISIBLE

                        val animator =
                            AnimatorInflater.loadAnimator(context, R.animator.icon_select)
                        animator.setTarget(this)
                        animatorList.add(animator)
                    }
                }
            }

            animatorSet?.playTogether(animatorList)
            animatorSet?.start()
        }
    }

    private fun observeActionBarMessage() {
        actionModeViewModel.onActionBarMessage().observe(viewLifecycleOwner) {
            callManager { manager -> manager.showTransfersSnackBar(StringResourcesUtils.getString(it)) }
        }
    }

    private fun observeActionModeDestroy() =
        actionModeViewModel.actionModeDestroy.observe(viewLifecycleOwner, EventObserver {
            actionMode = null
            callManager { manager ->
                manager.showKeyboardForSearch()
            }
        })

    fun doIfOnline(operation: () -> Unit) {
        if (Util.isOnline(context)) {
            operation()
        } else {
            val activity = activity as ManagerActivity

            activity.hideKeyboardSearch()  // Make the snack bar visible to the user
            activity.showSnackbar(
                Constants.SNACKBAR_TYPE,
                context.getString(R.string.error_server_connection_problem),
                MegaChatApiJava.MEGACHAT_INVALID_HANDLE
            )
        }
    }

    fun handleZoomMenuItemStatus() {
        val canZoomOut = zoomViewModel.canZoomOut()
        val canZoomIn = zoomViewModel.canZoomIn()
        //handle can zoom in then handle can zoom out
        handleEnableToolbarMenuIcon(R.id.action_zoom_in, canZoomIn)
        handleEnableToolbarMenuIcon(R.id.action_zoom_out, canZoomOut)
    }

    private fun handleEnableToolbarMenuIcon(menuItemId: Int, isEnable: Boolean) {
        if (!this::menu.isInitialized)
            return
        val menuItem = this.menu.findItem(menuItemId)
        var colorRes = ColorUtils.getThemeColor(context, R.attr.colorControlNormal)
        if (!isEnable) {
            colorRes = ContextCompat.getColor(context, R.color.grey_038_white_038)
        }
        DrawableCompat.setTint(
            menuItem.icon ?: return,
            colorRes
        )
        menuItem.isEnabled = isEnable
    }

    fun handleOptionsMenuUpdate(shouldShow: Boolean) {
        if (this::menu.isInitialized) {
            menu.findItem(R.id.action_zoom_in)?.isVisible = shouldShow
            menu.findItem(R.id.action_zoom_out)?.isVisible = shouldShow
            menu.findItem(R.id.action_photos_filter)?.isVisible = shouldShow
            menu.findItem(R.id.action_photos_sortby)?.isVisible = shouldShow
        }
    }

    override fun zoomIn() {
        zoomViewModel.zoomIn()
        handleZoomMenuItemStatus()
    }

    override fun zoomOut() {
        zoomViewModel.zoomOut()
        handleZoomMenuItemStatus()
    }

    private fun getCurrentZoom(): Int {
        return zoomViewModel.getCurrentZoom()
    }

    /**
     * Apply selected/unselected style for the TextView button.
     *
     * @param textView The TextView button to be applied with the style.
     * @param enabled true, apply selected style; false, apply unselected style.
     */
    private fun setViewTypeButtonStyle(textView: TextView?, enabled: Boolean) {
        if (textView == null)
            return
        textView.setBackgroundResource(
            if (enabled)
                R.drawable.background_18dp_rounded_selected_button
            else
                R.drawable.background_18dp_rounded_unselected_button
        )

        StyleUtils.setTextStyle(
            textView = textView,
            textAppearance = if (enabled) R.style.TextAppearance_Mega_Subtitle2_Medium_WhiteGrey87 else R.style.TextAppearance_Mega_Subtitle2_Normal_Grey87White87,
        )
    }

    private fun setupTimePanel() {
        yearsButton.setOnClickListener {
            newViewClicked(YEARS_VIEW)
        }

        monthsButton.setOnClickListener {
            newViewClicked(MONTHS_VIEW)
        }

        daysButton.setOnClickListener {
            newViewClicked(DAYS_VIEW)
        }

        allButton.setOnClickListener {
            newViewClicked(ALL_VIEW)
        }


        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val params = viewTypePanel.layoutParams
            params.width = outMetrics.heightPixels
            viewTypePanel.layoutParams = params
        }

        updateViewSelected()
        setHideBottomViewScrollBehaviour()
    }

    /**
     * Show the selected card view after corresponding button is clicked.
     *
     * @param selectedView The selected view.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun newViewClicked(selectedView: Int) {
        if (this.selectedView == selectedView) return

        this.selectedView = selectedView
        setupListAdapter(getCurrentZoom(), viewModel.items.value)

        when (selectedView) {
            DAYS_VIEW, MONTHS_VIEW, YEARS_VIEW -> {
                showCards(
                    viewModel.dateCards.value
                )

                listView.setOnTouchListener(null)
            }
            else -> {
                listView.setOnTouchListener(scaleGestureHandler)
            }
        }
        handleOptionsMenuUpdate(shouldShowZoomMenuItem())
        updateViewSelected()
        // If selected view is not all view, add layout param behaviour, so that button panel will go off when scroll.
        setHideBottomViewScrollBehaviour()
    }

    /**
     * Show the view with the data of years, months or days depends on selected view.
     *
     * @param dateCards
     *          The first element is the cards of days.
     *          The second element is the cards of months.
     *          The third element is the cards of years.
     */
    private fun showCards(dateCards: List<List<GalleryCard>?>?) {
        val index = when (selectedView) {
            DAYS_VIEW -> DAYS_INDEX
            MONTHS_VIEW -> MONTHS_INDEX
            YEARS_VIEW -> YEARS_INDEX
            else -> -1
        }

        if (index != -1) {
            cardAdapter.submitList(dateCards?.get(index))
        }

        updateFastScrollerVisibility()
    }

    private fun updateFastScrollerVisibility() {
        if (!this::cardAdapter.isInitialized) return

        val gridView = selectedView == ALL_VIEW

        scroller.visibility =
            if (!gridView && cardAdapter.itemCount >= Constants.MIN_ITEMS_SCROLLBAR)
                View.VISIBLE
            else
                View.GONE
    }

    /**
     * Whether should show zoom in/out menu items.
     * Depends on if selected view is all view.
     *
     * @return true, current view is all view should show the menu items, false, otherwise.
     */
    private fun shouldShowZoomMenuItem() = selectedView == ALL_VIEW

    /**
     * Get how many items will be shown per row, depends on screen direction and zoom level if all view is selected.
     *
     * @param isPortrait true, on portrait mode, false otherwise.
     */
    private fun getSpanCount(selectedView: Int, isPortrait: Boolean): Int {
        return if (selectedView != ALL_VIEW) {
            if (isPortrait) SPAN_CARD_PORTRAIT else SPAN_CARD_LANDSCAPE
        } else {
            ZoomUtil.getSpanCount(isPortrait, zoomViewModel.getCurrentZoom())
        }
    }

    /**
     * Check gridAdapter is initialized
     *
     * @return true is initialized, false is not initialized
     */
    fun isGridAdapterInitialized(): Boolean {
        return this::gridAdapter.isInitialized
    }

    /**
     * Check is in action mode.
     *
     * @return true in, false not in
     */
    fun isInActionMode() = actionMode != null

    /**
     * Create the filter dialog
     */
    fun createFilterDialog(
        context: Activity,
    ) {
        val filterDialog: AlertDialog
        val dialogBuilder = MaterialAlertDialogBuilder(context)

        val stringsArray: List<String> = listOf(
            getString(R.string.photos_filter_all_photos),
            getString(R.string.photos_filter_cloud_drive),
            getString(R.string.photos_filter_camera_uploads),
            getString(R.string.photos_filter_videos_only),
        )
        val itemsAdapter =
            ArrayAdapter(context, R.layout.checked_text_view_dialog_button, stringsArray)
        val listView = ListView(context)
        listView.adapter = itemsAdapter

        dialogBuilder.setSingleChoiceItems(
            itemsAdapter,
            viewModel.getCurrentFilter(),
            DialogInterface.OnClickListener { dialog, item ->
                itemsAdapter.getItem(item)?.let {
                    viewModel.setCurrentFilter(item)
                    with(photosFragment) {
                        setActionBarSubtitleText(Util.adjustForLargeFont(
                            it
                        ))
                        showHideABSubtitle(isFilterAllPhotos())
                        setEmptyState()
                    }
                }
                dialog.dismiss()
            })

        dialogBuilder.setNegativeButton(context.getString(R.string.general_cancel)) {
                dialog: DialogInterface,
                _,
            ->
            dialog.dismiss()
        }

        dialogBuilder.setTitle(R.string.photos_action_filter)
        filterDialog = dialogBuilder.create()
        filterDialog.show()
    }

    /**
     * Create the sort by dialog
     */
    fun createSortByDialog(
        context: Activity,
    ) {
        val sortDialog: AlertDialog
        val dialogBuilder = MaterialAlertDialogBuilder(context)

        val stringsArray: List<String> = listOf(
            getString(R.string.sortby_date_newest),
            getString(R.string.sortby_date_oldest),
        )
        val itemsAdapter =
            ArrayAdapter(context, R.layout.checked_text_view_dialog_button, stringsArray)
        val listView = ListView(context)
        listView.adapter = itemsAdapter

        dialogBuilder.setSingleChoiceItems(
            itemsAdapter,
            viewModel.getCurrentSort(),
            DialogInterface.OnClickListener { dialog, item ->
                viewModel.setCurrentSort(item)
                dialog.dismiss()
            })

        dialogBuilder.setNegativeButton(context.getString(R.string.general_cancel)) {
                dialog: DialogInterface,
                _,
            ->
            dialog.dismiss()
        }

        sortDialog = dialogBuilder.create()
        sortDialog.setTitle(R.string.action_sort_by)
        sortDialog.show()
    }

    /**
     * Get the current selected filter
     */
    fun getCurrentFilterAsString(): String {
        return viewModel.getCurrentFilterAsString()
    }

    fun getCurrentFilter(): Int {
        return viewModel.getCurrentFilter()
    }

    fun isFilterAllPhotos(): Boolean {
        return viewModel.getCurrentFilter() == FILTER_ALL_PHOTOS
    }
}
