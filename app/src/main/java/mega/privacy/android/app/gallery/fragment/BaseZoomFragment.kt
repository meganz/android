package mega.privacy.android.app.gallery.fragment

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import mega.privacy.android.app.R
import mega.privacy.android.app.components.GestureScaleListener.GestureScaleCallback
import mega.privacy.android.app.components.dragger.DragThumbnailGetter
import mega.privacy.android.app.components.dragger.DragToExitSupport
import mega.privacy.android.app.components.scrollBar.FastScroller
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.fragments.homepage.*
import mega.privacy.android.app.fragments.homepage.photos.ScaleGestureHandler
import mega.privacy.android.app.fragments.homepage.photos.ZoomViewModel
import mega.privacy.android.app.fragments.managerFragments.cu.CustomHideBottomViewOnScrollBehaviour
import mega.privacy.android.app.fragments.managerFragments.cu.album.AlbumContentFragment
import mega.privacy.android.app.gallery.adapter.GalleryAdapter
import mega.privacy.android.app.gallery.adapter.GalleryCardAdapter
import mega.privacy.android.app.gallery.data.GalleryCard
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.data.GalleryItemSizeConfig
import mega.privacy.android.app.gallery.ui.GalleryViewModel
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.modalbottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.ZoomUtil.ZOOM_OUT_2X
import mega.privacy.android.app.utils.ZoomUtil.getItemWidth
import mega.privacy.android.app.utils.ZoomUtil.getMargin
import mega.privacy.android.app.utils.ZoomUtil.getSelectedFrameMargin
import mega.privacy.android.app.utils.ZoomUtil.getSelectedFrameWidth
import mega.privacy.android.app.utils.ZoomUtil.setMargin
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApiJava

/**
 * A parent fragment with basic zoom UI logic, like menu, gestureScaleCallback.
 */
abstract class BaseZoomFragment : BaseFragment(), GestureScaleCallback,
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

    protected lateinit var mManagerActivity: ManagerActivity

    // View type panel
    protected lateinit var viewTypePanel: View
    protected lateinit var yearsButton: TextView
    protected lateinit var monthsButton: TextView
    protected lateinit var daysButton: TextView
    protected lateinit var allButton: TextView

    // List view
    protected lateinit var listView: RecyclerView
    protected lateinit var gridAdapter: GalleryAdapter
    protected lateinit var cardAdapter: GalleryCardAdapter
    protected lateinit var layoutManager: GridLayoutManager
    protected lateinit var scroller: FastScroller
    protected lateinit var scaleGestureHandler: ScaleGestureHandler

    protected lateinit var menu: Menu

    // Action mode
    protected var actionMode: ActionMode? = null
    protected lateinit var actionModeCallback: ActionModeCallback

    // View model
    abstract val viewModel : GalleryViewModel
    protected val zoomViewModel by viewModels<ZoomViewModel>()
    protected val actionModeViewModel by viewModels<ActionModeViewModel>()
    protected val itemOperationViewModel by viewModels<ItemOperationViewModel>()

    protected var selectedView = ALL_VIEW
    protected open var adapterType = 0

    /**
     * When zoom changes,handle zoom for sub class
     */
    abstract fun handleZoomChange(zoom: Int, needReload: Boolean)

    abstract fun getOrder(): Int

    /**
     * Handle menus for sub class
     */
    abstract fun handleOnCreateOptionsMenu()

    open fun animateBottomView() {
        // AlbumContentFragment doesn't have view type panel.
        if(this is AlbumContentFragment) return

        val deltaY =
            viewTypePanel.height.toFloat() + resources.getDimensionPixelSize(R.dimen.cu_view_type_button_vertical_margin)

        if (viewTypePanel.isVisible) {
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
            gridAdapter.submitList(newList)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mManagerActivity = activity as ManagerActivity
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        selectedView = savedInstanceState?.getInt(VIEW_TYPE) ?: ALL_VIEW

        subscribeObservers()
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_images_toolbar, menu)
        this.menu = menu
        handleOnCreateOptionsMenu()
        handleZoomMenuItemStatus()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(VIEW_TYPE, selectedView)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
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

    private fun subscribeObservers() {
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
                        NodeOptionsBottomSheetDialogFragment.MODE5
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

        val params = listView.layoutParams as MarginLayoutParams

        layoutManager = GridLayoutManager(context, spanCount)
        listView.layoutManager = layoutManager

        if (selectedView == ALL_VIEW) {
            val imageMargin = getMargin(context, currentZoom)
            setMargin(context, params, currentZoom)
            val gridWidth = getItemWidth(context, outMetrics, currentZoom, spanCount, isPortrait)
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

            gridAdapter =
                GalleryAdapter(actionModeViewModel, itemOperationViewModel, itemSizeConfig)

            layoutManager.apply {
                spanSizeLookup = gridAdapter.getSpanSizeLookup(spanCount)
                val itemDimen = getItemWidth(context, outMetrics, currentZoom, spanCount, isPortrait)
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
    protected fun setupListView() {
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
    protected fun showViewTypePanel() {
        val params = viewTypePanel.layoutParams as MarginLayoutParams
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
                val nodeHandle = nodeItem.node?.handle ?: INVALID_HANDLE
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
                        Constants.VIEWER_FROM_PHOTOS,
                        getter
                    )
                }

                startActivity(intent)
                mManagerActivity.overridePendingTransition(0, 0)
            }
    }

    fun gridAdapterHasData() = viewModel.items.value?.isNotEmpty()?:false

    fun layoutManagerInitialized() = this::layoutManager.isInitialized

    fun listViewInitialized() = this::listView.isInitialized

    private fun observeItemLongClick() =
        actionModeViewModel.longClick.observe(viewLifecycleOwner, EventObserver {
            if (zoomViewModel.getCurrentZoom() != ZOOM_OUT_2X) {
                doIfOnline { actionModeViewModel.enterActionMode(it) }
                animateBottomView()
            }
        })

    private fun observeSelectedItems() =
        actionModeViewModel.selectedNodes.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                actionMode?.apply {
                    finish()
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
            animateBottomView()
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

    protected fun getCurrentZoom(): Int {
        return zoomViewModel.getCurrentZoom()
    }

    protected open fun updateViewSelected() {
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

    protected fun setupTimePanel() {
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
    protected open fun newViewClicked(selectedView: Int) {
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
    protected fun showCards(dateCards: List<List<GalleryCard>?>?) {
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

    protected fun updateFastScrollerVisibility() {
        if (!this::cardAdapter.isInitialized) return

        val gridView = selectedView == ALL_VIEW

        scroller.visibility =
            if (!gridView && cardAdapter.itemCount >= MIN_ITEMS_SCROLLBAR)
                View.VISIBLE
            else
                View.GONE
    }

    protected open fun setHideBottomViewScrollBehaviour() {
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
    protected fun shouldShowZoomMenuItem() = selectedView == ALL_VIEW

    /**
     * Get how many items will be shown per row, depends on screen direction and zoom level if all view is selected.
     *
     * @param isPortrait true, on portrait mode, false otherwise.
     */
    protected fun getSpanCount(selectedView: Int, isPortrait: Boolean): Int {
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
    fun isGridAdapterInitialized():Boolean{
        return this::gridAdapter.isInitialized
    }
}