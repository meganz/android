package mega.privacy.android.app.fragments.homepage.photos

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.components.GestureScaleListener
import mega.privacy.android.app.components.dragger.DragThumbnailGetter
import mega.privacy.android.app.components.dragger.DragToExitSupport
import mega.privacy.android.app.components.scrollBar.FastScroller
import mega.privacy.android.app.databinding.FragmentImagesBinding
import mega.privacy.android.app.gallery.data.MediaCardType
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.fragments.homepage.ActionModeCallback
import mega.privacy.android.app.fragments.homepage.ActionModeViewModel
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.ItemOperationViewModel
import mega.privacy.android.app.fragments.homepage.getRoundingParams
import mega.privacy.android.app.fragments.managerFragments.cu.CustomHideBottomViewOnScrollBehaviour
import mega.privacy.android.app.gallery.adapter.GalleryAdapter
import mega.privacy.android.app.gallery.adapter.GalleryCardAdapter
import mega.privacy.android.app.gallery.data.GalleryCard
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.data.GalleryItemSizeConfig
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.modalbottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.Constants.*
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.ORDER_MODIFICATION_DESC
import nz.mega.sdk.MegaChatApiJava

@AndroidEntryPoint
class ImagesFragment : BaseFragment(), GestureScaleListener.GestureScaleCallback,
    GalleryCardAdapter.Listener {

    companion object {
        const val ALL_VIEW = 0
        const val DAYS_VIEW = 1
        const val MONTHS_VIEW = 2
        const val YEARS_VIEW = 3

        const val SPAN_CARD_PORTRAIT = 1
        const val SPAN_CARD_LANDSCAPE = 2

        const val DAYS_INDEX = 0
        const val MONTHS_INDEX = 1
        const val YEARS_INDEX = 2

        const val VIEW_TYPE = "VIEW_TYPE"
    }

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
    private val viewModel by viewModels<ImagesViewModel>()
    private val zoomViewModel by viewModels<ZoomViewModel>()
    private val actionModeViewModel by viewModels<ActionModeViewModel>()
    private val itemOperationViewModel by viewModels<ItemOperationViewModel>()

    private var showBottomNav = true

    private var selectedView = ALL_VIEW
    private var adapterType = 0

    private lateinit var binding: FragmentImagesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mManagerActivity = activity as ManagerActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentImagesBinding.inflate(inflater, container, false)
        adapterType = PHOTOS_BROWSE_ADAPTER
        setupBinding()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        selectedView =
            savedInstanceState?.getInt(VIEW_TYPE) ?: ALL_VIEW

        subscribeBaseObservers()
        setHasOptionsMenu(true)
        showBottomNav = false
        initViewCreated()
        subscribeObservers()
    }

    override fun onDestroyView() {
        viewModel.cancelSearch()
        super.onDestroyView()
    }

    private fun setupBinding() {
        binding.apply {
            viewModel = this@ImagesFragment.viewModel
            lifecycleOwner = viewLifecycleOwner
            viewTypePanel = photosViewType.root
        }

        listView = binding.photoList
        scroller = binding.scroller
        viewTypePanel = binding.photosViewType.root
        yearsButton = binding.photosViewType.yearsButton
        monthsButton = binding.photosViewType.monthsButton
        daysButton = binding.photosViewType.daysButton
        allButton = binding.photosViewType.allButton
    }

    private fun initViewCreated() {
        val currentZoom = ZoomUtil.IMAGES_ZOOM_LEVEL
        zoomViewModel.setCurrentZoom(currentZoom)
        zoomViewModel.setZoom(currentZoom)
        viewModel.mZoom = currentZoom
        setupEmptyHint()
        setupListView()
        setupTimePanel()
        setupListAdapter(currentZoom, viewModel.items.value)
        if (isInActionMode()) {
            mManagerActivity.updateCUViewTypes(View.GONE)
        }
    }

    fun handleZoomChange(zoom: Int, needReload: Boolean) {
        ZoomUtil.IMAGES_ZOOM_LEVEL = zoom
        handleZoomAdapterLayoutChange(zoom)
        if (needReload) {
            loadPhotos()
        }
    }

    fun handleOnCreateOptionsMenu() {
        val hasImages = gridAdapterHasData()
        handleOptionsMenuUpdate(hasImages && shouldShowZoomMenuItem())
        removeSortByMenu()
    }

    private fun subscribeObservers() {
        viewModel.items.observe(viewLifecycleOwner) {
            if (isGridAdapterInitialized()) {
                gridAdapter.submitList(it)
            }
            actionModeViewModel.setNodesData(it.filter { nodeItem -> nodeItem.type == MediaCardType.Image })
            viewTypePanel.visibility = if (it.isEmpty() || actionMode != null) View.GONE else View.VISIBLE
            if (it.isEmpty()) {
                handleOptionsMenuUpdate(false)
            } else {
                handleOptionsMenuUpdate(shouldShowZoomMenuItem())
            }
            removeSortByMenu()
        }
    }

    private fun setupEmptyHint() {
        binding.emptyHint.emptyHintImage.isVisible = false
        binding.emptyHint.emptyHintText.isVisible = false
        binding.emptyHint.emptyHintText.text = HtmlCompat.fromHtml(
            TextUtil.formatEmptyScreenText(
                context,
                StringResourcesUtils.getString(R.string.homepage_empty_hint_photos)
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
    fun newViewClicked(selectedView: Int) {
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
        removeSortByMenu()
    }

    private fun handleZoomAdapterLayoutChange(zoom: Int) {
        val state = listView.layoutManager?.onSaveInstanceState()
        setupListAdapter(zoom, viewModel.items.value)
        viewModel.mZoom = zoom
        listView.layoutManager?.onRestoreInstanceState(state)
    }

    fun loadPhotos() = viewModel.loadPhotos(true)

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (activity as ManagerActivity? != null && (activity as ManagerActivity?)!!.isInImagesPage) {
            super.onCreateOptionsMenu(menu, inflater)
            inflater.inflate(R.menu.fragment_images_toolbar, menu)
            this.menu = menu
            handleOnCreateOptionsMenu()
            handleZoomMenuItemStatus()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (activity as ManagerActivity? != null && (activity as ManagerActivity?)!!.isInImagesPage) {
            when (item.itemId) {
                R.id.action_zoom_in -> {
                    zoomIn()
                }
                R.id.action_zoom_out -> {
                    zoomOut()
                }
                R.id.action_menu_sort_by -> {
                    mManagerActivity.showNewSortByPanel(ORDER_CAMERA)
                }
            }
            return super.onOptionsItemSelected(item)
        }
        return true
    }

    fun getOrder() = ORDER_MODIFICATION_DESC


    fun animateBottomView(hide: Boolean) {
        val deltaY =
            viewTypePanel.height.toFloat() + resources.getDimensionPixelSize(R.dimen.cu_view_type_button_vertical_margin)

        if (hide) {
            viewTypePanel
                .animate()
                .translationYBy(deltaY)
                .setDuration(ANIMATION_DURATION)
                .withEndAction { viewTypePanel.visibility = View.GONE }
                .start()
        } else {
            viewTypePanel
                .animate()
                .translationYBy(-deltaY)
                .setDuration(ANIMATION_DURATION)
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
            VIEWER_FROM_PHOTOS
        )
    }

    private fun setupActionMode() {
        actionModeCallback =
            ActionModeCallback(mManagerActivity, actionModeViewModel, megaApi)
        observeItemLongClick()
        observeSelectedItems()
        observeAnimatedItems()
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

    override fun onCardClicked(position: Int, card: GalleryCard) {
        when (selectedView) {
            DAYS_VIEW -> {
                handleZoomMenuItemStatus()
                newViewClicked(ALL_VIEW)
                val photoPosition = gridAdapter.getNodePosition(card.node.handle)
                layoutManager.scrollToPosition(photoPosition)
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
                val parentNodeHandle = nodeItem.node?.parentHandle ?: return
                val nodeHandle = nodeItem.node?.handle ?: MegaApiJava.INVALID_HANDLE
                val childrenNodes = viewModel.getItemsHandle()
                val intent = when (adapterType) {
                    ALBUM_CONTENT_ADAPTER, PHOTOS_BROWSE_ADAPTER -> {
                        ImageViewerActivity.getIntentForChildren(
                            requireContext(),
                            childrenNodes,
                            nodeHandle
                        )
                    }
                    else -> {
                        ImageViewerActivity.getIntentForParentNode(
                            requireContext(),
                            parentNodeHandle,
                            getOrder(),
                            nodeHandle
                        )
                    }
                }

                (listView.adapter as? DragThumbnailGetter)?.let { getter ->
                    DragToExitSupport.putThumbnailLocation(
                        intent,
                        listView,
                        nodeItem.index,
                        VIEWER_FROM_PHOTOS,
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
                override fun onAnimationRepeat(animation: Animator?) {
                }

                override fun onAnimationEnd(animation: Animator?) {
                    updateUiWhenAnimationEnd()
                }

                override fun onAnimationCancel(animation: Animator?) {
                }

                override fun onAnimationStart(animation: Animator?) {
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
                SNACKBAR_TYPE,
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
            menuItem.icon,
            colorRes
        )
        menuItem.isEnabled = isEnable
    }

    fun handleOptionsMenuUpdate(shouldShow: Boolean) {
        if (this::menu.isInitialized) {
            menu.findItem(R.id.action_zoom_in)?.isVisible = shouldShow
            menu.findItem(R.id.action_zoom_out)?.isVisible = shouldShow
            menu.findItem(R.id.action_menu_sort_by)?.isVisible = shouldShow
        }
    }

    fun removeSortByMenu() {
        if (this::menu.isInitialized) {
            menu.removeItem(R.id.action_menu_sort_by)
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

    private fun updateViewSelected() {
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
            if (!gridView && cardAdapter.itemCount >= MIN_ITEMS_SCROLLBAR)
                View.VISIBLE
            else
                View.GONE
    }

    private fun setHideBottomViewScrollBehaviour() {
        val params = viewTypePanel.layoutParams as CoordinatorLayout.LayoutParams
        params.behavior =
            if (selectedView != ALL_VIEW) CustomHideBottomViewOnScrollBehaviour<LinearLayout>() else null
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
     * Sub fragment can custom operation when start ActionMode
     */
    fun whenStartActionMode() {
        mManagerActivity.showHideBottomNavigationView(true)
        animateBottomView(true)
    }

    /**
     * Sub fragment can custom operation when end ActionMode
     */
    fun whenEndActionMode() {
        mManagerActivity.showHideBottomNavigationView(!showBottomNav)
        if (viewModel.items.value != null && viewModel.items.value!!.isNotEmpty()) {
            animateBottomView(false)
        } else {
            animateBottomView(true)
        }
    }

    /**
     * Check is in action mode.
     *
     * @return true in, false not in
     */
    fun isInActionMode() = actionMode != null
}