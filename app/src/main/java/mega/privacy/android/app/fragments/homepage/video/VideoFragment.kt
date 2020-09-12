package mega.privacy.android.app.fragments.homepage.video

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.components.CustomizedGridLayoutManager
import mega.privacy.android.app.components.NewGridRecyclerView
import mega.privacy.android.app.components.PositionDividerItemDecoration
import mega.privacy.android.app.databinding.FragmentVideoBinding
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.fragments.homepage.*
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.lollipop.controllers.NodeController
import mega.privacy.android.app.modalbottomsheet.NodeOptionsBottomSheetDialogFragment.*
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.Constants.*
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import java.lang.ref.WeakReference

@AndroidEntryPoint
class VideoFragment : BaseFragment(), HomepageSearchable {

    private val viewModel by viewModels<VideoViewModel>()
    private val actionModeViewModel by viewModels<ActionModeViewModel>()
    private val itemOperationViewModel by viewModels<ItemOperationViewModel>()
    private val sortByHeaderViewModel by viewModels<SortByHeaderViewModel>()

    private lateinit var binding: FragmentVideoBinding
    private lateinit var listView: NewGridRecyclerView
    private lateinit var listAdapter: NodeListAdapter
    private lateinit var gridAdapter: NodeGridAdapter
    private lateinit var itemDecoration: PositionDividerItemDecoration

    private var actionMode: ActionMode? = null
    private lateinit var actionModeCallback: ActionModeCallback

    private lateinit var managerActivity: ManagerActivityLollipop

    private var draggingNodeHandle = MegaApiJava.INVALID_HANDLE

    private val animatorListener = object : Animator.AnimatorListener {

        override fun onAnimationRepeat(animation: Animator?) {
        }

        override fun onAnimationEnd(animation: Animator?) {
            updateUi()
        }

        override fun onAnimationCancel(animation: Animator?) {
        }

        override fun onAnimationStart(animation: Animator?) {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentVideoBinding.inflate(inflater, container, false).apply {
            viewModel = this@VideoFragment.viewModel
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        managerActivity = requireActivity() as ManagerActivityLollipop

        setupListView()
        setupListAdapter()
        setupFastScroller()
        setupActionMode()
        setupNavigation()
        setupDraggingThumbnailCallback()

        viewModel.items.observe(viewLifecycleOwner) {
            if (!viewModel.searchMode) {
                // Hide the search icon if no file
                managerActivity.invalidateOptionsMenu()
            }

            actionModeViewModel.setNodesData(it.filter { nodeItem -> nodeItem.node != null })
        }
    }

    private fun setupListView() {
        listView = binding.videoList
        preventListItemBlink(listView)
        elevateToolbarWhenScrolling(managerActivity, listView)
        itemDecoration = PositionDividerItemDecoration(context, outMetrics)

        listView.clipToPadding = false
        listView.setHasFixedSize(true)
    }

    private fun setupListAdapter() {
        listAdapter =
            NodeListAdapter(actionModeViewModel, itemOperationViewModel, sortByHeaderViewModel)
        gridAdapter =
            NodeGridAdapter(actionModeViewModel, itemOperationViewModel, sortByHeaderViewModel)

        switchListGridView(sortByHeaderViewModel.isList)
    }

    private fun switchListGridView(isList: Boolean) {
        if (isList) {
            listView.switchToLinear()
            listView.adapter = listAdapter
            listView.addItemDecoration(itemDecoration)
        } else {
            listView.switchBackToGrid()
            listView.adapter = gridAdapter
            listView.removeItemDecoration(itemDecoration)

            (listView.layoutManager as CustomizedGridLayoutManager).apply {
                spanSizeLookup = gridAdapter.getSpanSizeLookup(spanCount)
            }
        }
        viewModel.refreshUi()
    }

    private fun setupFastScroller() = binding.scroller.setRecyclerView(listView)

    private fun setupActionMode() {
        actionModeCallback = ActionModeCallback(managerActivity, actionModeViewModel, megaApi)

        observeItemLongClick()
        observeSelectedItems()
        observeAnimatedItems()
        observeActionModeDestroy()
    }

    private fun observeItemLongClick() =
        actionModeViewModel.longClick.observe(viewLifecycleOwner, EventObserver {
            doIfOnline(managerActivity) { actionModeViewModel.enterActionMode(it) }
        })

    private fun observeSelectedItems() =
        actionModeViewModel.selectedNodes.observe(viewLifecycleOwner, Observer {
            if (it.isEmpty()) {
                actionMode?.finish()
            } else {
                viewModel.items.value?.let { items ->
                    // The "sort by" header isn't counted
                    actionModeCallback.nodeCount = items.size - 1
                }

                if (actionMode == null) {
                    managerActivity.hideKeyboardSearch()
                    actionMode = managerActivity.startSupportActionMode(actionModeCallback)
                } else {
                    // Update the action items based on the selected nodes
                    actionMode!!.invalidate()
                }

                actionMode?.title = it.size.toString()
            }
        })

    private fun observeAnimatedItems() {
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

            animatorSet?.addListener(animatorListener)

            it.forEach { pos ->
                listView.findViewHolderForAdapterPosition(pos)?.let { viewHolder ->
                    val itemView = viewHolder.itemView

                    val imageView: ImageView? = if (sortByHeaderViewModel.isList) {
                        if (listAdapter.getItemViewType(pos) != NodeListAdapter.TYPE_HEADER) {
                            itemView.setBackgroundColor(resources.getColor(R.color.new_multiselect_color))
                        }
                        itemView.findViewById(R.id.thumbnail)
                    } else {
                        itemView.findViewById(R.id.ic_selected)
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
            managerActivity.showKeyboardForSearch()
        })

    private fun updateUi() = viewModel.items.value?.let {
        if (sortByHeaderViewModel.isList) listAdapter.submitList(it) else gridAdapter.submitList(it)
    }

    private fun setupNavigation() {
        itemOperationViewModel.openItemEvent.observe(viewLifecycleOwner, EventObserver {
            val node = it.node
            if (node != null) {
                openNode(node, it.index)
            }
        })

        itemOperationViewModel.showNodeItemOptionsEvent.observe(viewLifecycleOwner, EventObserver {
            doIfOnline(managerActivity) {
                managerActivity.showNodeOptionsPanel(
                    it.node,
                    if (viewModel.searchMode) MODE5 else MODE1
                )
            }
        })

        sortByHeaderViewModel.showDialogEvent.observe(viewLifecycleOwner, EventObserver {
            (managerActivity).showNewSortByPanel()
        })

        sortByHeaderViewModel.orderChangeEvent.observe(viewLifecycleOwner, EventObserver {
            if (sortByHeaderViewModel.isList) {
                listAdapter.notifyItemChanged(POSITION_HEADER)
            } else {
                gridAdapter.notifyItemChanged(POSITION_HEADER)
            }
            viewModel.loadVideo(true, it)
        })

        sortByHeaderViewModel.listGridChangeEvent.observe(
            viewLifecycleOwner,
            EventObserver { isList ->
                switchListGridView(isList)
            })
    }

    private fun openNode(node: MegaNode, index: Int) {
        val file: MegaNode = node

        val internalIntent = FileUtils.isInternalIntent(node)
        val intent = if (internalIntent) {
            Intent(context, AudioVideoPlayerLollipop::class.java)
        } else {
            Intent(Intent.ACTION_VIEW)
        }

        intent.putExtra(INTENT_EXTRA_KEY_POSITION, index)
        intent.putExtra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN, viewModel.order)
        intent.putExtra(INTENT_EXTRA_KEY_FILE_NAME, node.name)
        intent.putExtra(INTENT_EXTRA_KEY_HANDLE, file.handle)

        if (viewModel.searchMode) {
            intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, VIDEO_SEARCH_ADAPTER)
            intent.putExtra(INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH, viewModel.getHandlesOfVideo())
        } else {
            intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, VIDEO_BROWSE_ADAPTER)
        }

        listView.findViewHolderForLayoutPosition(index)?.itemView?.findViewById<ImageView>(R.id.thumbnail)
            ?.let {
                intent.putExtra(INTENT_EXTRA_KEY_SCREEN_POSITION, it.getLocationAndDimen())
            }

        val localPath = FileUtils.getLocalFile(context, file.name, file.size)
        var paramsSetSuccessfully = if (FileUtils.isLocalFile(context, node, megaApi, localPath)) {
            FileUtils.setLocalIntentParams(context, node, intent, localPath, false)
        } else {
            FileUtils.setStreamingIntentParams(context, node, megaApi, intent)
        }

        if (paramsSetSuccessfully && FileUtils.isOpusFile(node)) {
            intent.setDataAndType(intent.data, "audio/*")
        }

        if (!MegaApiUtils.isIntentAvailable(context, intent)) {
            paramsSetSuccessfully = false
            Util.showSnackbar(
                context,
                SNACKBAR_TYPE,
                getString(R.string.intent_not_available),
                -1
            )
        }

        if (paramsSetSuccessfully) {
            if (internalIntent) {
                draggingNodeHandle = node.handle
                setupDraggingThumbnailCallback()
                startActivity(intent)
                managerActivity.overridePendingTransition(0, 0)
            } else {
                startActivity(intent)
            }
        } else {
            LogUtil.logWarning("itemClick:noAvailableIntent")
            Util.showSnackbar(
                context,
                SNACKBAR_TYPE,
                getString(R.string.intent_not_available),
                -1
            )
            val nC = NodeController(context)
            nC.prepareForDownload(arrayListOf(node.handle), true)
        }
    }

    override fun shouldShowSearchMenu(): Boolean = viewModel.shouldShowSearchMenu()

    override fun searchReady() {
        // Rotate screen in action mode, the keyboard would pop up again, hide it
        if (actionMode != null) {
            RunOnUIThreadUtils.post { managerActivity.hideKeyboardSearch() }
        }
        if (viewModel.searchMode) return

        viewModel.searchMode = true
        viewModel.searchQuery = ""
        viewModel.refreshUi()
    }

    override fun exitSearch() {
        if (!viewModel.searchMode) return

        viewModel.searchMode = false
        viewModel.searchQuery = ""
        viewModel.refreshUi()
    }

    override fun searchQuery(query: String) {
        if (viewModel.searchQuery == query) return
        viewModel.searchQuery = query
        viewModel.loadVideo()
    }

    /** All below methods are for supporting functions of AudioVideoPlayerLollipop */

    companion object {
        private const val POSITION_HEADER = 0

        private class VideoDraggingThumbnailCallback(private val fragmentRef: WeakReference<VideoFragment>) :
            DraggingThumbnailCallback {

            override fun setVisibility(visibility: Int) {
                val fragment = fragmentRef.get() ?: return
                fragment.getThumbnailViewByHandle(fragment.draggingNodeHandle)
                    ?.apply { this.visibility = visibility }
            }

            override fun getLocationOnScreen(location: IntArray) {
                val fragment = fragmentRef.get() ?: return
                val result = fragment.getDraggingThumbnailLocationOnScreen() ?: return
                result.copyInto(location, 0, 0, 2)
            }
        }
    }

    private fun getDraggingThumbnailLocationOnScreen(): IntArray? {
        val thumbnailView = getThumbnailViewByHandle(draggingNodeHandle) ?: return null
        return thumbnailView.getLocationAndDimen()
    }

    private fun getThumbnailViewByHandle(handle: Long): ImageView? {
        val position = viewModel.getNodePositionByHandle(handle)
        val viewHolder = listView.findViewHolderForLayoutPosition(position) ?: return null
        return viewHolder.itemView.findViewById(R.id.thumbnail)
    }

    private fun setupDraggingThumbnailCallback() =
        AudioVideoPlayerLollipop.addDraggingThumbnailCallback(
            VideoFragment::class.java, VideoDraggingThumbnailCallback(WeakReference(this))
        )

    fun scrollToPhoto(handle: Long) {
        val position = viewModel.getNodePositionByHandle(handle)
        if (position == INVALID_POSITION) return

        listView.scrollToPosition(position)
        notifyThumbnailLocationOnScreen()
    }

    fun hideDraggingThumbnail(handle: Long) {
        getThumbnailViewByHandle(draggingNodeHandle)?.apply { visibility = View.VISIBLE }
        getThumbnailViewByHandle(handle)?.apply { visibility = View.INVISIBLE }
        draggingNodeHandle = handle
        notifyThumbnailLocationOnScreen()
    }

    private fun notifyThumbnailLocationOnScreen() {
        val location = getDraggingThumbnailLocationOnScreen() ?: return
        location[0] += location[2] / 2
        location[1] += location[3] / 2

        val intent = Intent(BROADCAST_ACTION_INTENT_FILTER_UPDATE_IMAGE_DRAG)
        intent.putExtra(INTENT_EXTRA_KEY_SCREEN_POSITION, location)
        LocalBroadcastManager.getInstance(managerActivity).sendBroadcast(intent)
    }
}