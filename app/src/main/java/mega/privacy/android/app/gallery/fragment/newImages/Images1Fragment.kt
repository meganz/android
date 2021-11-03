package mega.privacy.android.app.gallery.fragment.newImages

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.components.GestureScaleListener
import mega.privacy.android.app.components.GestureScaleListener.GestureScaleCallback
import mega.privacy.android.app.components.ListenScrollChangesHelper
import mega.privacy.android.app.components.SimpleDividerItemDecoration
import mega.privacy.android.app.components.dragger.DragThumbnailGetter
import mega.privacy.android.app.components.dragger.DragToExitSupport
import mega.privacy.android.app.databinding.FragmentImages1Binding
import mega.privacy.android.app.fragments.homepage.*
import mega.privacy.android.app.fragments.homepage.photos.PhotoNodeItem
import mega.privacy.android.app.fragments.homepage.photos.PhotosBrowseAdapter
import mega.privacy.android.app.fragments.managerFragments.cu.CUCard
import mega.privacy.android.app.fragments.managerFragments.cu.CUCardViewAdapter
import mega.privacy.android.app.fragments.managerFragments.cu.CustomHideBottomViewOnScrollBehaviour
import mega.privacy.android.app.fragments.managerFragments.cu.PhotosFragment
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.lollipop.ManagerActivityLollipop.*
import mega.privacy.android.app.modalbottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.ZoomUtil.disableButton
import mega.privacy.android.app.utils.ZoomUtil.needReload
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiJava
import java.util.*

@AndroidEntryPoint
class Images1Fragment : BaseBindingFragmentKt<NewImagesViewModel, FragmentImages1Binding>(),
    GestureScaleCallback {

    override val viewModel by viewModels<NewImagesViewModel>()
    private val actionModeViewModel by viewModels<ActionModeViewModel>()
    private val itemOperationViewModel by viewModels<ItemOperationViewModel>()

    private lateinit var listView: RecyclerView

    private lateinit var viewTypePanel: View
    private lateinit var yearsButton: TextView
    private lateinit var monthsButton: TextView
    private lateinit var daysButton: TextView
    private lateinit var allButton: TextView

    private lateinit var browseAdapter: ImagesBrowseAdapter
    private lateinit var cardAdapter: CUCardViewAdapter

    private lateinit var gridLayoutManager: GridLayoutManager
    private var linearLayoutManager: LinearLayoutManager? = null

    private var actionMode: ActionMode? = null
    private lateinit var actionModeCallback: ActionModeCallback

    private lateinit var itemDecoration: SimpleDividerItemDecoration

    private var currentZoom = ZoomUtil.ZOOM_DEFAULT

    private var selectedView = PhotosFragment.ALL_VIEW

    override fun initBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentImages1Binding = FragmentImages1Binding.inflate(inflater, container, false).apply {
        viewModel = this@Images1Fragment.viewModel
        viewTypePanel = photosViewType.root
    }

    override fun init() {
        setToolbarMenu()
        setupEmptyHint()
        setupListView()
        setupTimePanel()
        setupListAdapter(currentZoom)
        setupFastScroller()
        setupActionMode()
        setupNavigation()
    }

    private fun setToolbarMenu() {
        binding.layoutTitleBar.toolbar.apply {
            inflateMenu(R.menu.fragment_images_toolbar)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_zoom_in -> {
                        zoomIn()
                        handleZoomMenuItemStatus()
                        true
                    }
                    R.id.action_zoom_out -> {
                        zoomOut()
                        handleZoomMenuItemStatus()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun handleZoomMenuItemStatus() {
        val canZoomOut = viewModel.zoomManager.canZoomOut()
        val canZoomIn = viewModel.zoomManager.canZoomIn()
        if (!canZoomIn && canZoomOut) {
            handleEnableToolbarMenuIcon(R.id.action_zoom_in, false)
            handleEnableToolbarMenuIcon(R.id.action_zoom_out, true)
        } else if (canZoomIn && !canZoomOut) {
            handleEnableToolbarMenuIcon(R.id.action_zoom_in, true)
            handleEnableToolbarMenuIcon(R.id.action_zoom_out, false)
        } else {
            //canZoomOut && canZoomIn
            handleEnableToolbarMenuIcon(R.id.action_zoom_in, true)
            handleEnableToolbarMenuIcon(R.id.action_zoom_out, true)
        }
    }

    private fun handleEnableToolbarMenuIcon(menuItemId: Int, isEnable: Boolean) {
        val toolbar = binding.layoutTitleBar.toolbar
        val menuItem = toolbar.menu.findItem(menuItemId)
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

    override fun subscribeObservers() {

        viewModel.getZoom().observe(viewLifecycleOwner, { zoom: Int ->
            // Out 3X: organize by year, In 1X: oragnize by day, both need to reload nodes.
            val needReload = needReload(currentZoom, zoom)
            viewModel.zoomManager.setCurrentZoom(zoom)
            //refreshSelf()
            handleZoomAdapterLayoutChange(zoom)
            if (needReload) {
                loadPhotos()
            }
        })


        viewModel.items.observe(viewLifecycleOwner) {
            actionModeViewModel.setNodesData(it.filter { nodeItem -> nodeItem.type == PhotoNodeItem.TYPE_PHOTO })
        }

        viewModel.dateCards.observe(viewLifecycleOwner, ::showCards)

        viewModel.refreshCards.observe(viewLifecycleOwner) {
            if (it && selectedView != PhotosFragment.ALL_VIEW) {
                viewModel.refreshing()
                showCards(viewModel.dateCards.value)
            }
        }

        DragToExitSupport.observeDragSupportEvents(
            viewLifecycleOwner,
            listView,
            Constants.VIEWER_FROM_PHOTOS
        )
    }

    private val cardClickedListener = object : CUCardViewAdapter.Listener {

        override fun onCardClicked(position: Int, @NonNull card: CUCard) {
            when (selectedView) {
                PhotosFragment.DAYS_VIEW -> {
//                    callManager {
//                        it.restoreDefaultZoom()
//                    }
                    viewModel.zoomManager.restoreDefaultZoom()
                    newViewClicked(PhotosFragment.ALL_VIEW)
                    val photoPosition = browseAdapter.getNodePosition(card.node.handle)
                    gridLayoutManager.scrollToPosition(photoPosition)

                    val node = browseAdapter.getNodeAtPosition(photoPosition)
                    node?.let {
                        RunOnUIThreadUtils.post {
                            openPhoto(it)
                        }
                    }
                }
                PhotosFragment.MONTHS_VIEW -> {
                    newViewClicked(PhotosFragment.DAYS_VIEW)
                    gridLayoutManager.scrollToPosition(viewModel.monthClicked(position, card))
                }
                PhotosFragment.YEARS_VIEW -> {
                    newViewClicked(PhotosFragment.MONTHS_VIEW)
                    gridLayoutManager.scrollToPosition(viewModel.yearClicked(position, card))
                }
            }

            showViewTypePanel()
        }
    }

    private fun setupEmptyHint() {
        binding.emptyHint.emptyHintImage.isVisible = false
        binding.emptyHint.emptyHintText.isVisible = false
        binding.emptyHint.emptyHintText.text =
            getString(R.string.homepage_empty_hint_photos).toUpperCase(Locale.ROOT)
    }

    private fun setupTimePanel() {
        yearsButton = binding.photosViewType.yearsButton.apply {
            setOnClickListener {
                newViewClicked(PhotosFragment.YEARS_VIEW)
            }
        }
        monthsButton = binding.photosViewType.monthsButton.apply {
            setOnClickListener {
                newViewClicked(PhotosFragment.MONTHS_VIEW)
            }
        }
        daysButton = binding.photosViewType.daysButton.apply {
            setOnClickListener {
                newViewClicked(PhotosFragment.DAYS_VIEW)
            }
        }
        allButton = binding.photosViewType.allButton.apply {
            setOnClickListener {
                newViewClicked(PhotosFragment.ALL_VIEW)
            }
        }

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val params = viewTypePanel.layoutParams
            params.width = outMetrics.heightPixels
            viewTypePanel.layoutParams = params
        }

        updateViewSelected()
    }

    /**
     * First make all the buttons unselected,
     * then apply selected style for the selected button regarding to the selected view.
     */
    private fun updateViewSelected() {
        setViewTypeButtonStyle(allButton, false)
        setViewTypeButtonStyle(daysButton, false)
        setViewTypeButtonStyle(monthsButton, false)
        setViewTypeButtonStyle(yearsButton, false)

        when (selectedView) {
            PhotosFragment.DAYS_VIEW -> setViewTypeButtonStyle(daysButton, true)
            PhotosFragment.MONTHS_VIEW -> setViewTypeButtonStyle(monthsButton, true)
            PhotosFragment.YEARS_VIEW -> setViewTypeButtonStyle(yearsButton, true)
            else -> setViewTypeButtonStyle(allButton, true)
        }
    }

    private fun updateFastScrollerVisibility() {
        val gridView = selectedView == PhotosFragment.ALL_VIEW

        binding.scroller.visibility =
            if (!gridView && cardAdapter.itemCount >= Constants.MIN_ITEMS_SCROLLBAR)
                View.VISIBLE
            else
                View.GONE
    }

    /**
     * Apply selected/unselected style for the TextView button.
     *
     * @param textView The TextView button to be applied with the style.
     * @param enabled true, apply selected style; false, apply unselected style.
     */
    private fun setViewTypeButtonStyle(textView: TextView, enabled: Boolean) {
        textView.setBackgroundResource(
            if (enabled)
                R.drawable.background_18dp_rounded_selected_button
            else
                R.drawable.background_18dp_rounded_unselected_button
        )

        StyleUtils.setTextStyle(
            context,
            textView,
            if (enabled) R.style.TextAppearance_Mega_Subtitle2_Medium_WhiteGrey87 else R.style.TextAppearance_Mega_Subtitle2_Normal_Grey87White87,
            if (enabled) R.color.white_grey_087 else R.color.grey_087_white_087,
            false
        )
    }

    /**
     * Show the selected card view after corresponding button is clicked.
     *
     * @param selectedView The selected view.
     */
    private fun newViewClicked(selectedView: Int) {
        if (this.selectedView == selectedView) return

        this.selectedView = selectedView
        setupListAdapter(currentZoom)

        when (selectedView) {
            PhotosFragment.DAYS_VIEW, PhotosFragment.MONTHS_VIEW, PhotosFragment.YEARS_VIEW -> showCards(
                viewModel.dateCards.value
            )
            else -> {
            }
        }

        updateViewSelected()

        // If selected view is not all view, add layout param behaviour, so that button panel will go off when scroll.
        val params = viewTypePanel.layoutParams as CoordinatorLayout.LayoutParams
        params.behavior =
            if (selectedView != PhotosFragment.ALL_VIEW) CustomHideBottomViewOnScrollBehaviour<LinearLayout>() else null
        viewTypePanel.layoutParams = params
    }

    private fun doIfOnline(operation: () -> Unit) {
        if (Util.isOnline(context)) {
            operation()
        } else {
            val activity = activity as ManagerActivityLollipop

            activity.hideKeyboardSearch()  // Make the snack bar visible to the user
            activity.showSnackbar(
                Constants.SNACKBAR_TYPE,
                context.getString(R.string.error_server_connection_problem),
                MegaChatApiJava.MEGACHAT_INVALID_HANDLE
            )
        }
    }

    private fun setupNavigation() {
        itemOperationViewModel.openItemEvent.observe(viewLifecycleOwner, EventObserver {
            openPhoto(it as PhotoNodeItem)
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
     * Only refresh the list items of uiDirty = true
     */
    private fun updateUi() = viewModel.items.value?.let { it ->
        val newList = ArrayList(it)
        browseAdapter.submitList(newList)

    }

    private fun elevateToolbarWhenScrolling() = ListenScrollChangesHelper().addViewToListen(
        listView
    ) { v: View?, _, _, _, _ ->
        callManager {
            it.changeAppBarElevation(v!!.canScrollVertically(-1))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListView() {
        listView = binding.photoList
        listView.itemAnimator = Util.noChangeRecyclerViewItemAnimator()
        elevateToolbarWhenScrolling()

        itemDecoration = SimpleDividerItemDecoration(context)

        listView.clipToPadding = false
        listView.setHasFixedSize(true)

        val scaleDetector = ScaleGestureDetector(
            activity,
            GestureScaleListener(this)
        )

        listView.setOnTouchListener { _, event ->

            when (event.pointerCount) {
                2 -> scaleDetector.onTouchEvent(event)
                else -> false
            }
        }
    }

    private fun setupActionMode() {
        actionModeCallback =
            ActionModeCallback(activity as ManagerActivityLollipop, actionModeViewModel, megaApi)

        observeItemLongClick()
        observeSelectedItems()
        observeAnimatedItems()
        observeActionModeDestroy()
    }

    private fun observeItemLongClick() =
        actionModeViewModel.longClick.observe(viewLifecycleOwner, EventObserver {
            if (currentZoom == ZoomUtil.ZOOM_DEFAULT || currentZoom == ZoomUtil.ZOOM_OUT_1X) {
                doIfOnline { actionModeViewModel.enterActionMode(it) }
                animateBottomView()
            }
        })

    private fun observeSelectedItems() =
        actionModeViewModel.selectedNodes.observe(viewLifecycleOwner, {
            if (it.isEmpty()) {
                actionMode?.apply {
                    finish()
                }
            } else {
                actionModeCallback.nodeCount = viewModel.getRealPhotoCount()

                if (actionMode == null) {
                    callManager { manager ->
                        //manager.hideKeyboardSearch()
                    }

                    actionMode = (activity as AppCompatActivity).startSupportActionMode(
                        actionModeCallback
                    )
                } else {
                    actionMode?.invalidate()  // Update the action items based on the selected nodes
                }

                actionMode?.title = it.size.toString()
            }
        })

    private fun observeAnimatedItems() {
        var animatorSet: AnimatorSet? = null

        actionModeViewModel.animNodeIndices.observe(viewLifecycleOwner, {
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
                    updateUi()
                }

                override fun onAnimationCancel(animation: Animator?) {
                }

                override fun onAnimationStart(animation: Animator?) {
                }
            })

            it.forEach { pos ->
                listView.findViewHolderForAdapterPosition(pos)?.let { viewHolder ->
                    val itemView = viewHolder.itemView

                    val imageView = if (viewModel.searchMode) {
                        itemView.setBackgroundColor(
                            ContextCompat.getColor(
                                context,
                                R.color.new_multiselect_color
                            )
                        )
                        itemView.findViewById(R.id.thumbnail)
                    } else {
                        // Draw the green outline for the thumbnail view at once
                        val thumbnailView =
                            itemView.findViewById<SimpleDraweeView>(R.id.thumbnail)
                        thumbnailView.hierarchy.roundingParams = getRoundingParams(context)

                        itemView.findViewById<ImageView>(
                            R.id.icon_selected
                        )
                    }

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
        })
    }

    private fun observeActionModeDestroy() =
        actionModeViewModel.actionModeDestroy.observe(viewLifecycleOwner, EventObserver {
            actionMode = null
            callManager { manager ->
                manager.showKeyboardForSearch()
            }
            animateBottomView()
        })

    private fun setupFastScroller() = binding.scroller.setRecyclerView(listView)

    /**
     * Get how many items will be shown per row, depends on screen direction and zoom level if all view is selected.
     *
     * @param isPortrait true, on portrait mode, false otherwise.
     */
    private fun getSpanCount(isPortrait: Boolean): Int {
        return if (selectedView != PhotosFragment.ALL_VIEW) {
            if (isPortrait) PhotosFragment.SPAN_CARD_PORTRAIT else PhotosFragment.SPAN_CARD_LANDSCAPE
        } else {
            ZoomUtil.getSpanCount(isPortrait, currentZoom)
        }
    }

    private fun handleZoomAdapterLayoutChange(zoom: Int) {
        val state = listView.layoutManager?.onSaveInstanceState()
        setupListAdapter(zoom)
        listView.layoutManager?.onRestoreInstanceState(state)
    }

    /**
     * Set recycle view and its inner layout depends on card view selected and zoom level.
     *
     * @param zoom Zoom level.
     */
    fun setupListAdapter(zoom: Int) {
        currentZoom = zoom

        val isPortrait =
            resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val spanCount = getSpanCount(isPortrait)
        val params = listView.layoutParams as CoordinatorLayout.LayoutParams
        gridLayoutManager = GridLayoutManager(context, spanCount)
        listView.switchBackToGrid()

        if (selectedView == PhotosFragment.ALL_VIEW) {
            if (!this::browseAdapter.isInitialized){
                browseAdapter =
                    ImagesBrowseAdapter(actionModeViewModel, itemOperationViewModel, currentZoom)
            }

            if (currentZoom == ZoomUtil.ZOOM_IN_1X) {
                params.rightMargin = 0
                params.leftMargin = 0
            } else {
                val margin = ZoomUtil.getMargin(context, currentZoom)
                params.leftMargin = margin
                params.rightMargin = margin
            }

            gridLayoutManager.apply {
                val imageMargin = ZoomUtil.getMargin(context, currentZoom)
                spanSizeLookup = browseAdapter.getSpanSizeLookup(spanCount)

                val itemDimen = if (currentZoom == ZoomUtil.ZOOM_IN_1X) {
                    outMetrics.widthPixels
                } else {
                    ((outMetrics.widthPixels - imageMargin * spanCount * 2) - imageMargin * 2) / spanCount
                }
                browseAdapter.setItemDimen(itemDimen)
            }

            browseAdapter.submitList(viewModel.items.value)
            listView.adapter = browseAdapter
        } else {
            val cardMargin =
                resources.getDimensionPixelSize(if (isPortrait) R.dimen.card_margin_portrait else R.dimen.card_margin_landscape)

            val cardWidth: Int =
                (outMetrics.widthPixels - cardMargin * spanCount * 2 - cardMargin * 2) / spanCount

            cardAdapter =
                CUCardViewAdapter(selectedView, cardWidth, cardMargin, cardClickedListener)
            cardAdapter.setHasStableIds(true)
            params.leftMargin = cardMargin
            params.rightMargin = cardMargin
            listView.adapter = cardAdapter

            listView.layoutParams = params
        }
    }

    /**
     * Whether should show zoom in/out menu items.
     * Depends on if selected view is all view.
     *
     * @return true, current view is all view should show the menu items, false, otherwise.
     */
    fun shouldShowZoomMenuItem() = selectedView == PhotosFragment.ALL_VIEW

    /**
     * Display the view type buttons panel with animation effect, after a card is clicked.
     */
    private fun showViewTypePanel() {
        val params = viewTypePanel.layoutParams as CoordinatorLayout.LayoutParams
        params.setMargins(
            0, 0, 0,
            resources.getDimensionPixelSize(R.dimen.cu_view_type_button_vertical_margin)
        )
        viewTypePanel.animate().translationY(0f).setDuration(175)
            .withStartAction { viewTypePanel.visibility = View.VISIBLE }
            .withEndAction { viewTypePanel.layoutParams = params }.start()
    }

    fun loadPhotos() = viewModel.loadPhotos(true)

    private fun openPhoto(nodeItem: PhotoNodeItem) {
        listView.findViewHolderForLayoutPosition(nodeItem.index)?.itemView?.findViewById<ImageView>(
            R.id.thumbnail
        )?.also {
            val intent = Intent(context, FullScreenImageViewerLollipop::class.java)

            intent.putExtra(Constants.INTENT_EXTRA_KEY_POSITION, nodeItem.photoIndex)
            intent.putExtra(
                Constants.INTENT_EXTRA_KEY_ORDER_GET_CHILDREN,
                MegaApiJava.ORDER_MODIFICATION_DESC
            )

            intent.putExtra(
                Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE,
                Constants.PHOTOS_BROWSE_ADAPTER
            )

            intent.putExtra(
                Constants.INTENT_EXTRA_KEY_HANDLE,
                nodeItem.node?.handle ?: MegaApiJava.INVALID_HANDLE
            )
            (listView.adapter as? DragThumbnailGetter)?.let {
                DragToExitSupport.putThumbnailLocation(
                    intent,
                    listView,
                    nodeItem.index,
                    Constants.VIEWER_FROM_PHOTOS,
                    it
                )
            }

            startActivity(intent)
            requireActivity().overridePendingTransition(0, 0)
        }
    }

    /**
     * Show the view with the data of years, months or days depends on selected view.
     *
     * @param dateCards
     *          The first element is the cards of days.
     *          The second element is the cards of months.
     *          The third element is the cards of years.
     */
    private fun showCards(dateCards: List<List<CUCard>?>?) {
        val index = when (selectedView) {
            PhotosFragment.DAYS_VIEW -> 0
            PhotosFragment.MONTHS_VIEW -> 1
            PhotosFragment.YEARS_VIEW -> 2
            else -> -1
        }

        if (index != -1) {
            cardAdapter.submitList(dateCards?.get(index))
        }

        updateFastScrollerVisibility()
    }

    /**
     * Set LinearLayoutManager for the list view.
     */
    private fun RecyclerView.switchToLinear() {
        linearLayoutManager = LinearLayoutManager(context)
        listView.layoutManager = linearLayoutManager
    }

    /**
     * Set GridLayoutManager for the list view.
     */
    private fun RecyclerView.switchBackToGrid() {
        linearLayoutManager = null
        layoutManager = gridLayoutManager
    }

    /**
     * Shows or hides the bottom view and animates the transition.
     */
    fun animateBottomView() {
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        viewModel.skipNextAutoScroll = true
    }

    override fun zoomIn() {
        viewModel.zoomManager.zoomIn()
        handleZoomMenuItemStatus()
    }

    override fun zoomOut() {
        viewModel.zoomManager.zoomOut()
        handleZoomMenuItemStatus()
    }

    override fun bindToolbar(): MaterialToolbar? = binding.layoutTitleBar.toolbar

    override fun bindTitle(): Int? = R.string.section_images

}