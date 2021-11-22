package mega.privacy.android.app.fragments.homepage.photos

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
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
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.modalbottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.Constants.*
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiJava
import java.util.*

@AndroidEntryPoint
class ImagesFragment : BaseZoomFragment(){

    private val viewModel by viewModels<ImagesViewModel>()
    private val actionModeViewModel by viewModels<ActionModeViewModel>()
    private val itemOperationViewModel by viewModels<ItemOperationViewModel>()

    private lateinit var listView: RecyclerView
    private lateinit var browseAdapter: PhotosBrowseAdapter
    private lateinit var gridLayoutManager: GridLayoutManager
    private lateinit var cardAdapter: CUCardViewAdapter

    private lateinit var scaleGestureHandler: ScaleGestureHandler

    private lateinit var viewTypePanel: View
    private lateinit var yearsButton: TextView
    private lateinit var monthsButton: TextView
    private lateinit var daysButton: TextView
    private lateinit var allButton: TextView

    private var actionMode: ActionMode? = null
    private lateinit var actionModeCallback: ActionModeCallback

    private lateinit var itemDecoration: SimpleDividerItemDecoration

    private var selectedView = ALL_VIEW
    private lateinit var binding:FragmentImagesBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewCreated()
        subscribeObservers()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentImagesBinding.inflate(inflater, container, false).apply {
            viewModel = this@ImagesFragment.viewModel
        }
        binding.lifecycleOwner = viewLifecycleOwner
        viewTypePanel = binding.photosViewType.root
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

    override fun handleZoomChange(zoom: Int,needReload:Boolean) {
        ZoomUtil.IMAGES_ZOOM_LEVEL = zoom
        handleZoomAdapterLayoutChange(zoom)
        if (needReload) {
            loadPhotos()
        }
    }

    override fun handleOnCreateOptionsMenu() {
        handleOptionsMenuUpdate(shouldShowZoomMenuItem())
        removeSortByMenu()
    }

    private fun subscribeObservers() {
        viewModel.items.observe(viewLifecycleOwner) {
            actionModeViewModel.setNodesData(it.filter { nodeItem -> nodeItem.type == PhotoNodeItem.TYPE_PHOTO })
            if (it.isEmpty()) {
                handleOptionsMenuUpdate(false)
                viewTypePanel.visibility = View.GONE
            } else {
                handleOptionsMenuUpdate(true)
                viewTypePanel.visibility = View.VISIBLE
            }
            removeSortByMenu()
        }

        viewModel.dateCards.observe(viewLifecycleOwner, ::showCards)

        viewModel.refreshCards.observe(viewLifecycleOwner) {
            if (it && selectedView != ALL_VIEW) {
                viewModel.refreshing()
                showCards(viewModel.dateCards.value)
            }
        }

        DragToExitSupport.observeDragSupportEvents(
            viewLifecycleOwner,
            listView,
            VIEWER_FROM_PHOTOS
        )
    }

    private val cardClickedListener = object : CUCardViewAdapter.Listener {

        override fun onCardClicked(position: Int, @NonNull card: CUCard) {
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
       super.updateViewSelected(allButton,daysButton,monthsButton,yearsButton,selectedView)
    }

    private fun updateFastScrollerVisibility() {
        if (!this::cardAdapter.isInitialized)
            return
       super.updateFastScrollerVisibility(selectedView,binding.scroller,cardAdapter.itemCount)
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
            if (zoomViewModel.getCurrentZoom() == ZoomUtil.ZOOM_DEFAULT || zoomViewModel.getCurrentZoom() == ZoomUtil.ZOOM_OUT_1X) {
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

    /**
     * Get how many items will be shown per row, depends on screen direction and zoom level if all view is selected.
     *
     * @param isPortrait true, on portrait mode, false otherwise.
     */
    private fun getSpanCount(isPortrait: Boolean): Int {
        return super.getSpanCount(selectedView,isPortrait)
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
            browseAdapter =
                PhotosBrowseAdapter(actionModeViewModel, itemOperationViewModel, currentZoom)

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
                CUCardViewAdapter(selectedView, cardWidth, cardMargin, cardClickedListener)
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
    private fun showCards(dateCards: List<List<CUCard>?>?) {
        val index = when (selectedView) {
            DAYS_VIEW -> 0
            MONTHS_VIEW -> 1
            YEARS_VIEW -> 2
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
    fun animateBottomView() {
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