package mega.privacy.android.app.fragments.homepage.documents

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
import androidx.recyclerview.widget.RecyclerView
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.CustomizedGridLayoutManager
import mega.privacy.android.app.components.NewGridRecyclerView
import mega.privacy.android.app.components.PositionDividerItemDecoration
import mega.privacy.android.app.components.dragger.DragThumbnailGetter
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.putThumbnailLocation
import mega.privacy.android.app.databinding.FragmentDocumentsBinding
import mega.privacy.android.app.fragments.homepage.ActionModeCallback
import mega.privacy.android.app.fragments.homepage.ActionModeViewModel
import mega.privacy.android.app.fragments.homepage.BaseNodeItemAdapter.Companion.TYPE_HEADER
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.HomepageSearchable
import mega.privacy.android.app.fragments.homepage.ItemOperationViewModel
import mega.privacy.android.app.fragments.homepage.NodeGridAdapter
import mega.privacy.android.app.fragments.homepage.NodeListAdapter
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.fragments.homepage.disableRecyclerViewAnimator
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.PdfViewerActivity
import mega.privacy.android.app.mediaplayer.miniplayer.MiniAudioPlayerController
import mega.privacy.android.app.modalbottomsheet.NodeOptionsBottomSheetDialogFragment.Companion.CLOUD_DRIVE_MODE
import mega.privacy.android.app.modalbottomsheet.NodeOptionsBottomSheetDialogFragment.Companion.SEARCH_MODE
import mega.privacy.android.app.modalbottomsheet.UploadBottomSheetDialogFragment.Companion.DOCUMENTS_UPLOAD
import mega.privacy.android.app.utils.Constants.DOCUMENTS_BROWSE_ADAPTER
import mega.privacy.android.app.utils.Constants.DOCUMENTS_SEARCH_ADAPTER
import mega.privacy.android.app.utils.Constants.EVENT_FAB_CHANGE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_INSIDE
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.ORDER_CLOUD
import mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaNodeUtil.manageTextFileIntent
import mega.privacy.android.app.utils.MegaNodeUtil.onNodeTapped
import mega.privacy.android.app.utils.RunOnUIThreadUtils
import mega.privacy.android.app.utils.TextUtil.formatEmptyScreenText
import mega.privacy.android.app.utils.Util.noChangeRecyclerViewItemAnimator
import mega.privacy.android.app.utils.callManager
import mega.privacy.android.app.utils.displayMetrics
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.preference.ViewType
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * [Fragment] that handles Documents-related operations
 */
@AndroidEntryPoint
class DocumentsFragment : Fragment(), HomepageSearchable {

    private val viewModel by viewModels<DocumentsViewModel>()
    private val actionModeViewModel by viewModels<ActionModeViewModel>()
    private val itemOperationViewModel by viewModels<ItemOperationViewModel>()
    private val sortByHeaderViewModel by viewModels<SortByHeaderViewModel>()

    private lateinit var binding: FragmentDocumentsBinding
    private lateinit var listView: NewGridRecyclerView
    private lateinit var listAdapter: NodeListAdapter
    private lateinit var gridAdapter: NodeGridAdapter
    private lateinit var itemDecoration: PositionDividerItemDecoration

    private var actionMode: ActionMode? = null
    private lateinit var actionModeCallback: ActionModeCallback

    /**
     * Used to access SDK-related functions
     */
    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    private var openingNodeHandle = INVALID_HANDLE

    private val fabChangeObserver = androidx.lifecycle.Observer<Boolean> {
        if (it && actionModeViewModel.selectedNodes.value.isNullOrEmpty()) {
            showFabButton()
        } else {
            hideFabButton()
        }
    }

    /**
     * onCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentDocumentsBinding.inflate(inflater, container, false).apply {
            viewModel = this@DocumentsFragment.viewModel
            sortByHeaderViewModel = this@DocumentsFragment.sortByHeaderViewModel
        }

        LiveEventBus.get(EVENT_FAB_CHANGE, Boolean::class.java)
            .observeForever(fabChangeObserver)

        return binding.root
    }

    /**
     * onSaveInstanceState
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        viewModel.skipNextAutoScroll = true
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner

        setupEmptyHint()
        setupListView()
        setupListAdapter()
        setupFastScroller()
        setupActionMode()
        setupNavigation()
        setupAddFabButton()
        setupMiniAudioPlayer()

        viewModel.items.observe(viewLifecycleOwner) {
            if (viewModel.searchMode) {
                binding.addFabButton.hide()
            } else {
                callManager { manager ->
                    manager.invalidateOptionsMenu()  // Hide the search icon if no file
                }
            }

            actionModeViewModel.setNodesData(it.filter { nodeItem -> nodeItem.node != null })
        }

        viewLifecycleOwner.collectFlow(sortByHeaderViewModel.state) { state ->
            if ((state.viewType == ViewType.LIST) != viewModel.isList) {
                // Changing the adapter will cause the scroll position to be lost
                // To avoid that, the adapter will only change when the list/grid view
                // really changes
                switchViewType(state.viewType)
                viewModel.refreshUi()
            }
        }
    }

    /**
     * onDestroyView
     */
    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.cancelSearch()
        LiveEventBus.get(EVENT_FAB_CHANGE, Boolean::class.java)
            .removeObserver(fabChangeObserver)
    }

    /**
     * shouldShowSearchMenu
     */
    override fun shouldShowSearchMenu(): Boolean = viewModel.shouldShowSearchMenu()

    /**
     * searchReady
     */
    override fun searchReady() {
        // Rotate screen in action mode, the keyboard would pop up again, hide it
        if (actionMode != null) {
            RunOnUIThreadUtils.post { callManager { it.hideKeyboardSearch() } }
        }

        viewModel.readySearch()

        binding.addFabButton.hide()
    }

    /**
     * exitSearch
     */
    override fun exitSearch() {
        itemDecoration.setDrawAllDividers(false)
        disableRecyclerViewAnimator(listView)
        viewModel.exitSearch()

        binding.addFabButton.show()

    }

    /**
     * searchQuery
     */
    override fun searchQuery(query: String) {
        if (viewModel.searchQuery == query) return
        viewModel.searchQuery = query
        viewModel.loadDocuments()
    }

    /**
     * Setup empty hint behavior
     */
    private fun setupEmptyHint() {
        with(binding.emptyHint) {
            emptyHintImage.isVisible = false
            emptyHintImage.setImageResource(R.drawable.ic_homepage_empty_document)
            emptyHintText.isVisible = false
            emptyHintText.text = formatEmptyScreenText(
                requireContext(),
                getString(R.string.homepage_empty_hint_documents)
            )
        }
    }

    /**
     * Perform a specific operation when online
     *
     * @param operation lambda that specifies the operation to be executed
     */
    private fun doIfOnline(operation: () -> Unit) {
        if (viewModel.isConnected) {
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

    /**
     * Setup the navigation of this feature
     */
    private fun setupNavigation() {
        itemOperationViewModel.openItemEvent.observe(viewLifecycleOwner, EventObserver {
            openDoc(it.node, it.index)
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
            callManager { manager ->
                manager.showNewSortByPanel(ORDER_CLOUD)
            }
        })

        sortByHeaderViewModel.orderChangeEvent.observe(viewLifecycleOwner, EventObserver {
            viewModel.onOrderChange()
        })
    }

    /**
     * Switches how Audio items are being displayed
     *
     * @param viewType The View Type
     */
    private fun switchViewType(viewType: ViewType) {
        viewModel.isList = viewType == ViewType.LIST
        listView.apply {
            when (viewType) {
                ViewType.LIST -> {
                    switchToLinear()
                    adapter = listAdapter

                    if (itemDecorationCount == 0) addItemDecoration(itemDecoration)
                }
                ViewType.GRID -> {
                    switchBackToGrid()
                    adapter = gridAdapter
                    removeItemDecoration(itemDecoration)

                    (layoutManager as CustomizedGridLayoutManager).apply {
                        spanSizeLookup = gridAdapter.getSpanSizeLookup(spanCount)
                    }
                }
            }
        }
    }

    /**
     * Updates the UI by refreshing the list content
     */
    private fun updateUi() = viewModel.items.value?.let { it ->
        val newList = ArrayList(it)

        if (sortByHeaderViewModel.isListView()) {
            listAdapter.submitList(newList)
        } else {
            gridAdapter.submitList(newList)
        }
    }

    /**
     * Establishes the List View
     */
    private fun setupListView() {
        listView = binding.documentList
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

    /**
     * Establishes the Action Mode
     */
    private fun setupActionMode() {
        actionModeCallback = ActionModeCallback(
            requireActivity() as ManagerActivity, actionModeViewModel, megaApi
        )

        observeItemLongClick()
        observeSelectedItems()
        observeAnimatedItems()
        observeActionModeDestroy()
    }

    /**
     * Performs certain behavior when a long press is observed
     */
    private fun observeItemLongClick() =
        actionModeViewModel.longClick.observe(viewLifecycleOwner, EventObserver {
            doIfOnline { actionModeViewModel.enterActionMode(it) }
        })

    /**
     * Observe selected Nodes from [ActionModeViewModel]
     */
    private fun observeSelectedItems() =
        actionModeViewModel.selectedNodes.observe(viewLifecycleOwner) {
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

                    binding.addFabButton.hide()
                } else {
                    actionMode?.invalidate()  // Update the action items based on the selected nodes
                }

                actionMode?.title = it.size.toString()
            }
        }

    /**
     * Observes item animation
     */
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

                    val imageView: ImageView? = if (sortByHeaderViewModel.isListView()) {
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
        }
    }

    /**
     * Performs certain behavior when the Action Mode is destroyed
     */
    private fun observeActionModeDestroy() =
        actionModeViewModel.actionModeDestroy.observe(viewLifecycleOwner, EventObserver {
            actionMode = null
            binding.addFabButton.show()
            callManager { manager ->
                manager.showKeyboardForSearch()
            }
        })

    /**
     * Setup fast scroller for the [RecyclerView]
     */
    private fun setupFastScroller() = binding.scroller.setRecyclerView(listView)

    /**
     * Setup the List Adapter
     */
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

        switchViewType(sortByHeaderState().viewType)
    }

    /**
     * The UI State from [SortByHeaderViewModel]
     *
     * @return The UI State
     */
    private fun sortByHeaderState() = sortByHeaderViewModel.state.value

    /**
     * Immediately scroll to the top of the list
     */
    private fun autoScrollToTop() {
        if (!viewModel.skipNextAutoScroll) {
            listView.layoutManager?.scrollToPosition(0)
        }
        viewModel.skipNextAutoScroll = false
    }

    /**
     * Opens a Document
     *
     * @param node The [MegaNode] to be opened
     * @param index The [MegaNode] index
     */
    private fun openDoc(node: MegaNode?, index: Int) {
        if (node == null) {
            return
        }

        val localPath = FileUtil.getLocalFile(node)

        if (MimeTypeList.typeForName(node.name).isPdf) {
            val intent = Intent(context, PdfViewerActivity::class.java)
            intent.putExtra(INTENT_EXTRA_KEY_INSIDE, true)
            if (viewModel.searchMode) {
                intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, DOCUMENTS_SEARCH_ADAPTER)
            } else {
                intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, DOCUMENTS_BROWSE_ADAPTER)
            }

            (listView.adapter as? DragThumbnailGetter)?.let {
                putThumbnailLocation(intent, listView, index, INVALID_VALUE, it)
            }

            val paramsSetSuccessfully =
                if (FileUtil.isLocalFile(node, megaApi, localPath)) {
                    FileUtil.setLocalIntentParams(
                        activity, node, intent, localPath, false,
                        requireActivity() as ManagerActivity
                    )
                } else {
                    FileUtil.setStreamingIntentParams(
                        activity, node, megaApi, intent,
                        requireActivity() as ManagerActivity
                    )
                }

            intent.putExtra(INTENT_EXTRA_KEY_HANDLE, node.handle)

            if (paramsSetSuccessfully) {
                openingNodeHandle = node.handle
                startActivity(intent)
                requireActivity().overridePendingTransition(0, 0)
                return
            }
        } else if (MimeTypeList.typeForName(node.name).isOpenableTextFile(node.size)) {
            manageTextFileIntent(
                requireContext(),
                node,
                if (viewModel.searchMode) DOCUMENTS_SEARCH_ADAPTER else DOCUMENTS_BROWSE_ADAPTER
            )
        } else {
            onNodeTapped(
                requireActivity(),
                node,
                { (requireActivity() as ManagerActivity).saveNodeByTap(it) },
                requireActivity() as ManagerActivity,
                requireActivity() as ManagerActivity
            )
        }
    }

    /**
     * Setup the Fab button
     */
    private fun setupAddFabButton() {
        binding.addFabButton.setOnClickListener {
            (requireActivity() as ManagerActivity).showUploadPanel(DOCUMENTS_UPLOAD)
        }
    }

    /**
     * Hides the fabButton
     */
    fun hideFabButton() {
        binding.addFabButton.hide()
    }

    /**
     * Shows the fabButton
     */
    fun showFabButton() {
        binding.addFabButton.show()
    }

    /**
     * Establish the mini audio player
     */
    private fun setupMiniAudioPlayer() {
        val audioPlayerController = MiniAudioPlayerController(binding.miniAudioPlayer).apply {
            shouldVisible = true
        }
        lifecycle.addObserver(audioPlayerController)
    }
}
