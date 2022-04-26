package mega.privacy.android.app.fragments.managerFragments.cu

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.view.*
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentTimelineBinding
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.fragment.BaseZoomFragment
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.ColorUtils.DARK_IMAGE_ALPHA
import mega.privacy.android.app.utils.ColorUtils.setImageViewAlphaIfDark
import mega.privacy.android.app.utils.Constants.PHOTO_SYNC_ADAPTER
import mega.privacy.android.app.utils.ZoomUtil.PHOTO_ZOOM_LEVEL
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import nz.mega.sdk.MegaChatApiJava
import timber.log.Timber

/**
 * TimelineFragment is a sub fragment of PhotosFragment. Its sibling is AlbumsFragment
 *
 * TimelineFragment's logic is pretty much similar to previous PhotosFragment
 */
@AndroidEntryPoint
class TimelineFragment : BaseZoomFragment(), PhotosTabCallback {

    override val viewModel by viewModels<TimelineViewModel>()

    private lateinit var binding: FragmentTimelineBinding

    /**
     * Current order.
     */
    private var order = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        order = viewModel.getOrder()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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
        initAfterViewCreated()
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
            binding.fragmentPhotosFirstLogin.uploadVideosSwitch.isChecked
        )
        mManagerActivity.isFirstLogin = false
        viewModel.setEnableCUShown(false)
        startCameraUploadJob()
    }

    /**
     * User enabled Camera Upload, so a periodic job should be scheduled if not already running
     */
    private fun startCameraUploadJob() {
        callManager {
            it.refreshTimelineFragment()
        }

        Timber.d("CameraUpload enabled through Photos Tab - fireCameraUploadJob()")
        JobUtil.fireCameraUploadJob(context, false)
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
            mManagerActivity.updateCULayout(View.GONE)
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

        if (!mManagerActivity.fromAlbumContent) {
            mManagerActivity.updateCUViewTypes(View.VISIBLE)
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
        binding.emptyHintText.text = HtmlCompat.fromHtml(
            TextUtil.formatEmptyScreenText(
                context,
                StringResourcesUtils.getString(R.string.photos_empty)
            ),
            HtmlCompat.FROM_HTML_MODE_LEGACY
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

            actionModeViewModel.setNodesData(galleryItems.filter { nodeItem -> nodeItem.type != GalleryItem.TYPE_HEADER })

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
                    (parentFragment as PhotosFragment).tabIndex != 0 ||
                    actionMode != null
                ) {
                    View.GONE
                }
                else {
                    View.VISIBLE
                }
            )
        }

        viewModel.camSyncEnabled().observe(viewLifecycleOwner) { isEnabled ->
            updateEnableCUButtons(cuEnabled = isEnabled)
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
        cuEnabled: Boolean
    ) {
        binding.emptyEnableCuButton.visibility =
            if (!cuEnabled && !gridAdapterHasData) View.VISIBLE else View.GONE
        mManagerActivity.updateEnableCUButton(
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
     * Check should show full info and options
     */
    fun shouldShowFullInfoAndOptions() =
        !isEnablePhotosFragmentShown() && selectedView == ALL_VIEW

    /**
     * First make all the buttons unselected,
     * then apply selected style for the selected button regarding to the selected view.
     */
    override fun updateViewSelected() {
        super.updateViewSelected()
        updateFastScrollerVisibility()
        mManagerActivity.updateEnableCUButton(
            if (selectedView == ALL_VIEW && gridAdapterHasData() && !viewModel.isCUEnabled()
            ) View.VISIBLE else View.GONE
        )
        if (selectedView != ALL_VIEW) {
            hideCUProgress()
            binding.uploadProgress.visibility = View.GONE
        }
    }

    public override fun setHideBottomViewScrollBehaviour() {
        if (!isInActionMode()) {
            mManagerActivity.showBottomView()
            mManagerActivity.enableHideBottomViewOnScroll(selectedView != ALL_VIEW)
        }
    }

    fun updateOptionsButtons() {
        if (viewModel.items.value?.isEmpty() == true || mManagerActivity.fromAlbumContent) {
            handleOptionsMenuUpdate(shouldShow = false)
        } else {
            handleOptionsMenuUpdate(shouldShow = shouldShowZoomMenuItem())
        }
    }

    override fun whenStartActionMode() {
        if (!mManagerActivity.isInPhotosPage) return
        super.whenStartActionMode()
        with(parentFragment as PhotosFragment) {
            shouldShowTabLayout(false)
            shouldEnableViewPager(false)
        }

    }

    override fun whenEndActionMode() {
        if (!mManagerActivity.isInPhotosPage) return
        // Because when end action mode, destroy action mode will be trigger. So no need to invoke  animateBottomView()
        // But still need to check viewPanel visibility. If no items, no need to show viewPanel, otherwise, should show.
        super.whenEndActionMode()
        with(parentFragment as PhotosFragment) {
            shouldShowTabLayout(true)
            shouldEnableViewPager(true)
        }

    }

    /**
     * Hides CU progress bar and checks the scroll
     * in order to hide elevation if the list is not scrolled.
     */
    private fun hideCUProgress() {
        mManagerActivity.hideCUProgress()
        checkScroll()
    }

    override fun checkScroll() {
        if (!this::binding.isInitialized || !listViewInitialized()) return

        val isScrolled = listView.canScrollVertically(Constants.SCROLLING_UP_DIRECTION)
        mManagerActivity.changeAppBarElevation(binding.uploadProgress.isVisible || isScrolled)
    }

    /**
     * update progress UI
     */
    fun updateProgress(visibility: Int, pending: Int) {
        if (binding.uploadProgress.visibility != visibility) {
            binding.uploadProgress.visibility = visibility
            checkScroll()
        }
        binding.uploadProgress.text = StringResourcesUtils
            .getQuantityString(R.plurals.cu_upload_progress, pending, pending)
    }

    /**
     * Set default View in All View for TimelineFragment
     */
    fun setDefaultView() {
        newViewClicked(ALL_VIEW)
    }

    override fun handleZoomChange(zoom: Int, needReload: Boolean) {
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

    override fun handleOnCreateOptionsMenu() {
        handleOptionsMenuUpdate(isShowMenu())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (!isInPhotosPage()) {
            return
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (!isInPhotosPage()) {
            true
        } else super.onOptionsItemSelected(item)
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

    override fun getOrder() = viewModel.getOrder()

    override fun onDestroyView() {
        viewModel.cancelSearch()
        super.onDestroyView()
    }
}