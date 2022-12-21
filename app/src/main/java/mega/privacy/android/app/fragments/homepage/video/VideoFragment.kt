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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.components.CustomizedGridLayoutManager
import mega.privacy.android.app.components.NewGridRecyclerView
import mega.privacy.android.app.components.PositionDividerItemDecoration
import mega.privacy.android.app.components.dragger.DragThumbnailGetter
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.observeDragSupportEvents
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.putThumbnailLocation
import mega.privacy.android.app.databinding.FragmentVideoBinding
import mega.privacy.android.app.fragments.homepage.ActionModeCallback
import mega.privacy.android.app.fragments.homepage.ActionModeViewModel
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.HomepageSearchable
import mega.privacy.android.app.fragments.homepage.ItemOperationViewModel
import mega.privacy.android.app.fragments.homepage.NodeGridAdapter
import mega.privacy.android.app.fragments.homepage.NodeItem
import mega.privacy.android.app.fragments.homepage.NodeListAdapter
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.fragments.homepage.disableRecyclerViewAnimator
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.mediaplayer.miniplayer.MiniAudioPlayerController
import mega.privacy.android.app.modalbottomsheet.NodeOptionsBottomSheetDialogFragment.CLOUD_DRIVE_MODE
import mega.privacy.android.app.modalbottomsheet.NodeOptionsBottomSheetDialogFragment.SEARCH_MODE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ORDER_GET_CHILDREN
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_POSITION
import mega.privacy.android.app.utils.Constants.ORDER_CLOUD
import mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE
import mega.privacy.android.app.utils.Constants.VIDEO_BROWSE_ADAPTER
import mega.privacy.android.app.utils.Constants.VIDEO_SEARCH_ADAPTER
import mega.privacy.android.app.utils.Constants.VIEWER_FROM_VIDEOS
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.RunOnUIThreadUtils
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil.formatEmptyScreenText
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.getMediaIntent
import mega.privacy.android.app.utils.Util.noChangeRecyclerViewItemAnimator
import mega.privacy.android.app.utils.callManager
import mega.privacy.android.app.utils.displayMetrics
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class VideoFragment : Fragment(), HomepageSearchable {

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

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentVideoBinding.inflate(inflater, container, false).apply {
            viewModel = this@VideoFragment.viewModel
            sortByHeaderViewModel = this@VideoFragment.sortByHeaderViewModel
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner

        setupEmptyHint()
        setupListView()
        setupListAdapter()
        setupFastScroller()
        setupActionMode()
        setupNavigation()
        setupMiniAudioPlayer()

        viewModel.items.observe(viewLifecycleOwner) {
            if (!viewModel.searchMode) {
                callManager { manager ->
                    manager.invalidateOptionsMenu()  // Hide the search icon if no file
                }
            }

            actionModeViewModel.setNodesData(it.filter { nodeItem -> nodeItem.node != null })
        }

        observeDragSupportEvents(viewLifecycleOwner, listView, VIEWER_FROM_VIDEOS)
    }

    override fun onDestroyView() {
        viewModel.cancelSearch()
        super.onDestroyView()
    }

    private fun setupEmptyHint() {
        with(binding.emptyHint) {
            emptyHintImage.isVisible = false
            emptyHintImage.setImageResource(R.drawable.ic_homepage_empty_video)
            emptyHintText.isVisible = false
            emptyHintText.text = formatEmptyScreenText(requireContext(),
                getString(R.string.homepage_empty_hint_video))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        viewModel.skipNextAutoScroll = true
    }

    private fun setupListView() {
        listView = binding.videoList
        with(listView) {
            itemAnimator = noChangeRecyclerViewItemAnimator()
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
        }
        itemDecoration = PositionDividerItemDecoration(context, displayMetrics())
    }

    private fun doIfOnline(operation: () -> Unit) {
        if (viewModel.isConnected) {
            operation()
        } else {
            callManager {
                it.hideKeyboardSearch()  // Make the snack bar visible to the user
                it.showSnackbar(
                    SNACKBAR_TYPE,
                    StringResourcesUtils.getString(R.string.error_server_connection_problem),
                    MEGACHAT_INVALID_HANDLE
                )
            }
        }
    }

    private fun setupListAdapter() {
        listAdapter =
            NodeListAdapter(actionModeViewModel, itemOperationViewModel, sortByHeaderViewModel)
        listAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                autoScrollToTop()
            }
        })
        gridAdapter =
            NodeGridAdapter(actionModeViewModel, itemOperationViewModel, sortByHeaderViewModel)
        gridAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                autoScrollToTop()
            }
        })

        switchListGridView(sortByHeaderViewModel.isList)
    }

    private fun autoScrollToTop() {
        if (!viewModel.skipNextAutoScroll) {
            listView.layoutManager?.scrollToPosition(0)
        }
        viewModel.skipNextAutoScroll = false
    }

    private fun switchListGridView(isList: Boolean) {
        viewModel.isList = isList
        if (isList) {
            listView.switchToLinear()
            listView.adapter = listAdapter
            if (listView.itemDecorationCount == 0) {
                listView.addItemDecoration(itemDecoration)
            }
        } else {
            listView.switchBackToGrid()
            listView.adapter = gridAdapter
            listView.removeItemDecoration(itemDecoration)

            (listView.layoutManager as CustomizedGridLayoutManager).apply {
                spanSizeLookup = gridAdapter.getSpanSizeLookup(spanCount)
            }
        }
    }

    private fun setupFastScroller() = binding.scroller.setRecyclerView(listView)

    private fun setupActionMode() {
        actionModeCallback = ActionModeCallback(
            requireActivity() as ManagerActivity, actionModeViewModel, megaApi
        )

        observeItemLongClick()
        observeSelectedItems()
        observeAnimatedItems()
        observeActionModeDestroy()
    }

    private fun observeItemLongClick() =
        actionModeViewModel.longClick.observe(viewLifecycleOwner, EventObserver {
            doIfOnline { actionModeViewModel.enterActionMode(it) }
        })

    private fun observeSelectedItems() =
        actionModeViewModel.selectedNodes.observe(viewLifecycleOwner, Observer {
            if (it.isEmpty()) {
                actionMode?.apply {
                    finish()
                }
            } else {
                actionModeCallback.nodeCount = viewModel.getRealNodeCount()

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
                override fun onAnimationRepeat(animation: Animator) {
                }

                override fun onAnimationEnd(animation: Animator) {
                    updateUi()
                }

                override fun onAnimationCancel(animation: Animator) {
                }

                override fun onAnimationStart(animation: Animator) {
                }
            })

            it.forEach { pos ->
                listView.findViewHolderForAdapterPosition(pos)?.let { viewHolder ->
                    val itemView = viewHolder.itemView

                    val imageView: ImageView? = if (sortByHeaderViewModel.isList) {
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
            callManager { it.showKeyboardForSearch() }
        })

    private fun updateUi() = viewModel.items.value?.let {
        // Must create a new list, otherwise, onBindViewHolder in adapter cannot trigger.
        val newList = ArrayList<NodeItem>(it)
        if (sortByHeaderViewModel.isList) {
            listAdapter.submitList(newList)
        } else {
            gridAdapter.submitList(newList)
        }
    }

    private fun setupNavigation() {
        itemOperationViewModel.openItemEvent.observe(viewLifecycleOwner, EventObserver {
            val node = it.node
            if (node != null) {
                openNode(node, it.index)
            }
        })

        itemOperationViewModel.showNodeItemOptionsEvent.observe(viewLifecycleOwner, EventObserver {
            doIfOnline {
                callManager { manager ->
                    manager.showNodeOptionsPanel(
                        it.node,
                        if (viewModel.searchMode) SEARCH_MODE else CLOUD_DRIVE_MODE
                    )
                }
            }
        })

        sortByHeaderViewModel.showDialogEvent.observe(viewLifecycleOwner, EventObserver {
            callManager { it.showNewSortByPanel(ORDER_CLOUD) }
        })

        sortByHeaderViewModel.orderChangeEvent.observe(viewLifecycleOwner, EventObserver {
            viewModel.onOrderChange()
        })

        sortByHeaderViewModel.listGridChangeEvent.observe(
            viewLifecycleOwner,
            EventObserver { isList ->
                if (isList != viewModel.isList) {
                    // change adapter will cause lose scroll position,
                    // to avoid that, we only change adapter when the list/grid view
                    // really change.
                    switchListGridView(isList)
                }
                viewModel.refreshUi()
            })
    }

    private fun setupMiniAudioPlayer() {
        val audioPlayerController = MiniAudioPlayerController(binding.miniAudioPlayer).apply {
            shouldVisible = true
        }
        lifecycle.addObserver(audioPlayerController)
    }

    private fun openNode(node: MegaNode, index: Int) {
        val file: MegaNode = node

        val internalIntent = FileUtil.isInternalIntent(node)
        val intent = if (internalIntent) {
            getMediaIntent(context, node.name)
        } else {
            Intent(Intent.ACTION_VIEW)
        }

        intent.putExtra(INTENT_EXTRA_KEY_POSITION, index)
        intent.putExtra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN,
            sortByHeaderViewModel.cloudSortOrder.value)
        intent.putExtra(INTENT_EXTRA_KEY_FILE_NAME, node.name)
        intent.putExtra(INTENT_EXTRA_KEY_HANDLE, file.handle)

        if (viewModel.searchMode) {
            intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, VIDEO_SEARCH_ADAPTER)
            intent.putExtra(INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH, viewModel.getHandlesOfVideo())
        } else {
            intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, VIDEO_BROWSE_ADAPTER)
        }

        (listView.adapter as? DragThumbnailGetter)?.let {
            putThumbnailLocation(intent, listView, index, VIEWER_FROM_VIDEOS, it)
        }

        val localPath = FileUtil.getLocalFile(file)
        var paramsSetSuccessfully = if (FileUtil.isLocalFile(node, megaApi, localPath)) {
            FileUtil.setLocalIntentParams(context, node, intent, localPath, false,
                requireActivity() as ManagerActivity
            )
        } else {
            FileUtil.setStreamingIntentParams(context, node, megaApi, intent,
                requireActivity() as ManagerActivity
            )
        }

        if (paramsSetSuccessfully && FileUtil.isOpusFile(node)) {
            intent.setDataAndType(intent.data, "audio/*")
        }

        if (!MegaApiUtils.isIntentAvailable(context, intent)) {
            paramsSetSuccessfully = false
            Util.showSnackbar(
                context,
                SNACKBAR_TYPE,
                StringResourcesUtils.getString(R.string.intent_not_available),
                MEGACHAT_INVALID_HANDLE
            )
        }

        if (paramsSetSuccessfully) {
            startActivity(intent)
            if (internalIntent) {
                requireActivity().overridePendingTransition(0, 0)
            }
        } else {
            Timber.w("itemClick:noAvailableIntent")
            Util.showSnackbar(
                context,
                SNACKBAR_TYPE,
                StringResourcesUtils.getString(R.string.intent_not_available),
                MEGACHAT_INVALID_HANDLE
            )
            callManager { it.saveNodesToDevice(listOf(node), true, false, false, false) }
        }
    }

    override fun shouldShowSearchMenu(): Boolean = viewModel.shouldShowSearchMenu()

    override fun searchReady() {
        // Rotate screen in action mode, the keyboard would pop up again, hide it
        if (actionMode != null) {
            RunOnUIThreadUtils.post { callManager { it.hideKeyboardSearch() } }
        }

        itemDecoration.setDrawAllDividers(true)
        disableRecyclerViewAnimator(listView)


        if (viewModel.searchMode) return

        viewModel.searchMode = true
        viewModel.searchQuery = ""
        viewModel.refreshUi()
    }

    override fun exitSearch() {
        itemDecoration.setDrawAllDividers(false)
        disableRecyclerViewAnimator(listView)

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
}
