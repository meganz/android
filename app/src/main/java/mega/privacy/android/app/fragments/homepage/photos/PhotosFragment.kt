package mega.privacy.android.app.fragments.homepage.photos

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.components.GridScaleGestureDetector
import mega.privacy.android.app.components.ListenScrollChangesHelper
import mega.privacy.android.app.components.SimpleDividerItemDecoration
import mega.privacy.android.app.components.dragger.DragThumbnailGetter
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.observeDragSupportEvents
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.putThumbnailLocation
import mega.privacy.android.app.databinding.FragmentPhotosBinding
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.fragments.homepage.*
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.modalbottomsheet.NodeOptionsBottomSheetDialogFragment.MODE5
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.RunOnUIThreadUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.noChangeRecyclerViewItemAnimator
import mega.privacy.android.app.utils.ZoomUtil
import mega.privacy.android.app.utils.ZoomUtil.ZOOM_DEFAULT
import mega.privacy.android.app.utils.ZoomUtil.ZOOM_IN_1X
import mega.privacy.android.app.utils.ZoomUtil.ZOOM_OUT_1X
import mega.privacy.android.app.utils.ZoomUtil.getSpanCount
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import java.util.*

@AndroidEntryPoint
class PhotosFragment : BaseFragment(), HomepageSearchable {

    private val viewModel by viewModels<PhotosViewModel>()
    private val actionModeViewModel by viewModels<ActionModeViewModel>()
    private val itemOperationViewModel by viewModels<ItemOperationViewModel>()

    private lateinit var binding: FragmentPhotosBinding

    private lateinit var listView: RecyclerView

    private lateinit var browseAdapter: PhotosBrowseAdapter
    private lateinit var searchAdapter: PhotosSearchAdapter

    private lateinit var gridLayoutManager: GridLayoutManager
    private var linearLayoutManager: LinearLayoutManager? = null

    private var actionMode: ActionMode? = null
    private lateinit var actionModeCallback: ActionModeCallback

    private lateinit var itemDecoration: SimpleDividerItemDecoration

    private var currentZoom = ZOOM_DEFAULT

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        currentZoom = (activity as ManagerActivityLollipop).currentZoom

        binding = FragmentPhotosBinding.inflate(inflater, container, false).apply {
            viewModel = this@PhotosFragment.viewModel
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner

        setupEmptyHint()
        setupListView()
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
                (activity as ManagerActivityLollipop).showNodeOptionsPanel(
                    it.node,
                    MODE5
                )
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
        (activity as ManagerActivityLollipop).changeAppBarElevation(v!!.canScrollVertically(-1))
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


        val scaleDetector = GridScaleGestureDetector(activity as ManagerActivityLollipop)
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
            if(currentZoom == ZOOM_DEFAULT || currentZoom == ZOOM_OUT_1X) {
                doIfOnline { actionModeViewModel.enterActionMode(it) }
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
                    (activity as ManagerActivityLollipop).hideKeyboardSearch()
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
            (activity as ManagerActivityLollipop).showKeyboardForSearch()
        })

    private fun setupFastScroller() = binding.scroller.setRecyclerView(listView)

    fun setupListAdapter(zoom: Int) {
        currentZoom = zoom
        browseAdapter = PhotosBrowseAdapter(actionModeViewModel, itemOperationViewModel, currentZoom)
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
            val params = listView.layoutParams as ConstraintLayout.LayoutParams

            if (currentZoom == ZOOM_IN_1X) {
                params.rightMargin = 0
                params.leftMargin = 0
            }else{
                val margin = ZoomUtil.getMargin(context, currentZoom)
                params.leftMargin = margin
                params.rightMargin = margin
            }

            configureGridLayoutManager()

            listView.layoutParams = params
            listView.adapter = browseAdapter
        }
    }

    override fun shouldShowSearchMenu(): Boolean = viewModel.shouldShowSearchMenu()

    override fun searchReady() {
        // Rotate screen in action mode, the keyboard would pop up again, hide it
        if (actionMode != null) {
            RunOnUIThreadUtils.post { (activity as ManagerActivityLollipop).hideKeyboardSearch() }
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
                intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, PHOTOS_SEARCH_ADAPTER);
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

    private fun getSpanCount(isPortrait: Boolean) = getSpanCount(isPortrait, currentZoom)

    private fun RecyclerView.switchToLinear() {
        linearLayoutManager = LinearLayoutManager(context)
        listView.layoutManager = linearLayoutManager
    }

    private fun RecyclerView.switchBackToGrid() {
        linearLayoutManager = null
        listView.layoutManager = gridLayoutManager
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        viewModel.skipNextAutoScroll = true
    }
}
