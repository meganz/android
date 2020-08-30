package mega.privacy.android.app.fragments.photos

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Intent
import android.drm.DrmStore
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.ShapeAppearanceModel
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.components.CustomizedGridLayoutManager
import mega.privacy.android.app.components.ListenScrollChangesHelper
import mega.privacy.android.app.components.NewGridRecyclerView
import mega.privacy.android.app.databinding.FragmentPhotosBinding
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaNode
import java.lang.ref.WeakReference
import javax.inject.Inject

@AndroidEntryPoint
class PhotosFragment : BaseFragment(), HomepageSearchable, HomepageRefreshable {

    private val viewModel by viewModels<PhotosViewModel>()
    private val actionModeViewModel by viewModels<ActionModeViewModel>()

    private lateinit var binding: FragmentPhotosBinding

    private lateinit var listView: NewGridRecyclerView

    private lateinit var browseAdapter: PhotosBrowseAdapter
    private lateinit var searchAdapter: PhotosSearchAdapter

    private var actionMode: ActionMode? = null
    private lateinit var actionModeCallback: ActionModeCallback

    private lateinit var activity: ManagerActivityLollipop

    private var draggingPhotoHandle = INVALID_HANDLE

    private lateinit var itemDecoration: DividerDecoration

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPhotosBinding.inflate(inflater, container, false).apply {
            viewModel = this@PhotosFragment.viewModel
            actionModeViewModel = this@PhotosFragment.actionModeViewModel
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        activity = getActivity() as ManagerActivityLollipop

        setupListView()
        setupListAdapter()
        setupFastScroller()
        setupActionMode()
        setupNavigation()

        viewModel.items.observe(viewLifecycleOwner) {
            if (!viewModel.searchMode) {
                activity.invalidateOptionsMenu()  // Hide the search icon if no photo
            }

            actionModeViewModel.setNodesData(it.filter { node -> node.type == PhotoNode.TYPE_PHOTO })
        }

        forceUpdate()
    }

    private fun doIfOnline(operation: () -> Unit) {
        if (Util.isOnline(context)) {
            operation()
        } else {
            activity.hideKeyboardSearch()  // Make the snack bar visible to the user
            activity.showSnackbar(
                SNACKBAR_TYPE,
                context.getString(R.string.error_server_connection_problem),
                -1
            )
        }
    }

    private fun setupNavigation() {
        viewModel.openPhotoEvent.observe(viewLifecycleOwner, EventObserver {
            openPhoto(it)
        })

        viewModel.showFileInfoEvent.observe(viewLifecycleOwner, EventObserver {
            doIfOnline { activity.showNodeOptionsPanel(it.node) }
        })
    }

    override fun refreshUi() {
        // Callbacks of nodes change may provide the specific changed nodes, here
        // refresh all items for simplicity
        viewModel.items.value?.forEach {
            it.uiDirty = true
        }

        updateUi()
    }

    override fun forceUpdate() {
        viewModel.loadPhotos(viewModel.searchQuery, true)
    }

    /**
     * Only refresh the list items of uiDirty = true
     */
    private fun updateUi() = viewModel.items.value?.let { it ->
        val newList = ArrayList<PhotoNode>(it)
        if (viewModel.searchMode) {
            searchAdapter.submitList(newList)
        } else {
            browseAdapter.submitList(newList)
        }
    }

    private fun preventListItemBlink() {
        val animator = listView.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }

    private fun elevateToolbarWhenScrolling() = ListenScrollChangesHelper().addViewToListen(
        listView
    ) { v: View?, _, _, _, _ ->
        activity.changeActionBarElevation(v!!.canScrollVertically(-1))
    }

    private fun setupListView() {
        listView = binding.photoList
        preventListItemBlink()
        elevateToolbarWhenScrolling()
        itemDecoration = DividerDecoration(context)
    }

    private fun setupActionMode() {
        actionModeCallback = ActionModeCallback(context, actionModeViewModel, megaApi)

        observePhotoLongClick()
        observeSelectedPhotos()
        observeAnimatedPhotos()
        observeActionModeDestroy()
    }

    private fun observePhotoLongClick() =
        actionModeViewModel.longClick.observe(viewLifecycleOwner, EventObserver {
            doIfOnline { actionModeViewModel.enterActionMode(it) }
        })

    private fun observeSelectedPhotos() =
        actionModeViewModel.selectedNodes.observe(viewLifecycleOwner, Observer {
            if (it.isEmpty()) {
                actionMode?.apply {
                    finish()
                }
            } else {
                actionModeCallback.nodeCount = viewModel.getRealNodeCount()

                if (actionMode == null) {
                    activity.hideKeyboardSearch()
                    actionMode = (activity as AppCompatActivity).startSupportActionMode(
                        actionModeCallback
                    )
                } else {
                    actionMode?.invalidate()  // Update the action items based on the selected nodes
                }

                actionMode?.title = it.size.toString()
            }
        })

    private fun observeAnimatedPhotos() {
        var animatorSet: AnimatorSet? = null

        actionModeViewModel.animNodeIndices.observe(viewLifecycleOwner, Observer {
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
                        itemView.setBackgroundColor(resources.getColor(R.color.new_multiselect_color))
                        itemView.findViewById(R.id.thumbnail)
                    } else {
                        // Draw the green outline for the thumbnail view at once
                        val thumbnailView =
                            itemView.findViewById<ShapeableImageView>(R.id.thumbnail)
                        val strokeWidth =
                            resources.getDimension(R.dimen.photo_selected_border_width)
                        val shapeId = R.style.GalleryImageShape_Selected
                        thumbnailView.strokeWidth = strokeWidth
                        thumbnailView.shapeAppearanceModel = ShapeAppearanceModel.builder(
                            context, shapeId, 0
                        ).build()

                        itemView.findViewById<ImageView>(
                            R.id.icon_selected
                        )
                    }

                    imageView?.run {
                        setImageResource(R.drawable.ic_select_folder)
                        visibility = View.VISIBLE

                        val animator =
                            AnimatorInflater.loadAnimator(context, R.animator.photo_select)
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
            activity.showKeyboardForSearch()
        })

    private fun setupFastScroller() = binding.scroller.setRecyclerView(listView)

    private fun setupListAdapter() {
        browseAdapter = PhotosBrowseAdapter(viewModel, actionModeViewModel)
        searchAdapter = PhotosSearchAdapter(viewModel, actionModeViewModel)

        searchAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                listView.linearLayoutManager?.scrollToPosition(0)
            }
        })

        if (viewModel.searchMode) {
            listView.switchToLinear()
            listView.adapter = searchAdapter
        } else {
            configureGridLayoutManager()
            listView.adapter = browseAdapter
        }
    }

    override fun shouldShowSearchMenu(): Boolean = viewModel.shouldShowSearchMenu()

    override fun searchReady() {
        // Rotate screen in action mode, the keyboard would pop up again, hide it
        if (actionMode != null) {
            Handler().post { activity.hideKeyboardSearch() }
        }
        if (viewModel.searchMode) return

        viewModel.searchMode = true
        listView.switchToLinear()
        listView.adapter = searchAdapter
        listView.addItemDecoration(itemDecoration)

        viewModel.loadPhotos("")
    }

    override fun exitSearch() {
        if (!viewModel.searchMode) return

        viewModel.searchMode = false
        listView.switchBackToGrid()
        configureGridLayoutManager()
        listView.adapter = browseAdapter
        listView.removeItemDecoration(itemDecoration)

        viewModel.loadPhotos("")
    }

    private fun configureGridLayoutManager() {
        if (listView.layoutManager !is CustomizedGridLayoutManager) return

        (listView.layoutManager as CustomizedGridLayoutManager).apply {
            spanSizeLookup = browseAdapter.getSpanSizeLookup(spanCount)
            val itemDimen =
                outMetrics.widthPixels / spanCount - resources.getDimension(R.dimen.photo_grid_margin)
                    .toInt() * 2
            browseAdapter.setItemDimen(itemDimen)
        }
    }

    override fun searchQuery(query: String) {
        if (viewModel.searchQuery == query) return
        viewModel.loadPhotos(query)
    }

    private fun openPhoto(node: PhotoNode) {
        listView.findViewHolderForLayoutPosition(node.index)?.itemView?.findViewById<ImageView>(
            R.id.thumbnail
        )?.also {
            val intent = Intent(context, FullScreenImageViewerLollipop::class.java)

            intent.putExtra(INTENT_EXTRA_KEY_POSITION, node.photoIndex)
            intent.putExtra(
                INTENT_EXTRA_KEY_ORDER_GET_CHILDREN,
                MegaApiJava.ORDER_MODIFICATION_DESC
            )

            val parentNode = megaApi.getParentNode(node.node)
            if (parentNode == null || parentNode.type == MegaNode.TYPE_ROOT) {
                intent.putExtra(INTENT_EXTRA_KEY_PARENT_HANDLE, INVALID_HANDLE)
            } else {
                intent.putExtra(INTENT_EXTRA_KEY_PARENT_HANDLE, parentNode.handle)
            }

            if (viewModel.searchMode) {
                intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, PHOTOS_SEARCH_ADAPTER);
                intent.putExtra(
                    INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH,
                    viewModel.getHandlesOfPhotos()
                )
            } else {
                intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, PHOTOS_BROWSE_ADAPTER)
            }

            intent.putExtra(INTENT_EXTRA_KEY_SCREEN_POSITION, getThumbnailLocationOnScreen(it))

            FullScreenImageViewerLollipop.setDraggingThumbnailCallback(
                DraggingThumbnailCallback(
                    WeakReference(this)
                )
            )

            startActivity(intent)
            requireActivity().overridePendingTransition(0, 0)

            node.node?.let { node ->
                draggingPhotoHandle = node.handle
            }
        }
    }

    /** All below methods are for supporting functions of FullScreenImageViewer */

    fun scrollToPhoto(handle: Long) {
        val position = viewModel.getNodePositionByHandle(handle)
        if (position == INVALID_POSITION) return

        listView.scrollToPosition(position)
        notifyThumbnailLocationOnScreen()
    }

    private fun getThumbnailLocationOnScreen(imageView: ImageView): IntArray {
        val topLeft = IntArray(2)
        imageView.getLocationOnScreen(topLeft)
        return intArrayOf(topLeft[0], topLeft[1], imageView.width, imageView.height)
    }

    private fun getDraggingThumbnailLocationOnScreen(): IntArray? {
        val thumbnailView = getThumbnailViewByHandle(draggingPhotoHandle) ?: return null
        return getThumbnailLocationOnScreen(thumbnailView)
    }

    private fun notifyThumbnailLocationOnScreen() {
        val location = getDraggingThumbnailLocationOnScreen() ?: return
        location[0] += location[2] / 2
        location[1] += location[3] / 2

        val intent = Intent(BROADCAST_ACTION_INTENT_FILTER_UPDATE_IMAGE_DRAG)
        intent.putExtra(INTENT_EXTRA_KEY_SCREEN_POSITION, location)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    private fun getThumbnailViewByHandle(handle: Long): ImageView? {
        val position = viewModel.getNodePositionByHandle(handle)
        val viewHolder = listView.findViewHolderForLayoutPosition(position) ?: return null
        return viewHolder.itemView.findViewById(R.id.thumbnail)
    }

    fun hideDraggingThumbnail(handle: Long) {
        getThumbnailViewByHandle(draggingPhotoHandle)?.apply { visibility = View.VISIBLE }
        getThumbnailViewByHandle(handle)?.apply { visibility = View.INVISIBLE }
        draggingPhotoHandle = handle
        notifyThumbnailLocationOnScreen()
    }

    companion object {
        private class DraggingThumbnailCallback(private val fragmentRef: WeakReference<PhotosFragment>) :
            FullScreenImageViewerLollipop.DraggingThumbnailCallback {

            override fun setVisibility(visibility: Int) {
                val fragment = fragmentRef.get() ?: return
                fragment.getThumbnailViewByHandle(fragment.draggingPhotoHandle)
                    ?.apply { this.visibility = visibility }
            }

            override fun getLocationOnScreen(location: IntArray) {
                val fragment = fragmentRef.get() ?: return
                val result = fragment.getDraggingThumbnailLocationOnScreen() ?: return
                result.copyInto(location, 0, 0, 2)
            }
        }
    }
}