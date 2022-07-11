package mega.privacy.android.app.presentation.shares.outgoing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.components.NewGridRecyclerView
import mega.privacy.android.app.main.adapters.MegaNodeAdapter
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.presentation.manager.model.Tab
import mega.privacy.android.app.presentation.shares.MegaNodeBaseFragment
import mega.privacy.android.app.presentation.shares.managerState
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import mega.privacy.android.app.utils.Constants
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        if (megaApi.rootNode == null)
            return null

        val view =
            if (managerActivity.isList) getListView(inflater, container)
            else getGridView(inflater, container)

        initAdapter()
        refresh()

        return view
    }

    override fun activateActionMode() {
        if (!adapter.isMultipleSelect) {
            super.activateActionMode()
            actionMode =
                (activity as AppCompatActivity?)?.startSupportActionMode(ActionBarCallBack(SharesTab.OUTGOING_TAB))
        }
    }

    override fun refresh() {
        nodes = if (isInvalidParentHandle()) {
            megaApi.getOutShares(sortOrderManagement.getOrderOthers())
                .filter { it.user != null }
                .map { megaApi.getNodeByHandle(it.nodeHandle) }
                .distinctBy { it.handle }
        } else {
            val parentNode = megaApi.getNodeByHandle(managerState().outgoingParentHandle)
            megaApi.getChildren(parentNode, sortOrderManagement.getOrderCloud())
        }

        adapter.setNodes(nodes)

        managerActivity.showFabButton()
        managerActivity.invalidateOptionsMenu()
        managerActivity.setToolbarTitle()

        visibilityFastScroller()
        hideActionMode()
        setEmptyView()
    }

    override fun itemClick(position: Int) {
        when {
            // select mode
            adapter.isMultipleSelect -> {
                adapter.toggleSelection(position)
                val selectedNodes = adapter.selectedNodes
                if (selectedNodes.size > 0)
                    updateActionModeTitle()
            }

            // click on a folder
            nodes[position].isFolder ->
                navigateToFolder(nodes[position])

            // click on a file
            else ->
                openFile(nodes[position], Constants.OUTGOING_SHARES_ADAPTER, position)
        }
    }

    override fun navigateToFolder(node: MegaNode) {
        managerActivity.hideTabs(true, SharesTab.OUTGOING_TAB)
        managerViewModel.increaseOutgoingTreeDepth()
        Timber.d("deepBrowserTree after clicking folder%s", managerActivity.deepBrowserTreeOutgoing)

        val lastFirstVisiblePosition: Int = when {
            managerActivity.isList ->
                mLayoutManager.findFirstCompletelyVisibleItemPosition()

            (recyclerView as NewGridRecyclerView).findFirstCompletelyVisibleItemPosition() == -1 ->
                (recyclerView as NewGridRecyclerView).findFirstVisibleItemPosition()

            else ->
                (recyclerView as NewGridRecyclerView).findFirstCompletelyVisibleItemPosition()
        }

        lastPositionStack.push(lastFirstVisiblePosition)
        managerViewModel.setOutgoingParentHandle(node.handle)

        refresh()

        recyclerView.scrollToPosition(0)
        checkScroll()
    }

    override fun onBackPressed(): Int {
        Timber.d("deepBrowserTree: %s", managerActivity.deepBrowserTreeOutgoing)

        if (adapter == null)
            return 0

        managerViewModel.decreaseOutgoingTreeDepth()
        managerActivity.invalidateOptionsMenu()

        return when {
            managerState().outgoingTreeDepth == 0 -> {
                //In the beginning of the navigation
                managerViewModel.setOutgoingParentHandle(INVALID_HANDLE)
                managerActivity.hideTabs(false, SharesTab.OUTGOING_TAB)

                refresh()

                val lastVisiblePosition =
                    if (lastPositionStack.isNotEmpty())
                        lastPositionStack.pop()
                    else 0

                if (lastVisiblePosition >= 0) {
                    if (managerActivity.isList)
                        mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0)
                    else
                        gridLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0)
                }

                recyclerView.visibility = View.VISIBLE
                emptyImageView.visibility = View.GONE
                emptyLinearLayout.visibility = View.GONE
                3
            }

            managerState().outgoingTreeDepth > 0 -> {
                Timber.d("deepTree>0")

                val parentNode =
                    megaApi.getParentNode(
                        megaApi.getNodeByHandle(managerState().outgoingParentHandle)
                    )

                if (parentNode != null) {
                    recyclerView.visibility = View.VISIBLE
                    emptyImageView.visibility = View.GONE
                    emptyLinearLayout.visibility = View.GONE
                    managerViewModel.setOutgoingParentHandle(parentNode.handle)

                    refresh()

                    val lastVisiblePosition =
                        if (lastPositionStack.isNotEmpty())
                            lastPositionStack.pop()
                        else 0

                    if (lastVisiblePosition >= 0) {
                        if (managerActivity.isList)
                            mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0)
                        else
                            gridLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0)
                    }
                }

                2
            }

            else -> {
                Timber.d("ELSE deepTree")
                managerViewModel.resetOutgoingTreeDepth()
                0
            }
        }
    }

    override fun setNodes(nodes: List<MegaNode>) {}

    override fun setEmptyView() {
        var textToShow: String? = null

        if (isInvalidParentHandle()) {
            if (Util.isScreenInPortrait(requireContext())) {
                emptyImageView.setImageResource(R.drawable.empty_outgoing_portrait)
            } else {
                emptyImageView.setImageResource(R.drawable.empty_outgoing_landscape)
            }
            textToShow = requireContext().getString(R.string.context_empty_outgoing)
        }
        setFinalEmptyView(textToShow)
    }

    /**
     * Method to update an item when a nickname is added, updated or removed from a contact.
     *
     * @param contactHandle Contact ID.
     */
    override fun updateContact(contactHandle: Long) {
        adapter.updateItem(contactHandle)
    }

    override fun viewerFrom(): Int = Constants.VIEWER_FROM_OUTGOING_SHARES

    /**
     * Initialize the adapter
     */
    private fun initAdapter() {
        if (adapter == null) {
            adapter = MegaNodeAdapter(
                requireActivity(),
                this,
                nodes,
                managerState().outgoingParentHandle,
                recyclerView,
                Constants.OUTGOING_SHARES_ADAPTER,
                if (managerActivity.isList) MegaNodeAdapter.ITEM_VIEW_TYPE_LIST
                else MegaNodeAdapter.ITEM_VIEW_TYPE_GRID,
                sortByHeaderViewModel
            )
        } else {
            adapter.parentHandle = managerState().outgoingParentHandle
            adapter.setListFragment(recyclerView)
        }

        if (!managerActivity.isList)
            gridLayoutManager.spanSizeLookup =
                adapter.getSpanSizeLookup(gridLayoutManager.spanCount)

        adapter.isMultipleSelect = false

        recyclerView.adapter = adapter
    }

    /**
     * Check if the parent handle is valid
     *
     * @return true if the parent handle is valid
     */
    private fun isInvalidParentHandle(): Boolean =
        managerState().outgoingParentHandle == -1L ||
                managerState().outgoingParentHandle == INVALID_HANDLE ||
                megaApi.getNodeByHandle(managerState().outgoingParentHandle) == null

    private inner class ActionBarCallBack(currentTab: Tab?) : BaseActionBarCallBack(currentTab) {
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
                if (managerState().outgoingParentHandle == INVALID_HANDLE) {
                    control.removeShare().setVisible(true).showAsAction =
                        MenuItem.SHOW_AS_ACTION_ALWAYS
                }
                control.shareOut().setVisible(true).showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                if (managerState().outgoingTreeDepth > 0) {
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