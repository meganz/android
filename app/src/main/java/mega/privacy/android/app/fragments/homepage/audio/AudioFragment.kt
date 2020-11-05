package mega.privacy.android.app.fragments.homepage.audio

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Intent
import android.content.res.Configuration
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
import androidx.recyclerview.widget.SimpleItemAnimator
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.audioplayer.AudioPlayerActivity
import mega.privacy.android.app.components.CustomizedGridLayoutManager
import mega.privacy.android.app.components.ListenScrollChangesHelper
import mega.privacy.android.app.components.NewGridRecyclerView
import mega.privacy.android.app.components.PositionDividerItemDecoration
import mega.privacy.android.app.databinding.FragmentAudioBinding
import mega.privacy.android.app.fragments.homepage.*
import mega.privacy.android.app.fragments.homepage.BaseNodeItemAdapter.Companion.TYPE_HEADER
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.lollipop.controllers.NodeController
import mega.privacy.android.app.modalbottomsheet.NodeOptionsBottomSheetDialogFragment.MODE1
import mega.privacy.android.app.modalbottomsheet.NodeOptionsBottomSheetDialogFragment.MODE5
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.FileUtil.*
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.MegaApiUtils.isIntentAvailable
import mega.privacy.android.app.utils.Util.showSnackbar
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaNode
import java.lang.ref.WeakReference
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class AudioFragment : Fragment(), HomepageSearchable {

    private val viewModel by viewModels<AudioViewModel>()
    private val actionModeViewModel by viewModels<ActionModeViewModel>()
    private val itemOperationViewModel by viewModels<ItemOperationViewModel>()
    private val sortByHeaderViewModel by viewModels<SortByHeaderViewModel>()

    private lateinit var binding: FragmentAudioBinding
    private lateinit var listView: NewGridRecyclerView
    private lateinit var listAdapter: NodeListAdapter
    private lateinit var gridAdapter: NodeGridAdapter
    private lateinit var itemDecoration: PositionDividerItemDecoration

    private var actionMode: ActionMode? = null
    private lateinit var actionModeCallback: ActionModeCallback

    @Inject
    lateinit var megaApi: MegaApiAndroid

    private var draggingNodeHandle = INVALID_HANDLE

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAudioBinding.inflate(inflater, container, false).apply {
            viewModel = this@AudioFragment.viewModel
            sortByHeaderViewModel = this@AudioFragment.sortByHeaderViewModel
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
        setupDraggingThumbnailCallback()

        viewModel.items.observe(viewLifecycleOwner) {
            if (!viewModel.searchMode) {
                callManager { manager ->
                    manager.invalidateOptionsMenu()  // Hide the search icon if no file
                }
            }

            actionModeViewModel.setNodesData(it.filter { nodeItem -> nodeItem.node != null })
        }
    }

    private fun setupEmptyHint() {
        binding.emptyHint.emptyHintImage.setImageResource(
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                R.drawable.ic_zero_data_recents_portrait
            } else {
                R.drawable.ic_zero_data_recents_landscape
            }
        )
        binding.emptyHint.emptyHintImage.isVisible = false
        binding.emptyHint.emptyHintText.isVisible = false
        binding.emptyHint.emptyHintText.text =
                getString(R.string.homepage_empty_hint_audio).toUpperCase(Locale.ROOT)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        viewModel.skipNextAutoScroll = true
    }

    override fun onDestroy() {
        super.onDestroy()
        AudioVideoPlayerLollipop.removeDraggingThumbnailCallback(AudioFragment::class.java)
    }

    private fun doIfOnline(operation: () -> Unit) {
        if (Util.isOnline(context)) {
            operation()
        } else {
            callManager {
                it.hideKeyboardSearch()  // Make the snack bar visible to the user
                it.showSnackbar(
                        SNACKBAR_TYPE,
                        getString(R.string.error_server_connection_problem),
                        MEGACHAT_INVALID_HANDLE
                )
            }
        }
    }

    private fun setupNavigation() {
        itemOperationViewModel.openItemEvent.observe(viewLifecycleOwner, EventObserver {
            openNode(it.node, it.index)
        })

        itemOperationViewModel.showNodeItemOptionsEvent.observe(viewLifecycleOwner, EventObserver {
            doIfOnline {
                callManager { manager ->
                    manager.showNodeOptionsPanel(
                            it.node, if (viewModel.searchMode) MODE5 else MODE1
                    )
                }
            }
        })

        sortByHeaderViewModel.showDialogEvent.observe(viewLifecycleOwner, EventObserver {
            callManager { manager ->
                manager.showNewSortByPanel()
            }
        })

        sortByHeaderViewModel.orderChangeEvent.observe(viewLifecycleOwner, EventObserver {
            viewModel.loadAudio(true, it)
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

    private fun openNode(node: MegaNode?, index: Int) {
        if (node == null) {
            return
        }

        val file: MegaNode = node

        val internalIntent = isInternalIntent(node)
        val intent = if (internalIntent) {
            Intent(context, AudioPlayerActivity::class.java)
        } else {
            Intent(Intent.ACTION_VIEW)
        }

        intent.putExtra(INTENT_EXTRA_KEY_POSITION, index)
        intent.putExtra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN, viewModel.order)
        intent.putExtra(INTENT_EXTRA_KEY_FILE_NAME, node.name)
        intent.putExtra(INTENT_EXTRA_KEY_HANDLE, file.handle)

        if (viewModel.searchMode) {
            intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, AUDIO_SEARCH_ADAPTER)
            intent.putExtra(INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH, viewModel.getHandlesOfAudio())
        } else {
            intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, AUDIO_BROWSE_ADAPTER)
        }

        listView.findViewHolderForLayoutPosition(index)?.itemView?.findViewById<ImageView>(R.id.thumbnail)
                ?.let {
                    intent.putExtra(INTENT_EXTRA_KEY_SCREEN_POSITION, it.getLocationAndDimen())
                }

        val localPath = getLocalFile(context, file.name, file.size)
        var paramsSetSuccessfully = if (isLocalFile(node, megaApi, localPath)) {
            setLocalIntentParams(activity, node, intent, localPath, false)
        } else {
            setStreamingIntentParams(activity, node, megaApi, intent)
        }

        if (paramsSetSuccessfully && isOpusFile(node)) {
            intent.setDataAndType(intent.data, "audio/*")
        }

        if (!isIntentAvailable(context, intent)) {
            paramsSetSuccessfully = false
            showSnackbar(activity, SNACKBAR_TYPE, getString(R.string.intent_not_available),
                    MEGACHAT_INVALID_HANDLE)
        }

        if (paramsSetSuccessfully) {
            if (internalIntent) {
                draggingNodeHandle = node.handle
                setupDraggingThumbnailCallback()
                startActivity(intent)
                requireActivity().overridePendingTransition(0, 0)
            } else {
                startActivity(intent)
            }
        } else {
            logWarning("itemClick:noAvailableIntent")
            showSnackbar(activity, SNACKBAR_TYPE, getString(R.string.intent_not_available),
                    MEGACHAT_INVALID_HANDLE)
            val nC = NodeController(context)
            nC.prepareForDownload(arrayListOf(node.handle), true)
        }
    }

    /**
     * Only refresh the list items of uiDirty = true
     */
    private fun updateUi() = viewModel.items.value?.let { it ->
        val newList = ArrayList<NodeItem>(it)

        if (sortByHeaderViewModel.isList) {
            listAdapter.submitList(newList)
        } else {
            gridAdapter.submitList(newList)
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
        callManager { manager ->
            manager.changeActionBarElevation(v!!.canScrollVertically(-1))
        }
    }

    private fun setupListView() {
        listView = binding.audioList
        preventListItemBlink()
        elevateToolbarWhenScrolling()
        itemDecoration = PositionDividerItemDecoration(context, displayMetrics())

        listView.clipToPadding = false
        listView.setHasFixedSize(true)
    }

    private fun setupActionMode() {
        actionModeCallback = ActionModeCallback(
                requireActivity() as ManagerActivityLollipop, actionModeViewModel, megaApi
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

                    val imageView: ImageView? = if (sortByHeaderViewModel.isList) {
                        if (listAdapter.getItemViewType(pos) != TYPE_HEADER) {
                            itemView.setBackgroundColor(resources.getColor(R.color.new_multiselect_color))
                        }
                        itemView.findViewById(R.id.thumbnail)
                    } else {
                        if (gridAdapter.getItemViewType(pos) != TYPE_HEADER) {
                            itemView.setBackgroundResource(R.drawable.background_item_grid_selected)
                        }
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
                callManager { manager ->
                    manager.showKeyboardForSearch()
                }
            })

    private fun setupFastScroller() = binding.scroller.setRecyclerView(listView)

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
        viewModel.loadAudio()
    }

    /** All below methods are for supporting functions of AudioVideoPlayerLollipop */

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
                    AudioFragment::class.java, AudioDraggingThumbnailCallback(WeakReference(this))
            )

    fun scrollToPhoto(handle: Long) {
        val position = viewModel.getNodePositionByHandle(handle)
        if (position == Constants.INVALID_POSITION) return

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

        val intent = Intent(Constants.BROADCAST_ACTION_INTENT_FILTER_UPDATE_IMAGE_DRAG)
        intent.putExtra(INTENT_EXTRA_KEY_SCREEN_POSITION, location)
        requireContext().sendBroadcast(intent)
    }

    companion object {
        private class AudioDraggingThumbnailCallback(
                private val fragmentRef: WeakReference<AudioFragment>
        ) : DraggingThumbnailCallback {

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
}
