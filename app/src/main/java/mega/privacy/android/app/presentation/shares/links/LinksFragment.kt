package mega.privacy.android.app.presentation.shares.links

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.components.PositionDividerItemDecoration
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.main.adapters.MegaNodeAdapter
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.presentation.manager.model.Tab
import mega.privacy.android.app.presentation.shares.MegaNodeBaseFragment
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.ColorUtils.setImageViewAlphaIfDark
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.ORDER_CLOUD
import mega.privacy.android.app.utils.MegaNodeUtil.areAllFileNodesAndNotTakenDown
import mega.privacy.android.app.utils.MegaNodeUtil.areAllNotTakenDown
import mega.privacy.android.app.utils.MegaNodeUtil.canMoveToRubbish
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.displayMetrics
import mega.privacy.android.domain.entity.SortOrder
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import timber.log.Timber

/**
 * Links shares page
 */
@AndroidEntryPoint
class LinksFragment : MegaNodeBaseFragment() {

    private val viewModel by activityViewModels<LegacyLinksViewModel>()

    private fun state() = viewModel.state.value

    private val itemDecoration: PositionDividerItemDecoration by lazy(LazyThreadSafetyMode.NONE) {
        PositionDividerItemDecoration(requireContext(), displayMetrics())
    }

    /**
     * onCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        if (megaApi.rootNode == null) {
            return null
        }

        val view = setupUI(inflater, container)

        initAdapter()
        setupListViewConfiguration()

        return view
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
    }

    /**
     * activateActionMode
     */
    override fun activateActionMode() {
        if (megaNodeAdapter?.isMultipleSelect == true) return

        super.activateActionMode()
        actionMode =
            (requireActivity() as AppCompatActivity).startSupportActionMode(
                ActionBarCallBack(SharesTab.LINKS_TAB)
            )
    }

    override fun itemClick(position: Int) {
        val actualPosition = position - 1

        when {
            // select mode
            megaNodeAdapter?.isMultipleSelect == true -> {
                megaNodeAdapter?.toggleSelection(position)
                val selectedNodes = megaNodeAdapter?.selectedNodes
                if ((selectedNodes?.size ?: 0) > 0)
                    updateActionModeTitle()
            }

            // click on a folder
            state().nodes.getOrNull(actualPosition)?.isFolder == true ->
                navigateToFolder(state().nodes[actualPosition])

            // click on a file
            else -> state().nodes.getOrNull(actualPosition)?.let {
                openFile(it, Constants.LINKS_ADAPTER, actualPosition)
            }
        }
    }

    override fun navigateToFolder(node: MegaNode) {
        Timber.d("Is folder deep: %s", state().linksTreeDepth)

        recyclerView?.findFirstCompletelyVisibleItemPosition()
            ?.let { viewModel.pushToLastPositionStack(it) }
        viewModel.increaseLinksTreeDepth(node.handle)
        recyclerView?.scrollToPosition(0)
        checkScroll()
    }

    override fun onBackPressed(): Int {
        Timber.d("deepBrowserTree:%s", state().linksTreeDepth)

        if (megaNodeAdapter == null)
            return 0

        return when {
            state().linksTreeDepth == 1 -> {
                Timber.d("deepBrowserTree==1")
                viewModel.resetLinksTreeDepth()

                val lastVisiblePosition = viewModel.popLastPositionStack()

                lastVisiblePosition.takeIf { it > 0 }?.let {
                    recyclerView?.scrollToPosition(it)
                }

                recyclerView?.visibility = View.VISIBLE
                emptyListImageView?.visibility = View.GONE

                3
            }

            state().linksTreeDepth > 1 -> {
                Timber.d("deepTree>1")

                state().linksParentHandle?.let { parentHandle ->
                    recyclerView?.visibility = View.VISIBLE
                    emptyListImageView?.visibility = View.GONE
                    viewModel.decreaseLinksTreeDepth(parentHandle)

                    val lastVisiblePosition = viewModel.popLastPositionStack()

                    lastVisiblePosition.takeIf { it > 0 }?.let {
                        recyclerView?.scrollToPosition(it)
                    }
                }

                2
            }
            else -> {
                Timber.d("ELSE deepTree")
                viewModel.resetLinksTreeDepth()
                0
            }

        }

    }

    override fun showSortByPanel() {
        managerActivity?.showNewSortByPanel(ORDER_CLOUD)
    }

    override val viewerFrom: Int = Constants.VIEWER_FROM_LINKS
    override val currentSharesTab: SharesTab = SharesTab.LINKS_TAB
    override val sortOrder: SortOrder
        get() = state().sortOrder
    override val parentHandle: Long
        get() = state().linksHandle

    /**
     * Setup ViewModel observers
     */
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.state.collect {
                    Timber.d("Collect ui state")

                    // If the nodes are loading, don't display the UI
                    if (it.isLoading) {
                        recyclerView?.visibility = View.GONE
                        hideTabs(true)
                        return@collect
                    }

                    updateNodes(it.nodes)
                    hideTabs(!it.isFirstNavigationLevel())

                    managerActivity?.showFabButton()
                    managerActivity?.invalidateOptionsMenu()
                    managerActivity?.setToolbarTitle()

                    visibilityFastScroller()
                    hideActionMode()
                    setEmptyView(it.isInvalidHandle)
                }
            }
        }

        sortByHeaderViewModel.showDialogEvent.observe(viewLifecycleOwner, EventObserver {
            showSortByPanel()
        })
        sortByHeaderViewModel.orderChangeEvent.observe(viewLifecycleOwner, EventObserver {
            viewModel.refreshLinksSharesNode()
        })
    }

    /**
     * Sets up the Fragment to only run in List View configuration
     */
    private fun setupListViewConfiguration() {
        recyclerView?.run {
            switchToLinear()
            itemAnimator = Util.noChangeRecyclerViewItemAnimator()
            if (itemDecorationCount == 0) addItemDecoration(itemDecoration)
        }
    }

    /**
     * Initialize the adapter
     */
    private fun initAdapter() {
        if (megaNodeAdapter == null) {
            megaNodeAdapter = MegaNodeAdapter(
                requireActivity(),
                this,
                state().nodes,
                state().linksHandle,
                recyclerView,
                Constants.LINKS_ADAPTER,
                MegaNodeAdapter.ITEM_VIEW_TYPE_LIST,
                sortByHeaderViewModel
            )
        } else {
            megaNodeAdapter?.parentHandle = state().linksHandle
            megaNodeAdapter?.setListFragment(recyclerView)
        }

        megaNodeAdapter?.isMultipleSelect = false
        recyclerView?.adapter = megaNodeAdapter
    }

    /**
     * Update displayed nodes
     *
     * @param nodes the list of nodes to display
     */
    private fun updateNodes(nodes: List<MegaNode>) {
        val mutableListNodes = ArrayList(nodes)
        megaNodeAdapter?.setNodes(mutableListNodes)
    }

    /**
     * Hide/Show shares tab
     *
     * @param hide true if needs to hide shares tabs
     */
    private fun hideTabs(hide: Boolean) {
        managerActivity?.hideTabs(hide, SharesTab.LINKS_TAB)
    }

    /**
     * Set the empty view and message depending if the handle is valid or not
     *
     * @param isInvalidHandle true if the handle is invalid
     */
    private fun setEmptyView(isInvalidHandle: Boolean) {
        var textToShow: String? = null
        if (isInvalidHandle) {
            emptyListImageView?.let {
                setImageViewAlphaIfDark(
                    requireContext(),
                    it, ColorUtils.DARK_IMAGE_ALPHA
                )
            }
            emptyListImageView?.setImageResource(R.drawable.ic_zero_data_public_links)
            textToShow = requireContext().getString(R.string.context_empty_links)
        }
        setFinalEmptyView(textToShow)
    }

    private inner class ActionBarCallBack(currentTab: Tab) : BaseActionBarCallBack(currentTab) {
        override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
            super.onPrepareActionMode(actionMode, menu)
            val control = CloudStorageOptionControlUtil.Control()
            val areAllNotTakenDown = selected.areAllNotTakenDown()
            if (areAllNotTakenDown) {
                if (selected.size == 1) {
                    control.manageLink().setVisible(true).showAsAction =
                        MenuItem.SHOW_AS_ACTION_ALWAYS
                    control.removeLink().isVisible = true
                } else {
                    control.removeLink().setVisible(true).showAsAction =
                        MenuItem.SHOW_AS_ACTION_ALWAYS
                }
                control.shareOut().setVisible(true).showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                if (areAllFileNodesAndNotTakenDown(selected)) {
                    control.sendToChat().setVisible(true).showAsAction =
                        MenuItem.SHOW_AS_ACTION_ALWAYS
                }
                control.copy().isVisible = true
                if (control.alwaysActionCount() < CloudStorageOptionControlUtil.MAX_ACTION_COUNT) {
                    control.copy().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                } else {
                    control.copy().showAsAction = MenuItem.SHOW_AS_ACTION_NEVER
                }
            } else {
                control.saveToDevice().isVisible = false
            }
            if (selected.size == 1
                && megaApi.checkAccessErrorExtended(
                    selected[0],
                    MegaShare.ACCESS_FULL
                ).errorCode == MegaError.API_OK
            ) {
                control.rename().isVisible = true
                if (control.alwaysActionCount() < CloudStorageOptionControlUtil.MAX_ACTION_COUNT) {
                    control.rename().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                } else {
                    control.rename().showAsAction = MenuItem.SHOW_AS_ACTION_NEVER
                }
            }
            control.selectAll().isVisible = notAllNodesSelected()
            control.trash().isVisible = canMoveToRubbish(selected)

            // If there is at least one Backup folder found, then apply read-only restrictions
            // for all selected items
            if (selected.any { node -> megaApi.isInInbox(node) }) {
                control.rename().isVisible = false
                control.move().isVisible = false
                control.trash().isVisible = false
            }

            CloudStorageOptionControlUtil.applyControl(menu, control)
            return true
        }
    }
}