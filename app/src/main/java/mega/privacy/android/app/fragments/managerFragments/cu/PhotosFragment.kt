package mega.privacy.android.app.fragments.managerFragments.cu

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ListenScrollChangesHelper
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.observeDragSupportEvents
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.putThumbnailLocation
import mega.privacy.android.app.databinding.FragmentPhotosBinding
import mega.privacy.android.app.fragments.homepage.photos.ScaleGestureHandler
import mega.privacy.android.app.gallery.adapter.GalleryCardAdapter
import mega.privacy.android.app.gallery.data.GalleryCard
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.data.GalleryItemSizeConfig
import mega.privacy.android.app.globalmanagement.SortOrderManagement
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.ColorUtils.DARK_IMAGE_ALPHA
import mega.privacy.android.app.utils.ColorUtils.setImageViewAlphaIfDark
import mega.privacy.android.app.utils.ZoomUtil.PHOTO_ZOOM_LEVEL
import mega.privacy.android.app.utils.ZoomUtil.ZOOM_DEFAULT
import mega.privacy.android.app.utils.ZoomUtil.ZOOM_OUT_1X
import mega.privacy.android.app.utils.ZoomUtil.getItemWidth
import mega.privacy.android.app.utils.ZoomUtil.getMargin
import mega.privacy.android.app.utils.ZoomUtil.getSelectedFrameMargin
import mega.privacy.android.app.utils.ZoomUtil.getSelectedFrameWidth
import mega.privacy.android.app.utils.ZoomUtil.setMargin
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaNode
import javax.inject.Inject

@AndroidEntryPoint
class PhotosFragment : BaseZoomFragment(), CUGridViewAdapter.Listener,
    GalleryCardAdapter.Listener {

    @Inject
    lateinit var sortOrderManagement: SortOrderManagement

    private lateinit var mManagerActivity: ManagerActivityLollipop
    private lateinit var binding: FragmentPhotosBinding
    private var gridAdapter: CUGridViewAdapter? = null
    private var cardAdapter: GalleryCardAdapter? = null

    private var mActionMode: ActionMode? = null

    private var viewTypesLayout: LinearLayout? = null
    private var yearsButton: TextView? = null
    private var monthsButton: TextView? = null
    private var daysButton: TextView? = null
    private var allButton: TextView? = null

    private val viewModel by viewModels<CuViewModel>()

    private var layoutManager: GridLayoutManager? = null

    private lateinit var scaleGestureHandler: ScaleGestureHandler

    private var selectedView = ALL_VIEW

    fun getItemCount() = if (gridAdapter == null) 0 else gridAdapter!!.itemCount


    fun reloadNodes() {
        viewModel.loadNodes()
        viewModel.getCards()
    }

    fun checkScroll() {
        if (!this::binding.isInitialized) return

        val isScrolled = binding.cuList.canScrollVertically(Constants.SCROLLING_UP_DIRECTION)
        mManagerActivity.changeAppBarElevation(binding.uploadProgress.isVisible || viewModel.isSelecting() || isScrolled)
    }

    fun selectAll() {
        viewModel.selectAll()
    }

    fun onBackPressed() = when {
        mManagerActivity.isFirstNavigationLevel -> {
            if (selectedView != ALL_VIEW) {
                mManagerActivity.enableHideBottomViewOnScroll(false)
                mManagerActivity.showBottomView()
            }
            0
        }

        isEnableCUFragmentShown() -> {
            skipCUSetup()
            1
        }

        else -> {
            reloadNodes()
            mManagerActivity.invalidateOptionsMenu()
            mManagerActivity.setToolbarTitle()
            1
        }
    }

    fun onStoragePermissionRefused() {
        Util.showSnackbar(context, getString(R.string.on_refuse_storage_permission))
        skipCUSetup()
    }

    private fun skipCUSetup() {
        viewModel.setEnableCUShown(false)
        viewModel.setCamSyncEnabled(false)
        mManagerActivity.isFirstNavigationLevel = false
        if (mManagerActivity.isFirstLogin) {
            mManagerActivity.skipInitialCUSetup()
        } else {
            mManagerActivity.refreshPhotosFragment()
        }
    }

    private fun requestCameraUploadPermission(permissions: Array<String>, requestCode: Int) {
        PermissionUtils.requestPermission(mManagerActivity, requestCode, *permissions)
    }

    fun enableCu() {
        viewModel.enableCu(
            binding.fragmentPhotosFirstLogin.cellularConnectionSwitch.isChecked,
            binding.fragmentPhotosFirstLogin.uploadVideosSwitch.isChecked
        )
        mManagerActivity.isFirstLogin = false
        viewModel.setEnableCUShown(false)
        startCU()
    }

    private fun startCU() {
        mManagerActivity.refreshPhotosFragment()
        // TODO main looper or my looper?
        Handler(Looper.getMainLooper()).postDelayed({
            LogUtil.logDebug("Starting CU")
            JobUtil.startCameraUploadService(context)
        }, 1000)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mManagerActivity = context as ManagerActivityLollipop
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPhotosBinding.inflate(inflater, container, false)
        this.selectedView = viewModel.selectedViewType
        if (mManagerActivity.firstLogin || viewModel.isEnableCUShown()) {
            viewModel.setEnableCUShown(true)
            createCameraUploadsViewForFirstLogin()
        } else {
            showPhotosGrid()
        }

        return binding.root
    }

    /**
     * Refresh view and layout after CU enabled or disabled.
     */
    fun refreshViewLayout() {
        if (isEnableCUFragmentShown()) {
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
        ListenScrollChangesHelper().addViewToListen(
            binding.fragmentPhotosFirstLogin.camSyncScrollView
        ) { _, _, _, _, _ ->
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
            if (PermissionUtils.hasPermissions(context, *permissions)) {
                mManagerActivity.checkIfShouldShowBusinessCUAlert()
            } else {
                requestCameraUploadPermission(
                    permissions,
                    Constants.REQUEST_CAMERA_ON_OFF_FIRST_TIME
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAfterViewCreated()
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
        viewModel.resetOpenedNode()
        mManagerActivity.updateCUViewTypes(View.VISIBLE)
        setupRecyclerView()
        setupViewTypes()
        setupOtherViews()
        observeLiveData()
        val currentZoom = PHOTO_ZOOM_LEVEL
        zoomViewModel.setCurrentZoom(currentZoom)
        zoomViewModel.setZoom(currentZoom)
        viewModel.setZoom(currentZoom)
        viewModel.getCards()
        viewModel.getCUNodes()
    }

    fun setViewTypes(
        cuViewTypes: LinearLayout?,
        cuYearsButton: TextView?,
        cuMonthsButton: TextView?,
        cuDaysButton: TextView?,
        cuAllButton: TextView?
    ) {
        viewTypesLayout = cuViewTypes
        yearsButton = cuYearsButton
        monthsButton = cuMonthsButton
        daysButton = cuDaysButton
        allButton = cuAllButton

        setupViewTypes()
    }

    private fun setupViewTypes() {
        if (allButton != null) {
            allButton!!.setOnClickListener { _ ->
                newViewClicked(
                    ALL_VIEW
                )
            }
        }
        if (daysButton != null) {
            daysButton!!.setOnClickListener { _ ->
                newViewClicked(
                    DAYS_VIEW
                )
            }
        }
        if (monthsButton != null) {
            monthsButton!!.setOnClickListener { _ ->
                newViewClicked(
                    MONTHS_VIEW
                )
            }
        }
        if (yearsButton != null) {
            yearsButton!!.setOnClickListener { _ ->
                newViewClicked(
                    YEARS_VIEW
                )
            }
        }
        if (context != null && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && viewTypesLayout != null) {
            val params = viewTypesLayout!!.layoutParams as LinearLayout.LayoutParams
            params.width = outMetrics.heightPixels
            viewTypesLayout!!.layoutParams = params
        }
        if (view != null) {
            updateViewSelected()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupRecyclerView() {
        binding.cuList.setHasFixedSize(true)
        binding.cuList.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                checkScroll()
            }
        })
        scaleGestureHandler = ScaleGestureHandler(context, this)
        binding.cuList.setOnTouchListener(scaleGestureHandler)
        setGridView()
    }

    private fun setGridView() {
        viewModel.clearSelection()
        val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val spanCount = getSpanCount(selectedView, isPortrait)
        layoutManager = GridLayoutManager(context, spanCount)
        binding.cuList.layoutManager = layoutManager
        binding.cuList.setPadding(
            0,
            0,
            0,
            resources.getDimensionPixelSize(R.dimen.cu_margin_bottom)
        )
        val params = binding.cuList.layoutParams as RelativeLayout.LayoutParams
        if (selectedView == ALL_VIEW) {
            val imageMargin = getMargin(context, getCurrentZoom())
            setMargin(context, params, getCurrentZoom())
            val gridWidth = getItemWidth(context, outMetrics, getCurrentZoom(), spanCount)
            val icSelectedWidth = getSelectedFrameWidth(context, getCurrentZoom())
            val icSelectedMargin = getSelectedFrameMargin(context, getCurrentZoom())
            val itemSizeConfig = GalleryItemSizeConfig(
                getCurrentZoom(), gridWidth,
                icSelectedWidth, imageMargin,
                resources.getDimensionPixelSize(R.dimen.cu_fragment_selected_padding),
                icSelectedMargin,
                resources.getDimensionPixelSize(
                    R.dimen.cu_fragment_selected_round_corner_radius
                )
            )
            if (gridAdapter == null) {
                gridAdapter = CUGridViewAdapter(this, spanCount, itemSizeConfig)
            } else {
                gridAdapter!!.setSpanCount(spanCount)
                gridAdapter!!.setCuItemSizeConfig(itemSizeConfig)
            }
            layoutManager!!.spanSizeLookup = object : SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return gridAdapter!!.getSpanSize(position)
                }
            }
            binding.cuList.adapter = gridAdapter
        } else {
            val cardMargin =
                resources.getDimensionPixelSize(if (isPortrait) R.dimen.card_margin_portrait else R.dimen.card_margin_landscape)
            val cardWidth =
                (outMetrics.widthPixels - cardMargin * spanCount * 2 - cardMargin * 2) / spanCount
            cardAdapter = GalleryCardAdapter(selectedView, cardWidth, cardMargin, this)
            cardAdapter!!.setHasStableIds(true)
            binding.cuList.adapter = cardAdapter
            params.rightMargin = cardMargin
            params.leftMargin = params.rightMargin
        }
        binding.cuList.layoutParams = params
        binding.scroller.setRecyclerView(binding.cuList)
    }

    private fun setupOtherViews() {
        binding.emptyEnableCuButton.setOnClickListener { enableCUClick() }
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
     * Show the selected card view after corresponding button is clicked.
     *
     * @param selectedView The selected view.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun newViewClicked(selectedView: Int) {
        if (this.selectedView == selectedView) {
            return
        }
        this.selectedView = selectedView
        setGridView()
        when (selectedView) {
            DAYS_VIEW -> {
                showDayCards(viewModel.getDayCards())
                binding.cuList.setOnTouchListener(null)
            }
            MONTHS_VIEW -> {
                showMonthCards(viewModel.getMonthCards())
                binding.cuList.setOnTouchListener(null)
            }
            YEARS_VIEW -> {
                showYearCards(viewModel.getYearCards())
                binding.cuList.setOnTouchListener(null)
            }
            else -> {
                gridAdapter!!.setNodes(viewModel.getCUNodes()!!)
                binding.cuList.setOnTouchListener(scaleGestureHandler)
            }
        }
        handleOptionsMenuUpdate(shouldShowFullInfoAndOptions())
        updateViewSelected()
    }

    fun enableCUClick() {
        ((context as Activity).application as MegaApplication).sendSignalPresenceActivity()
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (PermissionUtils.hasPermissions(context, *permissions)) {
            viewModel.setEnableCUShown(true)
            mManagerActivity.refreshPhotosFragment()
        } else {
            requestCameraUploadPermission(permissions, Constants.REQUEST_CAMERA_ON_OFF)
        }
    }

    private fun observeLiveData() {
        viewModel.cuNodes().observe(
            viewLifecycleOwner, {
                // On enable CU page, don't update layout and view.
                if (isEnableCUFragmentShown()) return@observe
                val showScroller =
                    it?.size!! >= if (getCurrentZoom() < ZOOM_DEFAULT) Constants.MIN_ITEMS_SCROLLBAR_GRID else Constants.MIN_ITEMS_SCROLLBAR
                binding.scroller.visibility = if (showScroller) View.VISIBLE else View.GONE
                if (gridAdapter != null) {
                    gridAdapter!!.setNodes(it)
                }
                updateEnableCUButtons(viewModel.isCUEnabled())
                handlePhotosMenuUpdate(isShowMenu())
                binding.emptyHint.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
                binding.cuList.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
                binding.scroller.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
                mManagerActivity.updateCUViewTypes(if (it.isEmpty()) View.GONE else View.VISIBLE)
            })

        viewModel.nodeToOpen().observe(viewLifecycleOwner, { openNode(it.first, it.second) })

        viewModel.nodeToAnimate().observe(
            viewLifecycleOwner, {
                if (gridAdapter == null || it.first < 0 || it.first >= gridAdapter!!.itemCount) {
                    return@observe
                }

                gridAdapter!!.showSelectionAnimation(
                    it.first, it.second,
                    binding.cuList.findViewHolderForLayoutPosition(it.first)
                )
            })

        viewModel.actionBarTitle().observe(viewLifecycleOwner, { title: String? ->
            val actionBar =
                (context as AppCompatActivity).supportActionBar
            actionBar?.setTitle(title)
        })

        viewModel.actionMode().observe(viewLifecycleOwner, {
            if (it) {
                if (mActionMode == null) {
                    mActionMode = (context as AppCompatActivity).startSupportActionMode(
                        CuActionModeCallback(context, this, viewModel, megaApi)
                    )
                }
                mActionMode!!.title = viewModel.getSelectedNodesCount().toString()
                mActionMode!!.invalidate()
            } else if (mActionMode != null) {
                mActionMode!!.finish()
                mActionMode = null
            }
            animateUI(it)
        })

        viewModel.camSyncEnabled().observe(
            viewLifecycleOwner, {
                this.updateEnableCUButtons(
                    it
                )
            })

        observeDragSupportEvents(viewLifecycleOwner, binding.cuList, Constants.VIEWER_FROM_CUMU)

        viewModel.getDayCardsData().observe(viewLifecycleOwner, { showDayCards(it) })

        viewModel.getMonthCardsData().observe(viewLifecycleOwner, { showMonthCards(it) })

        viewModel.getYearCardsData().observe(viewLifecycleOwner, { showYearCards(it) })
    }

    /**
     * Animates the UI by showing or hiding some views.
     * Enables or disables the translucent navigation bar only if portrait mode.
     *
     * @param hide True if should hide the UI, false otherwise.
     */
    private fun animateUI(hide: Boolean) {
        mManagerActivity.animateCULayout(hide || viewModel.isCUEnabled())
        mManagerActivity.animateBottomView(hide)
        mManagerActivity.setDrawerLockMode(hide)
        checkScroll()
    }

    /**
     * Updates CU enable buttons visibility depending on if CU is enabled/disabled
     * and if the view contains some node.
     *
     * @param cuEnabled True if CU is enabled, false otherwise.
     */
    private fun updateEnableCUButtons(cuEnabled: Boolean) {
        val emptyAdapter = gridAdapter == null || gridAdapter!!.itemCount <= 0
        binding.emptyEnableCuButton.visibility =
            if (!cuEnabled && emptyAdapter) View.VISIBLE else View.GONE
        mManagerActivity.updateEnableCUButton(
            if (selectedView == ALL_VIEW && !cuEnabled
                && !emptyAdapter && mActionMode == null
            ) View.VISIBLE else View.GONE
        )
        if (!cuEnabled) {
            hideCUProgress()
        }
    }

    /**
     * this method handle is show menu.
     *
     * @return false, when no photo here or in the action mode or not in all view, then will hide the menu.
     * Otherwise, true, show menu.
     */
    private fun isShowMenu(): Boolean {
        val emptyAdapter = gridAdapter == null || gridAdapter!!.itemCount <= 0
        return !emptyAdapter && mActionMode == null && selectedView == ALL_VIEW
    }

    private fun showDayCards(dayCards: List<GalleryCard>) {
        if (selectedView == DAYS_VIEW) {
            cardAdapter!!.submitList(dayCards)
        }
    }

    private fun showMonthCards(monthCards: List<GalleryCard>) {
        if (selectedView == MONTHS_VIEW) {
            cardAdapter!!.submitList(monthCards)
        }
    }

    private fun showYearCards(yearCards: List<GalleryCard>) {
        if (selectedView == YEARS_VIEW) {
            cardAdapter!!.submitList(yearCards)
        }
    }

    private fun openNode(position: Int, cuNode: GalleryItem?) {
        if (position < 0 || gridAdapter == null || position >= gridAdapter!!.itemCount) {
            return
        }
        val node = cuNode?.node ?: return
        val parentNode = megaApi.getParentNode(node)
        val intent = Intent(context, FullScreenImageViewerLollipop::class.java)
            .putExtra(Constants.INTENT_EXTRA_KEY_POSITION, cuNode.indexForViewer)
            .putExtra(
                Constants.INTENT_EXTRA_KEY_ORDER_GET_CHILDREN,
                sortOrderManagement.getOrderCamera()
            )
            .putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, node.handle)
            .putExtra(
                Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE,
                if (parentNode == null || parentNode.type == MegaNode.TYPE_ROOT) MegaApiJava.INVALID_HANDLE else parentNode.handle
            )
            .putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.PHOTO_SYNC_ADAPTER)
        putThumbnailLocation(
            intent, binding.cuList, position, Constants.VIEWER_FROM_CUMU,
            gridAdapter!!
        )
        startActivity(intent)
        requireActivity().overridePendingTransition(0, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onNodeClicked(position: Int, node: GalleryItem?) {
        viewModel.onNodeClicked(position, node!!)
    }

    override fun onNodeLongClicked(position: Int, node: GalleryItem?) {
        // Multiple selection only available for zoom default (3 items per row) or zoom out 1x (5 items per row).
        if (getCurrentZoom() == ZOOM_DEFAULT || getCurrentZoom() == ZOOM_OUT_1X) {
            viewModel.onNodeLongClicked(position, node!!)
        }
    }

    fun isEnableCUFragmentShown() = viewModel.isEnableCUShown()

    fun shouldShowFullInfoAndOptions() =
        !isEnableCUFragmentShown() && selectedView == ALL_VIEW

    /**
     * First make all the buttons unselected,
     * then apply selected style for the selected button regarding to the selected view.
     */
    private fun updateViewSelected() {
        super.updateViewSelected(allButton, daysButton, monthsButton, yearsButton, selectedView)
        updateFastScrollerVisibility()
        mManagerActivity.enableHideBottomViewOnScroll(selectedView != ALL_VIEW)
        mManagerActivity.updateEnableCUButton(
            if (selectedView == ALL_VIEW && gridAdapter != null && gridAdapter!!.itemCount > 0 && !viewModel.isCUEnabled()
            ) View.VISIBLE else View.GONE
        )
        if (selectedView != ALL_VIEW) {
            hideCUProgress()
            binding.uploadProgress.visibility = View.GONE
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

    private fun updateFastScrollerVisibility() {
        if (cardAdapter == null) return

        super.updateFastScrollerVisibility(
            selectedView,
            binding.scroller,
            cardAdapter!!.itemCount
        )
    }

    override fun onCardClicked(position: Int, card: GalleryCard) {
        when (selectedView) {
            DAYS_VIEW -> {
                zoomViewModel.restoreDefaultZoom()
                handleZoomMenuItemStatus()
                val cardTemp = viewModel.dayClicked(position, card)!!
                newViewClicked(ALL_VIEW)
                val cuNodePosition = gridAdapter!!.getNodePosition(cardTemp.node.handle)
                openNode(cuNodePosition, gridAdapter!!.getNodeAtPosition(cuNodePosition)!!)
                layoutManager!!.scrollToPosition(cuNodePosition)
                mManagerActivity.showBottomView()
            }
            MONTHS_VIEW -> {
                newViewClicked(DAYS_VIEW)
                layoutManager!!.scrollToPosition(viewModel.monthClicked(position, card))
            }
            YEARS_VIEW -> {
                newViewClicked(MONTHS_VIEW)
                layoutManager!!.scrollToPosition(viewModel.yearClicked(position, card))
            }
        }
    }

    fun updateProgress(visibility: Int, pending: Int) {
        if (binding.uploadProgress.visibility != visibility) {
            binding.uploadProgress.visibility = visibility
            checkScroll()
        }
        binding.uploadProgress.text = StringResourcesUtils
            .getQuantityString(R.plurals.cu_upload_progress, pending, pending)
    }

    fun setDefaultView() {
        newViewClicked(ALL_VIEW)
    }

    override fun handleZoomChange(zoom: Int, needReload: Boolean) {
        handleZoomAdapterLayoutChange(zoom)
        if (needReload) {
            reloadNodes()
        }
    }

    private fun handleZoomAdapterLayoutChange(zoom: Int) {
        if (!viewModel.isEnableCUShown()) {
            viewModel.setZoom(zoom)
            PHOTO_ZOOM_LEVEL = zoom
            val state = layoutManager!!.onSaveInstanceState()
            setGridView()
            layoutManager!!.onRestoreInstanceState(state)
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.selectedViewType = selectedView
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

    fun isInPhotosPage(): Boolean {
        return activity as ManagerActivityLollipop? != null && (activity as ManagerActivityLollipop?)!!.drawerItem == ManagerActivityLollipop.DrawerItem.PHOTOS
    }

    private fun handlePhotosMenuUpdate(isShowMenu: Boolean) {
        if (!isInPhotosPage()) {
            return
        }
        handleOptionsMenuUpdate(isShowMenu)
    }
}