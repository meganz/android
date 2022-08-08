package mega.privacy.android.app.presentation.shares.outgoing

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
import mega.privacy.android.app.components.NewGridRecyclerView
import mega.privacy.android.app.main.adapters.MegaNodeAdapter
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.presentation.manager.model.Tab
import mega.privacy.android.app.presentation.shares.MegaNodeBaseFragment
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.ORDER_CLOUD
import mega.privacy.android.app.utils.Constants.ORDER_OTHERS
import mega.privacy.android.app.utils.MegaNodeUtil.areAllFileNodesAndNotTakenDown
import mega.privacy.android.app.utils.MegaNodeUtil.areAllNotTakenDown
import mega.privacy.android.app.utils.MegaNodeUtil.canMoveToRubbish
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import timber.log.Timber

/**
 * Outgoing shares page
 */
@AndroidEntryPoint
class OutgoingSharesFragment : MegaNodeBaseFragment() {

    private val viewModel: OutgoingSharesViewModel by activityViewModels()

    private fun state() = viewModel.state.value

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        if (megaApi.rootNode == null)
            return null

        val view =
            if (managerActivity?.isList == true) getListView(inflater, container)
            else getGridView(inflater, container)

        initAdapter()
        observe()

        return view
    }

    override fun activateActionMode() {
        if (adapter?.isMultipleSelect == true) return

        super.activateActionMode()
        actionMode =
            (requireActivity() as AppCompatActivity).startSupportActionMode(
                ActionBarCallBack(SharesTab.OUTGOING_TAB)
            )
    }

    override fun itemClick(position: Int) {
        val actualPosition = position - 1

        when {
            // select mode
            adapter?.isMultipleSelect == true -> {
                adapter?.toggleSelection(position)
                val selectedNodes = adapter?.selectedNodes
                if ((selectedNodes?.size ?: 0) > 0)
                    updateActionModeTitle()
            }

            // click on a folder
            state().nodes[actualPosition].isFolder ->
                navigateToFolder(state().nodes[actualPosition])

            // click on a file
            else ->
                openFile(state().nodes[actualPosition],
                    Constants.OUTGOING_SHARES_ADAPTER,
                    actualPosition)
        }
    }

    override fun navigateToFolder(node: MegaNode) {
        Timber.d("Is folder deep: %s", state().outgoingTreeDepth)

        val lastFirstVisiblePosition: Int = when {
            managerActivity?.isList == true ->
                mLayoutManager?.findFirstCompletelyVisibleItemPosition() ?: 0

            (recyclerView as NewGridRecyclerView).findFirstCompletelyVisibleItemPosition() == -1 ->
                (recyclerView as NewGridRecyclerView).findFirstVisibleItemPosition()

            else ->
                (recyclerView as NewGridRecyclerView).findFirstCompletelyVisibleItemPosition()
        }

        viewModel.pushToLastPositionStack(lastFirstVisiblePosition)
        viewModel.increaseOutgoingTreeDepth(node.handle)
        recyclerView?.scrollToPosition(0)
        checkScroll()
    }

    override fun onBackPressed(): Int {
        Timber.d("deepBrowserTree: %s", managerActivity?.deepBrowserTreeOutgoing)

        if (adapter == null)
            return 0

        managerActivity?.invalidateOptionsMenu()

        return when {
            state().outgoingTreeDepth == 1 -> {
                Timber.d("deepBrowserTree==1")
                viewModel.resetOutgoingTreeDepth()

                val lastVisiblePosition = viewModel.popLastPositionStack()

                lastVisiblePosition.takeIf { it > 0 }?.let {
                    if (managerActivity?.isList == true)
                        mLayoutManager?.scrollToPositionWithOffset(it, 0)
                    else
                        gridLayoutManager?.scrollToPositionWithOffset(it, 0)
                }

                recyclerView?.visibility = View.VISIBLE
                emptyImageView?.visibility = View.GONE
                emptyLinearLayout?.visibility = View.GONE
                3
            }

            state().outgoingTreeDepth > 1 -> {
                Timber.d("deepTree>1")

                state().outgoingParentHandle?.let { parentHandle ->
                    recyclerView?.visibility = View.VISIBLE
                    emptyImageView?.visibility = View.GONE
                    emptyLinearLayout?.visibility = View.GONE
                    viewModel.decreaseOutgoingTreeDepth(parentHandle)

                    val lastVisiblePosition = viewModel.popLastPositionStack()

                    lastVisiblePosition.takeIf { it > 0 }?.let {
                        if (managerActivity?.isList == true)
                            mLayoutManager?.scrollToPositionWithOffset(it, 0)
                        else
                            gridLayoutManager?.scrollToPositionWithOffset(it, 0)
                    }
                }

                2
            }

            else -> {
                Timber.d("ELSE deepTree")
                viewModel.resetOutgoingTreeDepth()
                0
            }
        }
    }

    /**
     * Method to update an item when a nickname is added, updated or removed from a contact.
     *
     * @param contactHandle Contact ID.
     */
    override fun updateContact(contactHandle: Long) {
        adapter?.updateItem(contactHandle)
    }

    override fun showSortByPanel() {
        val orderType = when (state().outgoingTreeDepth) {
            0 -> ORDER_OTHERS
            else -> ORDER_CLOUD
        }
        managerActivity?.showNewSortByPanel(orderType)
    }

    override val viewerFrom: Int
        get() = Constants.VIEWER_FROM_OUTGOING_SHARES
    override val intentOrder: Int
        get() = state().sortOrder
    override val currentSharesTab: SharesTab
        get() = SharesTab.OUTGOING_TAB
    override val parentHandle: Long
        get() = state().outgoingHandle

    /**
     * Observe viewModel
     */
    private fun observe() {
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
    }

    /**
     * Update displayed nodes
     *
     * @param nodes the list of nodes to display
     */
    private fun updateNodes(nodes: List<MegaNode>) {
        val mutableListNodes = ArrayList(nodes)
        adapter?.setNodes(mutableListNodes)
    }

    /**
     * Initialize the adapter
     */
    private fun initAdapter() {
        if (adapter == null) {
            adapter = MegaNodeAdapter(
                requireActivity(),
                this,
                state().nodes,
                state().outgoingHandle,
                recyclerView,
                Constants.OUTGOING_SHARES_ADAPTER,
                if (managerActivity?.isList == true) MegaNodeAdapter.ITEM_VIEW_TYPE_LIST
                else MegaNodeAdapter.ITEM_VIEW_TYPE_GRID,
                sortByHeaderViewModel
            )
        } else {
            adapter?.parentHandle = state().outgoingHandle
            adapter?.setListFragment(recyclerView)
        }

        if (managerActivity?.isList == false)
            gridLayoutManager?.spanSizeLookup =
                gridLayoutManager?.spanCount?.let { adapter?.getSpanSizeLookup(it) }

        adapter?.isMultipleSelect = false
        recyclerView?.adapter = adapter
    }

    /**
     * Hide/Show shares tab
     *
     * @param hide true if needs to hide shares tabs
     */
    private fun hideTabs(hide: Boolean) {
        managerActivity?.hideTabs(hide, SharesTab.OUTGOING_TAB)
    }

    /**
     * Set the empty view and message depending if the handle is valid or not
     *
     * @param isInvalidHandle true if the handle is invalid
     */
    private fun setEmptyView(isInvalidHandle: Boolean) {
        var textToShow: String? = null

        if (isInvalidHandle) {
            if (Util.isScreenInPortrait(requireContext())) {
                emptyImageView?.setImageResource(R.drawable.empty_outgoing_portrait)
            } else {
                emptyImageView?.setImageResource(R.drawable.empty_outgoing_landscape)
            }
            textToShow = requireContext().getString(R.string.context_empty_outgoing)
        }
        setFinalEmptyView(textToShow)
    }

    private inner class ActionBarCallBack(currentTab: Tab) : BaseActionBarCallBack(currentTab) {
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            super.onPrepareActionMode(mode, menu)
            val control = CloudStorageOptionControlUtil.Control()
            if (selected.size == 1 && !selected[0].isTakenDown) {
                if (megaApi.checkAccessErrorExtended(selected[0], MegaShare.ACCESS_OWNER).errorCode
                    == MegaError.API_OK
                ) {
                    if (selected[0].isExported) {
                        control.manageLink().setVisible(true).showAsAction =
                            MenuItem.SHOW_AS_ACTION_ALWAYS
                        control.removeLink().isVisible = true
                    } else {
                        control.link.setVisible(true).showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                    }
                }
                if (selected[0].isFolder) {
                    control.shareFolder().setVisible(true).showAsAction =
                        MenuItem.SHOW_AS_ACTION_ALWAYS
                }
                if (selected[0].isOutShare) {
                    control.removeShare().isVisible = true
                }
            }
            val areAllNotTakenDown = selected.areAllNotTakenDown()
            if (areAllNotTakenDown) {
                if (state().outgoingHandle == INVALID_HANDLE) {
                    control.removeShare().setVisible(true).showAsAction =
                        MenuItem.SHOW_AS_ACTION_ALWAYS
                }
                control.shareOut().setVisible(true).showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                if (state().outgoingTreeDepth > 0) {
                    if (areAllFileNodesAndNotTakenDown(selected)) {
                        control.sendToChat().setVisible(true).showAsAction =
                            MenuItem.SHOW_AS_ACTION_ALWAYS
                    }
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
                && megaApi.checkAccessErrorExtended(selected[0],
                    MegaShare.ACCESS_FULL).errorCode == MegaError.API_OK
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
            CloudStorageOptionControlUtil.applyControl(menu, control)
            return true
        }
    }
}