package mega.privacy.android.app.presentation.shares.incoming

import android.content.Intent
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
import androidx.recyclerview.widget.DefaultItemAnimator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.CustomizedGridLayoutManager
import mega.privacy.android.app.components.PositionDividerItemDecoration
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.main.adapters.MegaNodeAdapter
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.presentation.contact.authenticitycredendials.AuthenticityCredentialsActivity
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.presentation.manager.model.Tab
import mega.privacy.android.app.presentation.shares.MegaNodeBaseFragment
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.ColorUtils.setImageViewAlphaIfDark
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.ORDER_CLOUD
import mega.privacy.android.app.utils.Constants.ORDER_OTHERS
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.MegaNodeUtil.allHaveFullAccess
import mega.privacy.android.app.utils.MegaNodeUtil.areAllFileNodesAndNotTakenDown
import mega.privacy.android.app.utils.MegaNodeUtil.areAllNotTakenDown
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.displayMetrics
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.preference.ViewType
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import timber.log.Timber
import java.util.Collections

/**
 * Incoming shares page
 */
@AndroidEntryPoint
class IncomingSharesFragment : MegaNodeBaseFragment() {
    private val viewModel by activityViewModels<IncomingSharesViewModel>()

    private fun incomingSharesState() = viewModel.state.value

    private lateinit var nodeController: NodeController

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

        if (megaApi.rootNode == null)
            return null

        val view = setupUI(inflater, container)
        setupAdapter()
        selectNewlyAddedNodes()
        switchViewType()

        return view
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        nodeController = NodeController(requireActivity())
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
                ActionBarCallBack(SharesTab.INCOMING_TAB)
            )
    }

    override fun itemClick(position: Int) {
        val actualPosition = position - 1
        val node = incomingSharesState().nodes.getOrNull(actualPosition)?.first
        val shareData = incomingSharesState().nodes.getOrNull(actualPosition)?.second
        when {
            shareData?.isVerified == false -> {
                Intent(requireActivity(), AuthenticityCredentialsActivity::class.java).apply {
                    putExtra(
                        Constants.IS_NODE_INCOMING,
                        nodeController.nodeComesFromIncoming(node)
                    )
                    putExtra(
                        Constants.EMAIL,
                        node?.let { ContactUtil.getContactEmailDB(it.owner) }
                    )
                    requireActivity().startActivity(this)
                }
            }
            // select mode
            megaNodeAdapter?.isMultipleSelect == true -> {
                megaNodeAdapter?.toggleSelection(position)
                val selectedNodes = megaNodeAdapter?.selectedNodes
                if ((selectedNodes?.size ?: 0) > 0)
                    updateActionModeTitle()
            }

            // click on a folder
            node?.isFolder == true ->
                navigateToFolder(node)

            // click on a file
            else ->
                node?.let {
                    openFile(
                        it,
                        Constants.INCOMING_SHARES_ADAPTER,
                        actualPosition
                    )
                }
        }
    }

    override fun navigateToFolder(node: MegaNode) {
        Timber.d("Is folder deep: %s", incomingSharesState().incomingTreeDepth)

        val lastFirstVisiblePosition: Int = when {
            incomingSharesState().currentViewType == ViewType.LIST ->
                recyclerView?.findFirstCompletelyVisibleItemPosition() ?: 0

            recyclerView?.findFirstCompletelyVisibleItemPosition() == -1 ->
                recyclerView?.findFirstVisibleItemPosition() ?: 0

            else ->
                recyclerView?.findFirstCompletelyVisibleItemPosition() ?: 0
        }

        viewModel.pushToLastPositionStack(lastFirstVisiblePosition)
        viewModel.increaseIncomingTreeDepth(node.handle)
        recyclerView?.scrollToPosition(0)
        checkScroll()
    }

    override fun onBackPressed(): Int {

        if (megaNodeAdapter == null)
            return 0

        if (managerActivity?.comesFromNotifications == true && managerActivity?.comesFromNotificationsLevel == incomingSharesState().incomingTreeDepth) {
            managerActivity?.restoreSharesAfterComingFromNotifications()
            return 4
        }

        managerActivity?.invalidateOptionsMenu()

        return when {
            incomingSharesState().incomingTreeDepth == 1 -> {
                Timber.d("deepBrowserTree==1")
                viewModel.resetIncomingTreeDepth()

                val lastVisiblePosition = viewModel.popLastPositionStack()

                lastVisiblePosition.takeIf { it > 0 }?.let {
                    recyclerView?.scrollToPosition(it)
                }

                recyclerView?.visibility = View.VISIBLE
                emptyListImageView?.visibility = View.GONE
                3
            }

            incomingSharesState().incomingTreeDepth > 1 -> {
                Timber.d("deepTree>1")

                incomingSharesState().incomingParentHandle?.let { parentHandle ->
                    recyclerView?.visibility = View.VISIBLE
                    emptyListImageView?.visibility = View.GONE
                    viewModel.decreaseIncomingTreeDepth(parentHandle)

                    val lastVisiblePosition = viewModel.popLastPositionStack()

                    lastVisiblePosition.takeIf { it > 0 }?.let {
                        recyclerView?.scrollToPosition(it)
                    }
                }

                2
            }

            else -> {
                Timber.d("ELSE deepTree")
                viewModel.resetIncomingTreeDepth()
                0
            }
        }
    }

    override fun showSortByPanel() {
        val orderType = when (incomingSharesState().incomingTreeDepth) {
            0 -> ORDER_OTHERS
            else -> ORDER_CLOUD
        }
        managerActivity?.showNewSortByPanel(orderType)
    }

    override val viewerFrom: Int = Constants.VIEWER_FROM_INCOMING_SHARES
    override val currentSharesTab: SharesTab = SharesTab.INCOMING_TAB
    override val sortOrder: SortOrder
        get() = incomingSharesState().sortOrder
    override val parentHandle: Long
        get() = incomingSharesState().incomingHandle

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
                    hideTabs(!it.isFirstNavigationLevel())

                    managerActivity?.showFabButton()
                    managerActivity?.invalidateOptionsMenu()
                    managerActivity?.setToolbarTitle()

                    visibilityFastScroller()
                    hideActionMode()
                    updateNodes(it.nodes)
                    setEmptyView(it.isInvalidHandle)
                }
            }
        }

        sortByHeaderViewModel.showDialogEvent.observe(viewLifecycleOwner, EventObserver {
            showSortByPanel()
        })
        sortByHeaderViewModel.orderChangeEvent.observe(viewLifecycleOwner, EventObserver {
            viewModel.refreshIncomingSharesNode()
        })

        viewLifecycleOwner.collectFlow(sortByHeaderViewModel.state) { state ->
            updateViewType(state.viewType)
        }
    }

    /**
     * Updates the View Type of this Fragment
     *
     * Changing the View Type will cause the scroll position to be lost. To avoid that, only
     * refresh the contents when the new View Type is different from the original View Type
     *
     * @param viewType The new View Type received from [SortByHeaderViewModel]
     */
    private fun updateViewType(viewType: ViewType) {
        if (viewType != incomingSharesState().currentViewType) {
            viewModel.setCurrentViewType(viewType)
            switchViewType()
        }
    }

    /**
     * Switches how items in the [MegaNodeAdapter] are being displayed, based on the current
     * [ViewType] in [IncomingSharesViewModel]
     */
    private fun switchViewType() {
        recyclerView?.run {
            when (incomingSharesState().currentViewType) {
                ViewType.LIST -> {
                    switchToLinear()
                    itemAnimator = Util.noChangeRecyclerViewItemAnimator()
                    if (itemDecorationCount == 0) addItemDecoration(itemDecoration)
                    megaNodeAdapter?.adapterType = MegaNodeAdapter.ITEM_VIEW_TYPE_LIST
                }
                ViewType.GRID -> {
                    switchBackToGrid()
                    itemAnimator = DefaultItemAnimator()
                    removeItemDecoration(itemDecoration)
                    (layoutManager as CustomizedGridLayoutManager).apply {
                        spanSizeLookup = megaNodeAdapter?.getSpanSizeLookup(spanCount)
                    }
                    megaNodeAdapter?.adapterType = MegaNodeAdapter.ITEM_VIEW_TYPE_GRID
                }
            }
        }
    }

    /**
     * Update displayed nodes
     *
     * @param nodes the list of nodes to display
     */
    private fun updateNodes(nodes: List<Pair<MegaNode, ShareData?>>) {
        val mutableListNodes = nodes.map { it.first }
        val mutableListShareData = nodes.map { it.second }
        megaNodeAdapter?.setNodesWithShareData(mutableListNodes, mutableListShareData)
    }

    /**
     * Initialize the adapter
     */
    private fun setupAdapter() {
        megaNodeAdapter = MegaNodeAdapter(
            requireActivity(),
            this,
            incomingSharesState().nodes.map { it.first },
            incomingSharesState().incomingHandle,
            recyclerView,
            Constants.INCOMING_SHARES_ADAPTER,
            if (incomingSharesState().currentViewType == ViewType.LIST) MegaNodeAdapter.ITEM_VIEW_TYPE_LIST
            else MegaNodeAdapter.ITEM_VIEW_TYPE_GRID,
            sortByHeaderViewModel
        ).also {
            it.isMultipleSelect = false
            recyclerView?.adapter = it
        }
    }

    /**
     * Hide/Show shares tab
     *
     * @param hide true if needs to hide shares tabs
     */
    private fun hideTabs(hide: Boolean) {
        managerActivity?.hideTabs(hide, SharesTab.INCOMING_TAB)
    }

    /**
     * If user navigates from notification about new nodes added to shared folder select all nodes and scroll to the first node in the list
     */
    private fun selectNewlyAddedNodes() {
        val positions =
            managerActivity?.getPositionsList(incomingSharesState().nodes.map { it.first })
        if (!positions.isNullOrEmpty()) {
            val firstPosition = Collections.min(positions)
            activateActionMode()
            for (position in positions) {
                if (megaNodeAdapter?.isMultipleSelect == true) {
                    megaNodeAdapter?.toggleSelection(position)
                }
            }
            val selectedNodes = megaNodeAdapter?.selectedNodes
            if ((selectedNodes?.size ?: 0) > 0) {
                updateActionModeTitle()
            }
            recyclerView?.scrollToPosition(firstPosition)
        }
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
            if (Util.isScreenInPortrait(requireContext())) {
                emptyListImageView?.setImageResource(R.drawable.incoming_shares_empty)
            } else {
                emptyListImageView?.setImageResource(R.drawable.incoming_empty_landscape)
            }
            textToShow = requireContext().getString(R.string.context_empty_incoming)
        }
        setFinalEmptyView(textToShow)
    }

    private inner class ActionBarCallBack(currentTab: Tab) : BaseActionBarCallBack(currentTab) {
        override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
            super.onPrepareActionMode(actionMode, menu)
            val control = CloudStorageOptionControlUtil.Control()

            if (incomingSharesState().incomingTreeDepth == 0) {
                control.leaveShare().setVisible(true).showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
            } else if (areAllFileNodesAndNotTakenDown(selected)) {
                control.sendToChat().setVisible(true).showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
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

            if (incomingSharesState().incomingTreeDepth > 0 && selected.isNotEmpty() && allHaveFullAccess(
                    selected
                )
            ) {
                control.move().isVisible = true
                if (control.alwaysActionCount() < CloudStorageOptionControlUtil.MAX_ACTION_COUNT) {
                    control.move().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                } else {
                    control.move().showAsAction = MenuItem.SHOW_AS_ACTION_NEVER
                }
            }

            if (selected.areAllNotTakenDown()) {
                control.copy().isVisible = true
                if (control.alwaysActionCount() < CloudStorageOptionControlUtil.MAX_ACTION_COUNT) {
                    control.copy().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                } else {
                    control.copy().showAsAction = MenuItem.SHOW_AS_ACTION_NEVER
                }
            } else {
                control.saveToDevice().isVisible = false
            }

            control.selectAll().isVisible = notAllNodesSelected()
            control.trash().isVisible = (incomingSharesState().incomingTreeDepth > 0
                    && allHaveFullAccess(selected))
            CloudStorageOptionControlUtil.applyControl(menu, control)
            return true
        }
    }
}
