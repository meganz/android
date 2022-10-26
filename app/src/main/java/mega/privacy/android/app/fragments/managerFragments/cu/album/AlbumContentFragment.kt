package mega.privacy.android.app.fragments.managerFragments.cu.album

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
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
import mega.privacy.android.app.databinding.FragmentAlbumContentBinding
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.fragments.homepage.ActionModeCallback
import mega.privacy.android.app.fragments.homepage.ActionModeViewModel
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.ItemOperationViewModel
import mega.privacy.android.app.fragments.homepage.getRoundingParams
import mega.privacy.android.app.fragments.homepage.photos.ScaleGestureHandler
import mega.privacy.android.app.fragments.homepage.photos.ZoomViewModel
import mega.privacy.android.app.gallery.adapter.GalleryAdapter
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.data.GalleryItemSizeConfig
import mega.privacy.android.app.gallery.data.MediaCardType
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.modalbottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.ALBUM_CONTENT_ADAPTER
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil.formatEmptyScreenText
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.ZoomUtil
import mega.privacy.android.app.utils.callManager
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.domain.entity.SortOrder
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiJava
import javax.inject.Inject

/**
 * AlbumContentFragment is using to show album content when click album cover.
 */
@AndroidEntryPoint
class AlbumContentFragment : Fragment(), GestureScaleListener.GestureScaleCallback {

    @Inject
    @MegaApi
    lateinit var megaApi: MegaApiAndroid

    /**
     * Sort order Int Mapper
     */
    @Inject
    lateinit var sortOrderIntMapper: SortOrderIntMapper

    private lateinit var mManagerActivity: ManagerActivity

    // List view
    private lateinit var listView: RecyclerView
    private lateinit var gridAdapter: GalleryAdapter
    private lateinit var layoutManager: GridLayoutManager
    private lateinit var scroller: FastScroller
    private lateinit var scaleGestureHandler: ScaleGestureHandler

    private lateinit var menu: Menu

    // Action mode
    private var actionMode: ActionMode? = null
    private lateinit var actionModeCallback: ActionModeCallback

    // View model
    private val zoomViewModel by viewModels<ZoomViewModel>()
    private val actionModeViewModel by viewModels<ActionModeViewModel>()
    private val itemOperationViewModel by viewModels<ItemOperationViewModel>()

    private var showBottomNav = true

    private var adapterType = 0

    val viewModel by viewModels<AlbumContentViewModel>()

    private lateinit var binding: FragmentAlbumContentBinding

    /**
     * Current order.
     */
    private var order = SortOrder.ORDER_NONE

    companion object {
        @JvmStatic
        fun getInstance(): AlbumContentFragment {
            return AlbumContentFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mManagerActivity = activity as ManagerActivity
        order = viewModel.getOrder()
        showBottomNav = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAlbumContentBinding.inflate(inflater, container, false)
        adapterType = ALBUM_CONTENT_ADAPTER
        setupBinding()
        setupParentActivityUI()
        return binding.root
    }

    /**
     * Setup ManagerActivity UI
     */
    private fun setupParentActivityUI() {
        mManagerActivity.setToolbarTitle()
        mManagerActivity.invalidateOptionsMenu()
        mManagerActivity.hideFabButton()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeBaseObservers()
        setHasOptionsMenu(true)
        initViewCreated()
        subscribeObservers()
    }

    /**
     * Setup UI Binding
     */
    private fun setupBinding() {
        binding.apply {
            viewModel = this@AlbumContentFragment.viewModel
            lifecycleOwner = viewLifecycleOwner
        }

        listView = binding.photoList
        scroller = binding.scroller
    }

    /**
     * Handle init logic when view created
     */
    private fun initViewCreated() {
        val currentZoom = ZoomUtil.ALBUM_ZOOM_LEVEL
        zoomViewModel.setCurrentZoom(currentZoom)
        zoomViewModel.setZoom(currentZoom)
        viewModel.mZoom = currentZoom
        setupEmptyHint()
        setupListView()
        setupListAdapter(currentZoom, viewModel.items.value)
    }

    fun handleZoomChange(zoom: Int, needReload: Boolean) {
        ZoomUtil.ALBUM_ZOOM_LEVEL = zoom
        handleZoomAdapterLayoutChange(zoom)
        if (needReload) {
            loadPhotos()
        }
    }

    fun handleOnCreateOptionsMenu() {
        val hasImages = gridAdapterHasData()
        handleOptionsMenuUpdate(hasImages)
    }

    /**
     * Subscribe Observers
     */
    private fun subscribeObservers() {
        viewModel.items.observe(viewLifecycleOwner) {
            // Order changed.
            if (order != viewModel.getOrder()) {
                setupListAdapter(getCurrentZoom(), it)
                order = viewModel.getOrder()
            }

            actionModeViewModel.setNodesData(it.filter { nodeItem -> nodeItem.type != MediaCardType.Header })
            handleOptionsMenuUpdate(it.isNotEmpty())
        }
    }

    /**
     * Setup empty UI status like text, image
     */
    private fun setupEmptyHint() {
        binding.emptyHint.emptyHintImage.isVisible = false
        binding.emptyHint.emptyHintText.isVisible = false
        binding.emptyHint.emptyHintImage.setImageResource(R.drawable.ic_zero_data_favourites)

        ColorUtils.setImageViewAlphaIfDark(
            requireContext(),
            binding.emptyHint.emptyHintImage,
            ColorUtils.DARK_IMAGE_ALPHA
        )
        binding.emptyHint.emptyHintText.text = formatEmptyScreenText(
            context,
            StringResourcesUtils.getString(R.string.empty_hint_favourite_album)
        )
    }

    /**
     * Handle logic when zoom adapter layout change
     */
    private fun handleZoomAdapterLayoutChange(zoom: Int) {
        val state = listView.layoutManager?.onSaveInstanceState()
        setupListAdapter(zoom, viewModel.items.value)
        viewModel.mZoom = zoom
        listView.layoutManager?.onRestoreInstanceState(state)
    }

    /**
     * Load photos
     */
    fun loadPhotos() {
        if (isAdded) viewModel.loadPhotos(true)
    }

    fun getOrder() = viewModel.getOrder()

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (!isInThisPage()) {
            return
        }
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_images_toolbar, menu)
        this.menu = menu
        handleOnCreateOptionsMenu()
        handleZoomMenuItemStatus()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (!isInThisPage()) {
            true
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
            return super.onOptionsItemSelected(item)
        }
    }

    /**
     * Check if in AlbumContentFragment
     *
     * @return true, in AlbumContentFragment; false, not in AlbumContentFragment
     */
    private fun isInThisPage(): Boolean {
        return mManagerActivity.isInAlbumContentPage
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

    private fun subscribeBaseObservers() {
        zoomViewModel.zoom.observe(viewLifecycleOwner) { zoom: Int ->
            val needReload = ZoomUtil.needReload(getCurrentZoom(), zoom)
            zoomViewModel.setCurrentZoom(zoom)
            handleZoomChange(zoom, needReload)
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
        val spanCount = getSpanCount(isPortrait)

        val params = listView.layoutParams as ViewGroup.MarginLayoutParams

        layoutManager = GridLayoutManager(context, spanCount)
        listView.layoutManager = layoutManager

        val imageMargin = ZoomUtil.getMargin(requireContext(), currentZoom)
        ZoomUtil.setMargin(requireContext(), params, currentZoom)
        val gridWidth =
            ZoomUtil.getItemWidth(requireContext(), resources.displayMetrics, currentZoom, spanCount, isPortrait)
        val icSelectedWidth = ZoomUtil.getSelectedFrameWidth(requireContext(), currentZoom)
        val icSelectedMargin = ZoomUtil.getSelectedFrameMargin(requireContext(), currentZoom)
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
                ZoomUtil.getItemWidth(requireContext(), resources.displayMetrics, currentZoom, spanCount, isPortrait)
            gridAdapter.setItemDimen(itemDimen)
        }

        gridAdapter.submitList(data)
        listView.adapter = gridAdapter

        // Set fast scroller after adapter is set.
        scroller.setRecyclerView(listView)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListView() {
        scaleGestureHandler = ScaleGestureHandler(
            requireContext(),
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

    private fun openPhoto(nodeItem: GalleryItem) {
        listView.findViewHolderForLayoutPosition(nodeItem.index)
            ?.itemView?.findViewById<ImageView>(R.id.thumbnail)?.also {
                val parentNodeHandle = nodeItem.node?.parentHandle ?: return
                val nodeHandle = nodeItem.node?.handle ?: MegaApiJava.INVALID_HANDLE
                val childrenNodes = viewModel.getItemsHandle()
                val intent = when (adapterType) {
                    ALBUM_CONTENT_ADAPTER, Constants.PHOTOS_BROWSE_ADAPTER -> {
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
                            sortOrderIntMapper(getOrder()),
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
                    thumbnailView.hierarchy.roundingParams = getRoundingParams(requireContext())

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
                Constants.SNACKBAR_TYPE,
                StringResourcesUtils.getString(R.string.error_server_connection_problem),
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
        var colorRes = ColorUtils.getThemeColor(requireContext(), R.attr.colorControlNormal)
        if (!isEnable) {
            colorRes = ContextCompat.getColor(requireContext(), R.color.grey_038_white_038)
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
            menu.findItem(R.id.action_menu_sort_by)?.isVisible = shouldShow
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
     * Get how many items will be shown per row, depends on screen direction and zoom level if all view is selected.
     *
     * @param isPortrait true, on portrait mode, false otherwise.
     */
    private fun getSpanCount(isPortrait: Boolean): Int {
        return ZoomUtil.getSpanCount(isPortrait, zoomViewModel.getCurrentZoom())

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
}