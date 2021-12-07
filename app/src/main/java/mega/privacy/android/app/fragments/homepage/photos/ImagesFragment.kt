package mega.privacy.android.app.fragments.homepage.photos

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ListenScrollChangesHelper
import mega.privacy.android.app.components.SimpleDividerItemDecoration
import mega.privacy.android.app.components.dragger.DragThumbnailGetter
import mega.privacy.android.app.components.dragger.DragToExitSupport
import mega.privacy.android.app.databinding.FragmentImagesBinding
import mega.privacy.android.app.fragments.homepage.*
import mega.privacy.android.app.fragments.managerFragments.cu.*
import mega.privacy.android.app.fragments.managerFragments.cu.PhotosFragment.*
import mega.privacy.android.app.gallery.adapter.GalleryAdapter
import mega.privacy.android.app.gallery.adapter.GalleryCardAdapter
import mega.privacy.android.app.gallery.data.GalleryCard
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.data.GalleryItemSizeConfig
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.modalbottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.ZoomUtil.DAYS_INDEX
import mega.privacy.android.app.utils.ZoomUtil.MONTHS_INDEX
import mega.privacy.android.app.utils.ZoomUtil.YEARS_INDEX
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiJava
import java.util.*

@AndroidEntryPoint
class ImagesFragment : BaseZoomFragment() {

    private val viewModel by viewModels<ImagesViewModel>()

    override lateinit var listView: RecyclerView
    private lateinit var browseAdapter: GalleryAdapter
    private lateinit var gridLayoutManager: GridLayoutManager
    private lateinit var cardAdapter: GalleryCardAdapter

    private lateinit var scaleGestureHandler: ScaleGestureHandler

    private lateinit var viewTypePanel: View
    private lateinit var yearsButton: TextView
    private lateinit var monthsButton: TextView
    private lateinit var daysButton: TextView
    private lateinit var allButton: TextView

    private lateinit var itemDecoration: SimpleDividerItemDecoration

    private var selectedView = ALL_VIEW
    private lateinit var binding: FragmentImagesBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding()
        initViewCreated()
        subscribeObservers()
    }

    private fun setupBinding() {
        binding.apply {
            viewModel = this@ImagesFragment.viewModel
            lifecycleOwner = viewLifecycleOwner
            viewTypePanel = photosViewType.root
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentImagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun initViewCreated() {
        val currentZoom = ZoomUtil.IMAGES_ZOOM_LEVEL
        zoomViewModel.setCurrentZoom(currentZoom)
        zoomViewModel.setZoom(currentZoom)
        viewModel.setZoom(currentZoom)
        setupEmptyHint()
        setupListView()
        setupTimePanel()
        setupListAdapter(currentZoom)
        setupActionMode()
        setupNavigation()
    }

    override fun handleZoomChange(zoom: Int, needReload: Boolean) {
        ZoomUtil.IMAGES_ZOOM_LEVEL = zoom
        handleZoomAdapterLayoutChange(zoom)
        if (needReload) {
            loadPhotos()
        }
    }

    override fun handleOnCreateOptionsMenu() {
        var hasImages = false
        if (this::browseAdapter.isInitialized) {
            hasImages = browseAdapter.itemCount > 0
        }
        handleOptionsMenuUpdate(hasImages && shouldShowZoomMenuItem())
        removeSortByMenu()
    }

    private fun subscribeObservers() {
        viewModel.items.observe(viewLifecycleOwner) {
            actionModeViewModel.setNodesData(it.filter { nodeItem -> nodeItem.type == GalleryItem.TYPE_IMAGE })
            if (it.isEmpty()) {
                handleOptionsMenuUpdate(false)
                viewTypePanel.visibility = View.GONE
            } else {
                handleOptionsMenuUpdate(shouldShowZoomMenuItem())
                viewTypePanel.visibility = View.VISIBLE
            }
            removeSortByMenu()
        }

        viewModel.dateCards.observe(viewLifecycleOwner, ::showCards)

        viewModel.refreshCards.observe(viewLifecycleOwner) {
            if (it && selectedView != ALL_VIEW) {
                showCards(viewModel.dateCards.value)
                viewModel.refreshCompleted()
            }
        }

        DragToExitSupport.observeDragSupportEvents(
            viewLifecycleOwner,
            listView,
            VIEWER_FROM_PHOTOS
        )
    }

    private val cardClickedListener = object : GalleryCardAdapter.Listener {

        override fun onCardClicked(position: Int, @NonNull card: GalleryCard) {
            when (selectedView) {
                DAYS_VIEW -> {
                    zoomViewModel.restoreDefaultZoom()
                    handleZoomMenuItemStatus()
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

    private fun setupEmptyHint() {
        binding.emptyHint.emptyHintImage.isVisible = false
        binding.emptyHint.emptyHintText.isVisible = false
        binding.emptyHint.emptyHintText.text =
            getString(R.string.homepage_empty_hint_photos).toUpperCase(Locale.ROOT)
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
        super.updateViewSelected(allButton, daysButton, monthsButton, yearsButton, selectedView)
    }

    private fun updateFastScrollerVisibility() {
        if (!this::cardAdapter.isInitialized)
            return
        super.updateFastScrollerVisibility(selectedView, binding.scroller, cardAdapter.itemCount)
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
        setupListAdapter(getCurrentZoom())

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
        removeSortByMenu()
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
                MegaChatApiJava.MEGACHAT_INVALID_HANDLE
            )
        }
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
     * Only refresh the list items of uiDirty = true
     */
    override fun updateUiWhenAnimationEnd() {
        viewModel.items.value?.let {
            val newList = ArrayList(it)
            browseAdapter.submitList(newList)
        }
    }

    private fun elevateToolbarWhenScrolling() = ListenScrollChangesHelper().addViewToListen(
        listView
    ) { v: View?, _, _, _, _ ->
        callManager {
            it.changeAppBarElevation(
                v!!.canScrollVertically(-1)
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListView() {
        selectedView = viewModel.selectedViewType
        listView = binding.photoList
        listView.itemAnimator = Util.noChangeRecyclerViewItemAnimator()
        elevateToolbarWhenScrolling()

        itemDecoration = SimpleDividerItemDecoration(context)

        listView.clipToPadding = false
        listView.setHasFixedSize(true)

        scaleGestureHandler = ScaleGestureHandler(
            context,
            this
        )
        listView.setOnTouchListener(scaleGestureHandler)
    }

    /**
     * Get how many items will be shown per row, depends on screen direction and zoom level if all view is selected.
     *
     * @param isPortrait true, on portrait mode, false otherwise.
     */
    private fun getSpanCount(isPortrait: Boolean): Int {
        return super.getSpanCount(selectedView, isPortrait)
    }

    private fun handleZoomAdapterLayoutChange(zoom: Int) {
        val state = listView.layoutManager?.onSaveInstanceState()
        setupListAdapter(zoom)
        viewModel.setZoom(zoom)
        listView.layoutManager?.onRestoreInstanceState(state)
    }

    /**
     * Set recycle view and its inner layout depends on card view selected and zoom level.
     *
     * @param currentZoom Zoom level.
     */
    fun setupListAdapter(currentZoom: Int) {
        val isPortrait =
            resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val spanCount = getSpanCount(isPortrait)
        val params = listView.layoutParams as CoordinatorLayout.LayoutParams
        gridLayoutManager = GridLayoutManager(context, spanCount)
        listView.layoutManager = gridLayoutManager

        if (selectedView == ALL_VIEW) {
            val imageMargin = ZoomUtil.getMargin(context, currentZoom)
            ZoomUtil.setMargin(context, params, currentZoom)
            val gridWidth = ZoomUtil.getItemWidth(context, outMetrics, currentZoom, spanCount)
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

            browseAdapter =
                GalleryAdapter(actionModeViewModel, itemOperationViewModel, itemSizeConfig)

            ZoomUtil.setMargin(context, params, currentZoom)

            gridLayoutManager.apply {
                spanSizeLookup = browseAdapter.getSpanSizeLookup(spanCount)
                val itemDimen = ZoomUtil.getItemWidth(context, outMetrics, currentZoom, spanCount)
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
                GalleryCardAdapter(selectedView, cardWidth, cardMargin, cardClickedListener)
            cardAdapter.setHasStableIds(true)
            params.leftMargin = cardMargin
            params.rightMargin = cardMargin
            listView.adapter = cardAdapter

            listView.layoutParams = params
        }

        // Set fast scroller after adapter is set.
        binding.scroller.setRecyclerView(listView)
    }

    /**
     * Whether should show zoom in/out menu items.
     * Depends on if selected view is all view.
     *
     * @return true, current view is all view should show the menu items, false, otherwise.
     */
    private fun shouldShowZoomMenuItem() = selectedView == ALL_VIEW

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

    private fun openPhoto(nodeItem: GalleryItem) {
        listView.findViewHolderForLayoutPosition(nodeItem.index)?.itemView?.findViewById<ImageView>(
            R.id.thumbnail
        )?.also {
            val intent = Intent(context, FullScreenImageViewerLollipop::class.java)

            intent.putExtra(INTENT_EXTRA_KEY_POSITION, nodeItem.indexForViewer)
            intent.putExtra(
                INTENT_EXTRA_KEY_ORDER_GET_CHILDREN,
                MegaApiJava.ORDER_MODIFICATION_DESC
            )

            intent.putExtra(
                INTENT_EXTRA_KEY_ADAPTER_TYPE,
                PHOTOS_BROWSE_ADAPTER
            )

            intent.putExtra(
                INTENT_EXTRA_KEY_HANDLE,
                nodeItem.node?.handle ?: MegaApiJava.INVALID_HANDLE
            )
            (listView.adapter as? DragThumbnailGetter)?.let {
                DragToExitSupport.putThumbnailLocation(
                    intent,
                    listView,
                    nodeItem.index,
                    VIEWER_FROM_PHOTOS,
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

    /**
     * Shows or hides the bottom view and animates the transition.
     */
    override fun animateBottomView() {
        val deltaY =
            viewTypePanel.height.toFloat() + resources.getDimensionPixelSize(R.dimen.cu_view_type_button_vertical_margin)

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

    override fun getNodeCount() = viewModel.getRealPhotoCount()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.selectedViewType = selectedView
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (activity as ManagerActivityLollipop? != null && (activity as ManagerActivityLollipop?)!!.drawerItem != ManagerActivityLollipop.DrawerItem.HOMEPAGE) {
            return
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (activity as ManagerActivityLollipop? != null && (activity as ManagerActivityLollipop?)!!.drawerItem != ManagerActivityLollipop.DrawerItem.HOMEPAGE) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}