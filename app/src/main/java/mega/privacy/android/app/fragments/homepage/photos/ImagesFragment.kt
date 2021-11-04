package mega.privacy.android.app.fragments.homepage.photos

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
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.components.GestureScaleListener
import mega.privacy.android.app.components.ListenScrollChangesHelper
import mega.privacy.android.app.components.SimpleDividerItemDecoration
import mega.privacy.android.app.components.dragger.DragThumbnailGetter
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.observeDragSupportEvents
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.putThumbnailLocation
import mega.privacy.android.app.databinding.FragmentImagesBinding
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.fragments.homepage.*
import mega.privacy.android.app.fragments.managerFragments.cu.*
import mega.privacy.android.app.fragments.managerFragments.cu.PhotosFragment.*
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.modalbottomsheet.NodeOptionsBottomSheetDialogFragment.MODE5
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.RunOnUIThreadUtils
import mega.privacy.android.app.utils.StyleUtils.setTextStyle
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.noChangeRecyclerViewItemAnimator
import mega.privacy.android.app.utils.ZoomUtil
import mega.privacy.android.app.utils.ZoomUtil.ZOOM_DEFAULT
import mega.privacy.android.app.utils.ZoomUtil.ZOOM_IN_1X
import mega.privacy.android.app.utils.ZoomUtil.ZOOM_OUT_1X
import mega.privacy.android.app.utils.ZoomUtil.getSpanCount
import mega.privacy.android.app.utils.callManager
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import java.util.*

@AndroidEntryPoint
class ImagesFragment : BaseFragment(), HomepageSearchable {

    private val viewModel by viewModels<ImagesViewModel>()
    private val actionModeViewModel by viewModels<ActionModeViewModel>()
    private val itemOperationViewModel by viewModels<ItemOperationViewModel>()

    private lateinit var binding: FragmentImagesBinding

    private lateinit var listView: RecyclerView

    private lateinit var viewTypePanel: View
    private lateinit var yearsButton: TextView
    private lateinit var monthsButton: TextView
    private lateinit var daysButton: TextView
    private lateinit var allButton: TextView

    private lateinit var browseAdapter: PhotosBrowseAdapter
    private lateinit var searchAdapter: PhotosSearchAdapter
    private lateinit var cardAdapter: CUCardViewAdapter

    private lateinit var gridLayoutManager: GridLayoutManager
    private var linearLayoutManager: LinearLayoutManager? = null

    private var actionMode: ActionMode? = null
    private lateinit var actionModeCallback: ActionModeCallback

    private lateinit var itemDecoration: SimpleDividerItemDecoration

    private var currentZoom = ZOOM_DEFAULT

    private var selectedView = ALL_VIEW

    private val cardClickedListener = object : CUCardViewAdapter.Listener {

        override fun onCardClicked(position: Int, @NonNull card: CUCard) {
            when (selectedView) {
                DAYS_VIEW -> {
                    callManager { it.restoreDefaultZoom() }
                    newViewClicked(ALL_VIEW)
                    val photoPosition = browseAdapter.getNodePosition(card.node.handle)
                    gridLayoutManager.scrollToPosition(photoPosition)

                    val node = browseAdapter.getNodeAtPosition(photoPosition)
                    node?.let {
                        RunOnUIThreadUtils.post {
                            openPhoto(it)
                        }
                    }
                }
                MONTHS_VIEW -> {
                    newViewClicked(DAYS_VIEW)
                    gridLayoutManager.scrollToPosition(viewModel.monthClicked(position, card))
                }
                YEARS_VIEW -> {
                    newViewClicked(MONTHS_VIEW)
                    gridLayoutManager.scrollToPosition(viewModel.yearClicked(position, card))
                }
            }

            showViewTypePanel()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        callManager {
            currentZoom = it.currentZoom
        }

        binding = FragmentImagesBinding.inflate(inflater, container, false).apply {
            viewModel = this@ImagesFragment.viewModel
        }

        viewTypePanel = binding.photosViewType.root

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner

        setupEmptyHint()
        setupListView()
        setupTimePanel()
        setupListAdapter(currentZoom)
        setupFastScroller()
        setupActionMode()
        setupNavigation()

        viewModel.items.observe(viewLifecycleOwner) {
            if (!viewModel.searchMode) {
                activity?.invalidateOptionsMenu()  // Hide the search icon if no photo
            }

            actionModeViewModel.setNodesData(it.filter { nodeItem -> nodeItem.type == PhotoNodeItem.TYPE_PHOTO })
        }

        viewModel.dateCards.observe(viewLifecycleOwner, ::showCards)

        viewModel.refreshCards.observe(viewLifecycleOwner) {
            if(it && selectedView != ALL_VIEW) {
                viewModel.refreshing()
                showCards(viewModel.dateCards.value)
            }
        }

        observeDragSupportEvents(viewLifecycleOwner, listView, VIEWER_FROM_PHOTOS)
    }

    private fun setupEmptyHint() {
        binding.emptyHint.emptyHintImage.isVisible = false
        binding.emptyHint.emptyHintText.isVisible = false
        binding.emptyHint.emptyHintText.text =
            getString(R.string.homepage_empty_hint_photos).toUpperCase(Locale.ROOT)
    }

    fun refreshSelf() {
        val ft = parentFragmentManager.beginTransaction()
        ft.detach(this)
        ft.attach(this)
        ft.commitNowAllowingStateLoss()
    }

    private fun setupTimePanel() {
        yearsButton = binding.photosViewType.yearsButton.apply {
            setOnClickListener {
                newViewClicked(YEARS_VIEW)
            }
        }
        monthsButton = binding.photosViewType.monthsButton.apply {
            setOnClickListener {
                newViewClicked(MONTHS_VIEW)
            }
        }
        daysButton = binding.photosViewType.daysButton.apply {
            setOnClickListener {
                newViewClicked(DAYS_VIEW)
            }
        }
        allButton = binding.photosViewType.allButton.apply {
            setOnClickListener {
                newViewClicked(ALL_VIEW)
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
            DAYS_VIEW -> setViewTypeButtonStyle(daysButton, true)
            MONTHS_VIEW -> setViewTypeButtonStyle(monthsButton, true)
            YEARS_VIEW -> setViewTypeButtonStyle(yearsButton, true)
            else -> setViewTypeButtonStyle(allButton, true)
        }

//        callManager {
//            it.updatePhotosFragmentOptionsMenu()
//        }
    }

    private fun updateFastScrollerVisibility() {
        val gridView = selectedView == ALL_VIEW

        binding.scroller.visibility =
            if (!gridView && cardAdapter.itemCount >= MIN_ITEMS_SCROLLBAR)
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

        setTextStyle(
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
            DAYS_VIEW, MONTHS_VIEW, YEARS_VIEW -> showCards(viewModel.dateCards.value)
            else -> {}
        }

        updateViewSelected()

        // If selected view is not all view, add layout param behaviour, so that button panel will go off when scroll.
        val params = viewTypePanel.layoutParams as CoordinatorLayout.LayoutParams
        params.behavior =
            if (selectedView != ALL_VIEW) CustomHideBottomViewOnScrollBehaviour<LinearLayout>() else null
        viewTypePanel.layoutParams = params
    }

    private fun doIfOnline(operation: () -> Unit) {
        if (Util.isOnline(context)) {
            operation()
        } else {
            val activity = activity as ManagerActivityLollipop

            activity.hideKeyboardSearch()  // Make the snack bar visible to the user
            activity.showSnackbar(
                SNACKBAR_TYPE,
                context.getString(R.string.error_server_connection_problem),
                MEGACHAT_INVALID_HANDLE
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
                        MODE5
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

        if (viewModel.searchMode) {
            searchAdapter.submitList(newList)
        } else {
            browseAdapter.submitList(newList)
        }
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
        listView.itemAnimator = noChangeRecyclerViewItemAnimator()
        elevateToolbarWhenScrolling()

        itemDecoration = SimpleDividerItemDecoration(context)
        if (viewModel.searchMode) listView.addItemDecoration(itemDecoration)

        listView.clipToPadding = false
        listView.setHasFixedSize(true)

        val scaleDetector = ScaleGestureDetector(activity, GestureScaleListener(activity as? GestureScaleListener.GestureScaleCallback))
        callManager {

        }

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
            if (currentZoom == ZOOM_DEFAULT || currentZoom == ZOOM_OUT_1X) {
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
        return if (selectedView != ALL_VIEW) {
            if (isPortrait) SPAN_CARD_PORTRAIT else SPAN_CARD_LANDSCAPE
        } else {
            getSpanCount(isPortrait, currentZoom)
        }
    }

    /**
     * Set recycle view and its inner layout depends on card view selected and zoom level.
     *
     * @param zoom Zoom level.
     */
    fun setupListAdapter(zoom: Int) {
        currentZoom = zoom
        searchAdapter = PhotosSearchAdapter(actionModeViewModel, itemOperationViewModel)

        searchAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (!viewModel.skipNextAutoScroll) {
                    listView.layoutManager?.scrollToPosition(0)
                }
                viewModel.skipNextAutoScroll = false
            }
        })

        if (viewModel.searchMode) {
            listView.switchToLinear()
            listView.adapter = searchAdapter
        } else {
            val isPortrait =
                resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            val spanCount = getSpanCount(isPortrait)
            val params = listView.layoutParams as CoordinatorLayout.LayoutParams
            gridLayoutManager = GridLayoutManager(context, spanCount)
            listView.switchBackToGrid()

            if (selectedView == ALL_VIEW) {
                browseAdapter =
                    PhotosBrowseAdapter(actionModeViewModel, itemOperationViewModel, currentZoom)

                if (currentZoom == ZOOM_IN_1X) {
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

                    val itemDimen = if (currentZoom == ZOOM_IN_1X) {
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
            }

            listView.layoutParams = params
        }
    }

    // As designer required, images section no longer has search option.
    override fun shouldShowSearchMenu() = false

    override fun searchReady() {
        // Rotate screen in action mode, the keyboard would pop up again, hide it
        if (actionMode != null) {
            RunOnUIThreadUtils.post {
                callManager {
                    it.hideKeyboardSearch()
                }
            }
        }

        if (viewModel.searchMode) return

        listView.switchToLinear()
        listView.adapter = searchAdapter
        listView.addItemDecoration(itemDecoration)

        viewModel.searchMode = true
        viewModel.searchQuery = ""
        viewModel.refreshUi()
    }

    override fun exitSearch() {
        if (!viewModel.searchMode) return

        listView.switchBackToGrid()
        configureGridLayoutManager()
        listView.adapter = browseAdapter
        listView.removeItemDecoration(itemDecoration)

        viewModel.searchMode = false
        viewModel.searchQuery = ""
        viewModel.refreshUi()
    }

    private fun configureGridLayoutManager() {
        val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val spanCount = getSpanCount(isPortrait)
        gridLayoutManager = GridLayoutManager(context, spanCount)
        listView.switchBackToGrid()

        gridLayoutManager.apply {
            val imageMargin = ZoomUtil.getMargin(context, currentZoom)
            spanSizeLookup = browseAdapter.getSpanSizeLookup(spanCount)
            val itemDimen = if (currentZoom == ZOOM_IN_1X) {
                outMetrics.widthPixels
            } else {
                ((outMetrics.widthPixels - imageMargin * spanCount * 2) - imageMargin * 2) / spanCount
            }
            browseAdapter.setItemDimen(itemDimen)
        }
    }

    /**
     * Whether should show zoom in/out menu items.
     * Depends on if selected view is all view.
     *
     * @return true, current view is all view should show the menu items, false, otherwise.
     */
    fun shouldShowZoomMenuItem() = selectedView == ALL_VIEW

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
            .withStartAction{ viewTypePanel.visibility = View.VISIBLE }
            .withEndAction{ viewTypePanel.layoutParams = params }.start()
    }

    override fun searchQuery(query: String) {
        if (viewModel.searchQuery == query) return

        viewModel.searchQuery = query
        viewModel.loadPhotos()
    }

    fun loadPhotos() = viewModel.loadPhotos(true)

    private fun openPhoto(nodeItem: PhotoNodeItem) {
        listView.findViewHolderForLayoutPosition(nodeItem.index)?.itemView?.findViewById<ImageView>(
            R.id.thumbnail
        )?.also {
            val intent = Intent(context, FullScreenImageViewerLollipop::class.java)

            intent.putExtra(INTENT_EXTRA_KEY_POSITION, nodeItem.photoIndex)
            intent.putExtra(
                INTENT_EXTRA_KEY_ORDER_GET_CHILDREN,
                MegaApiJava.ORDER_MODIFICATION_DESC
            )

            if (viewModel.searchMode) {
                intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, PHOTOS_SEARCH_ADAPTER)
                intent.putExtra(
                    INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH,
                    viewModel.getHandlesOfPhotos()
                )
            } else {
                intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, PHOTOS_BROWSE_ADAPTER)
            }

            intent.putExtra(INTENT_EXTRA_KEY_HANDLE, nodeItem.node?.handle ?: INVALID_HANDLE)
            (listView.adapter as? DragThumbnailGetter)?.let {
                putThumbnailLocation(intent, listView, nodeItem.index, VIEWER_FROM_PHOTOS, it)
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
        val index = when(selectedView) {
            DAYS_VIEW -> 0
            MONTHS_VIEW -> 1
            YEARS_VIEW -> 2
            else -> -1
        }

        if(index != -1) {
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
        val deltaY = viewTypePanel.height.toFloat() + resources.getDimensionPixelSize(R.dimen.cu_view_type_button_vertical_margin)

        if (viewTypePanel.isVisible) {
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        viewModel.skipNextAutoScroll = true
    }
}
