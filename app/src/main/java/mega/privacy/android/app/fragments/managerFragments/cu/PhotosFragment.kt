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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ash.TL
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ListenScrollChangesHelper
import mega.privacy.android.app.components.dragger.DragThumbnailGetter
import mega.privacy.android.app.components.dragger.DragToExitSupport
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.observeDragSupportEvents
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.putThumbnailLocation
import mega.privacy.android.app.databinding.FragmentPhotosBinding
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.photos.ScaleGestureHandler
import mega.privacy.android.app.gallery.adapter.GalleryAdapter
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
import mega.privacy.android.app.utils.ZoomUtil.getItemWidth
import mega.privacy.android.app.utils.ZoomUtil.getMargin
import mega.privacy.android.app.utils.ZoomUtil.getSelectedFrameMargin
import mega.privacy.android.app.utils.ZoomUtil.getSelectedFrameWidth
import mega.privacy.android.app.utils.ZoomUtil.setMargin
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaNode
import java.util.ArrayList
import javax.inject.Inject

@AndroidEntryPoint
class PhotosFragment : BaseZoomFragment(), GalleryCardAdapter.Listener {

    @Inject
    lateinit var sortOrderManagement: SortOrderManagement

    override lateinit var listView: RecyclerView

    private lateinit var mManagerActivity: ManagerActivityLollipop
    private lateinit var binding: FragmentPhotosBinding
    private lateinit var gridAdapter: GalleryAdapter
    private var cardAdapter: GalleryCardAdapter? = null

    private var viewTypesLayout: LinearLayout? = null
    private var yearsButton: TextView? = null
    private var monthsButton: TextView? = null
    private var daysButton: TextView? = null
    private var allButton: TextView? = null

    private val viewModel by viewModels<CuViewModel>()

    private lateinit var layoutManager: GridLayoutManager

    private lateinit var scaleGestureHandler: ScaleGestureHandler

    private var selectedView = ALL_VIEW

    fun reloadNodes() {
        viewModel.loadNodes()
        viewModel.getCards()
    }

    fun checkScroll() {
        if (!this::binding.isInitialized || !this::listView.isInitialized) return

        val isScrolled = listView.canScrollVertically(Constants.SCROLLING_UP_DIRECTION)
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

        mManagerActivity.updateCUViewTypes(View.VISIBLE)
        val currentZoom = PHOTO_ZOOM_LEVEL
        zoomViewModel.setCurrentZoom(currentZoom)
        zoomViewModel.setZoom(currentZoom)
        viewModel.setZoom(currentZoom)
        viewModel.getCards()
        viewModel.getCUNodes()
        setupViewTypes()
        setupOtherViews()
        setupRecyclerView(currentZoom)
        observeLiveData()
        setupActionMode()
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
            allButton!!.setOnClickListener {
                newViewClicked(
                    ALL_VIEW
                )
            }
        }
        if (daysButton != null) {
            daysButton!!.setOnClickListener {
                newViewClicked(
                    DAYS_VIEW
                )
            }
        }
        if (monthsButton != null) {
            monthsButton!!.setOnClickListener {
                newViewClicked(
                    MONTHS_VIEW
                )
            }
        }
        if (yearsButton != null) {
            yearsButton!!.setOnClickListener {
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
    private fun setupRecyclerView(currentZoom: Int) {
        listView = binding.cuList
        listView.setHasFixedSize(true)
        listView.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                checkScroll()
            }
        })
        scaleGestureHandler = ScaleGestureHandler(context, this)
        listView.setOnTouchListener(scaleGestureHandler)
        setGridView(currentZoom)
    }

    private fun setGridView(currentZoom: Int) {
        viewModel.clearSelection()
        val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val spanCount = getSpanCount(selectedView, isPortrait)
        layoutManager = GridLayoutManager(context, spanCount)
        listView.layoutManager = layoutManager
        listView.setPadding(
            0,
            0,
            0,
            resources.getDimensionPixelSize(R.dimen.cu_margin_bottom)
        )
        val params = listView.layoutParams as RelativeLayout.LayoutParams
        if (selectedView == ALL_VIEW) {
            val imageMargin = getMargin(context, currentZoom)
            setMargin(context, params, currentZoom)
            val gridWidth = getItemWidth(context, outMetrics, currentZoom, spanCount)
            val icSelectedWidth = getSelectedFrameWidth(context, currentZoom)
            val icSelectedMargin = getSelectedFrameMargin(context, currentZoom)
            val itemSizeConfig = GalleryItemSizeConfig(
                currentZoom, gridWidth,
                icSelectedWidth, imageMargin,
                resources.getDimensionPixelSize(R.dimen.cu_fragment_selected_padding),
                icSelectedMargin,
                resources.getDimensionPixelSize(
                    R.dimen.cu_fragment_selected_round_corner_radius
                )
            )

            gridAdapter = GalleryAdapter(
                actionModeViewModel,
                itemOperationViewModel,
                itemSizeConfig
            )

            layoutManager.apply {
                spanSizeLookup = gridAdapter.getSpanSizeLookup(spanCount)
                val itemDimen = getItemWidth(context, outMetrics, currentZoom, spanCount)
                gridAdapter.setItemDimen(itemDimen)
            }

            gridAdapter.submitList(viewModel.getCUNodes())
            listView.adapter = gridAdapter
        } else {
            val cardMargin =
                resources.getDimensionPixelSize(if (isPortrait) R.dimen.card_margin_portrait else R.dimen.card_margin_landscape)
            val cardWidth =
                (outMetrics.widthPixels - cardMargin * spanCount * 2 - cardMargin * 2) / spanCount
            cardAdapter = GalleryCardAdapter(selectedView, cardWidth, cardMargin, this)
            cardAdapter!!.setHasStableIds(true)
            listView.adapter = cardAdapter
            params.rightMargin = cardMargin
            params.leftMargin = params.rightMargin
        }
        listView.layoutParams = params
        binding.scroller.setRecyclerView(listView)
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
        setGridView(getCurrentZoom())

        when (selectedView) {
            DAYS_VIEW -> {
                showDayCards(viewModel.getDayCards())
                listView.setOnTouchListener(null)
            }
            MONTHS_VIEW -> {
                showMonthCards(viewModel.getMonthCards())
                listView.setOnTouchListener(null)
            }
            YEARS_VIEW -> {
                showYearCards(viewModel.getYearCards())
                listView.setOnTouchListener(null)
            }
            else -> {
                gridAdapter.submitList(viewModel.getCUNodes())
                listView.setOnTouchListener(scaleGestureHandler)
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

                actionModeViewModel.setNodesData(it.filter { nodeItem -> nodeItem.type == GalleryItem.TYPE_IMAGE || nodeItem.type == GalleryItem.TYPE_VIDEO })
                val showScroller =
                    it?.size!! >= if (getCurrentZoom() < ZOOM_DEFAULT) Constants.MIN_ITEMS_SCROLLBAR_GRID else Constants.MIN_ITEMS_SCROLLBAR
                binding.scroller.visibility = if (showScroller) View.VISIBLE else View.GONE
                if (this::gridAdapter.isInitialized) {
                    gridAdapter.submitList(it)
                }
                updateEnableCUButtons(viewModel.isCUEnabled())
                handlePhotosMenuUpdate(isShowMenu())
                binding.emptyHint.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
                listView.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
                binding.scroller.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
                mManagerActivity.updateCUViewTypes(if (it.isEmpty()) View.GONE else View.VISIBLE)
            })

        itemOperationViewModel.openItemEvent.observe(viewLifecycleOwner, EventObserver {
            openNode(it.index, it as GalleryItem)
        })

        viewModel.actionBarTitle().observe(viewLifecycleOwner, { title: String? ->
            val actionBar =
                (context as AppCompatActivity).supportActionBar
            actionBar?.title = title
        })

        viewModel.camSyncEnabled().observe(
            viewLifecycleOwner, {
                this.updateEnableCUButtons(
                    it
                )
            })

        observeDragSupportEvents(viewLifecycleOwner, listView, Constants.VIEWER_FROM_CUMU)

        viewModel.getDayCardsData().observe(viewLifecycleOwner, { showDayCards(it) })

        viewModel.getMonthCardsData().observe(viewLifecycleOwner, { showMonthCards(it) })

        viewModel.getYearCardsData().observe(viewLifecycleOwner, { showYearCards(it) })
    }

    /**
     * Updates CU enable buttons visibility depending on if CU is enabled/disabled
     * and if the view contains some node.
     *
     * @param cuEnabled True if CU is enabled, false otherwise.
     */
    private fun updateEnableCUButtons(cuEnabled: Boolean) {
        binding.emptyEnableCuButton.visibility =
            if (!cuEnabled && emptyAdapter()) View.VISIBLE else View.GONE
        mManagerActivity.updateEnableCUButton(
            if (selectedView == ALL_VIEW && !cuEnabled
                && !emptyAdapter() && actionMode == null
            ) View.VISIBLE else View.GONE
        )
        if (!cuEnabled) {
            hideCUProgress()
        }
    }

    private fun emptyAdapter(): Boolean {
        val result = if (this::gridAdapter.isInitialized) {
            TL.log("count: ${gridAdapter.itemCount}")
            gridAdapter.itemCount <= 0
        } else {
            false
        }
        TL.log(result)

        return false
    }

    /**
     * this method handle is show menu.
     *
     * @return false, when no photo here or in the action mode or not in all view, then will hide the menu.
     * Otherwise, true, show menu.
     */
    private fun isShowMenu() = !emptyAdapter() && actionMode == null && selectedView == ALL_VIEW

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
        if (position < 0 || position >= gridAdapter.itemCount) {
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
            intent, listView, position, Constants.VIEWER_FROM_CUMU,
            gridAdapter
        )
        startActivity(intent)
        requireActivity().overridePendingTransition(0, 0)
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
            if (selectedView == ALL_VIEW && !emptyAdapter() && !viewModel.isCUEnabled()
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
                val cuNodePosition = gridAdapter.getNodePosition(cardTemp.node.handle)
                openNode(cuNodePosition, gridAdapter.getNodeAtPosition(cuNodePosition)!!)
                layoutManager.scrollToPosition(cuNodePosition)
                mManagerActivity.showBottomView()
            }
            MONTHS_VIEW -> {
                newViewClicked(DAYS_VIEW)
                layoutManager.scrollToPosition(viewModel.monthClicked(position, card))
            }
            YEARS_VIEW -> {
                newViewClicked(MONTHS_VIEW)
                layoutManager.scrollToPosition(viewModel.yearClicked(position, card))
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
            val state = layoutManager.onSaveInstanceState()
            setGridView(zoom)
            layoutManager.onRestoreInstanceState(state)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.selectedViewType = selectedView
    }

    override fun handleOnCreateOptionsMenu() {
        handleOptionsMenuUpdate(isShowMenu())
    }

    override fun animateBottomView() {
        val hide = actionMode != null
        mManagerActivity.animateCULayout(hide || viewModel.isCUEnabled())
        mManagerActivity.animateBottomView(hide)
        mManagerActivity.setDrawerLockMode(hide)
        checkScroll()
    }


    override fun getNodeCount() = viewModel.getRealMegaNodes().size

    override fun updateUiWhenAnimationEnd() {
        val newList = ArrayList(viewModel.getCUNodes())
        gridAdapter.submitList(newList)
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
        return activity as ManagerActivityLollipop? != null && (activity as ManagerActivityLollipop?)!!.isInPhotosPage
    }

    private fun handlePhotosMenuUpdate(isShowMenu: Boolean) {
        if (!isInPhotosPage()) {
            return
        }
        handleOptionsMenuUpdate(isShowMenu)
    }
}