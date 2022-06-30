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
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaNodeUtil.areAllFileNodesAndNotTakenDown
import mega.privacy.android.app.utils.MegaNodeUtil.areAllNotTakenDown
import mega.privacy.android.app.utils.MegaNodeUtil.canMoveToRubbish
import mega.privacy.android.app.utils.SortUtil
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiJava
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
        Timber.d("onCreateView")
        if (megaApi.rootNode == null) {
            return null
        }
        managerActivity.showFabButton()
        val v: View
        if (managerActivity.isList) {
            v = getListView(inflater, container)
            if (adapter == null) {
                adapter = MegaNodeAdapter(context,
                    this,
                    nodes,
                    managerViewModel.state.value.outgoingParentHandle,
                    recyclerView,
                    Constants.OUTGOING_SHARES_ADAPTER,
                    MegaNodeAdapter.ITEM_VIEW_TYPE_LIST,
                    sortByHeaderViewModel)
            }
        } else {
            v = getGridView(inflater, container)
            if (adapter == null) {
                adapter = MegaNodeAdapter(context,
                    this,
                    nodes,
                    managerViewModel.state.value.outgoingParentHandle,
                    recyclerView,
                    Constants.OUTGOING_SHARES_ADAPTER,
                    MegaNodeAdapter.ITEM_VIEW_TYPE_GRID,
                    sortByHeaderViewModel)
            }
            gridLayoutManager.spanSizeLookup =
                adapter.getSpanSizeLookup(gridLayoutManager.spanCount)
        }
        adapter.parentHandle = managerViewModel.state.value.outgoingParentHandle
        adapter.setListFragment(recyclerView)
        if (managerViewModel.state.value.outgoingParentHandle == INVALID_HANDLE) {
            Timber.w("Parent Handle == -1")
            findNodes()
            adapter.parentHandle = INVALID_HANDLE
        } else {
            managerActivity.hideTabs(true, SharesTab.OUTGOING_TAB)
            val parentNode =
                megaApi.getNodeByHandle(managerViewModel.state.value.outgoingParentHandle)
            Timber.d("Parent Handle: %s", managerViewModel.state.value.outgoingParentHandle)
            nodes = megaApi.getChildren(parentNode, sortOrderManagement.getOrderCloud())
            adapter.setNodes(nodes)
        }
        managerActivity.setToolbarTitle()
        managerActivity.invalidateOptionsMenu()
        adapter.isMultipleSelect = false
        recyclerView.adapter = adapter
        visibilityFastScroller()
        setEmptyView()
        return v
    }

    override fun activateActionMode() {
        if (!adapter.isMultipleSelect) {
            super.activateActionMode()
            actionMode =
                (activity as AppCompatActivity?)?.startSupportActionMode(ActionBarCallBack(SharesTab.OUTGOING_TAB))
        }
    }

    override fun viewerFrom(): Int = Constants.VIEWER_FROM_OUTGOING_SHARES

    public override fun refresh() {
        Timber.d("Parent Handle: %s", managerViewModel.state.value.outgoingParentHandle)
        if (managerViewModel.state.value.outgoingParentHandle == -1L) {
            findNodes()
        } else {
            val n = megaApi.getNodeByHandle(managerViewModel.state.value.outgoingParentHandle)
            managerActivity.setToolbarTitle()
            nodes = megaApi.getChildren(n, sortOrderManagement.getOrderCloud())
            adapter.setNodes(nodes)
        }
        managerActivity.invalidateOptionsMenu()
        visibilityFastScroller()
        hideActionMode()
        setEmptyView()
    }

    fun findNodes() {
        val outNodeList = megaApi.outShares
        nodes.clear()
        var lastFolder: Long = -1
        for (k in outNodeList.indices) {
            if (outNodeList[k].user != null) {
                val mS = outNodeList[k]
                val node = megaApi.getNodeByHandle(mS.nodeHandle)
                if (lastFolder != node.handle) {
                    lastFolder = node.handle
                    nodes.add(node)
                }
            }
        }
        orderNodes()
    }

    public override fun itemClick(position: Int) {
        if (adapter.isMultipleSelect) {
            Timber.d("multiselect ON")
            adapter.toggleSelection(position)
            val selectedNodes = adapter.selectedNodes
            if (selectedNodes.size > 0) {
                updateActionModeTitle()
            }
        } else if (nodes[position].isFolder) {
            navigateToFolder(nodes[position])
        } else {
            //Is file
            openFile(nodes[position], Constants.OUTGOING_SHARES_ADAPTER, position)
        }
    }

    public override fun navigateToFolder(node: MegaNode) {
        managerActivity.hideTabs(true, SharesTab.OUTGOING_TAB)
        managerViewModel.increaseOutgoingTreeDepth()
        Timber.d("deepBrowserTree after clicking folder%s", managerActivity.deepBrowserTreeOutgoing)
        var lastFirstVisiblePosition: Int
        if (managerActivity.isList) {
            lastFirstVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition()
        } else {
            lastFirstVisiblePosition =
                (recyclerView as NewGridRecyclerView).findFirstCompletelyVisibleItemPosition()
            if (lastFirstVisiblePosition == -1) {
                lastFirstVisiblePosition =
                    (recyclerView as NewGridRecyclerView).findFirstVisibleItemPosition()
            }
        }
        lastPositionStack.push(lastFirstVisiblePosition)
        managerViewModel.setOutgoingParentHandle(node.handle)
        managerActivity.invalidateOptionsMenu()
        managerActivity.setToolbarTitle()
        nodes = megaApi.getChildren(node, sortOrderManagement.getOrderCloud())
        adapter.setNodes(nodes)
        recyclerView.scrollToPosition(0)
        visibilityFastScroller()
        setEmptyView()
        checkScroll()
        managerActivity.showFabButton()
    }

    override fun setNodes(nodes: ArrayList<MegaNode>) {
        this.nodes = nodes
        orderNodes()
    }

    private fun orderNodes() {
        if (sortOrderManagement.getOrderOthers() == MegaApiJava.ORDER_DEFAULT_DESC) {
            SortUtil.sortByNameDescending(nodes)
        } else {
            SortUtil.sortByNameAscending(nodes)
        }
        adapter.setNodes(nodes)
    }

    public override fun onBackPressed(): Int {
        Timber.d("deepBrowserTree: %s", managerActivity.deepBrowserTreeOutgoing)
        if (adapter == null) {
            return 0
        }
        managerViewModel.decreaseOutgoingTreeDepth()
        managerActivity.invalidateOptionsMenu()
        return if (managerActivity.deepBrowserTreeOutgoing == 0) {
            Timber.d("deepBrowserTree==0")
            //In the beginning of the navigation
            managerViewModel.setOutgoingParentHandle(INVALID_HANDLE)
            managerActivity.hideTabs(false, SharesTab.OUTGOING_TAB)
            managerActivity.setToolbarTitle()
            findNodes()
            visibilityFastScroller()
            var lastVisiblePosition = 0
            if (!lastPositionStack.empty()) {
                lastVisiblePosition = lastPositionStack.pop()
            }
            if (lastVisiblePosition >= 0) {
                if (managerActivity.isList) {
                    mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0)
                } else {
                    gridLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0)
                }
            }
            recyclerView.visibility = View.VISIBLE
            emptyImageView.visibility = View.GONE
            emptyLinearLayout.visibility = View.GONE
            managerActivity.showFabButton()
            3
        } else if (managerActivity.deepBrowserTreeOutgoing > 0) {
            Timber.d("Keep navigation")
            val parentNode =
                megaApi.getParentNode(megaApi.getNodeByHandle(managerViewModel.state.value.outgoingParentHandle))
            if (parentNode != null) {
                recyclerView.visibility = View.VISIBLE
                emptyImageView.visibility = View.GONE
                emptyLinearLayout.visibility = View.GONE
                managerViewModel.setOutgoingParentHandle(parentNode.handle)
                managerActivity.setToolbarTitle()
                managerActivity.invalidateOptionsMenu()
                nodes = megaApi.getChildren(parentNode, sortOrderManagement.getOrderCloud())
                adapter.setNodes(nodes)
                visibilityFastScroller()
                var lastVisiblePosition = 0
                if (!lastPositionStack.empty()) {
                    lastVisiblePosition = lastPositionStack.pop()
                }
                if (lastVisiblePosition >= 0) {
                    if (managerActivity.isList) {
                        mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0)
                    } else {
                        gridLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0)
                    }
                }
            }
            managerActivity.showFabButton()
            2
        } else {
            Timber.d("Back to Cloud")
            managerViewModel.resetOutgoingTreeDepth()
            0
        }
    }

    override fun setEmptyView() {
        var textToShow: String? = null
        val rootNode = megaApi.rootNode
        if (rootNode != null && rootNode.handle == managerViewModel.state.value.outgoingParentHandle
            || managerViewModel.state.value.outgoingParentHandle == -1L
        ) {
            if (Util.isScreenInPortrait(context)) {
                emptyImageView.setImageResource(R.drawable.empty_outgoing_portrait)
            } else {
                emptyImageView.setImageResource(R.drawable.empty_outgoing_landscape)
            }
            textToShow = context.getString(R.string.context_empty_outgoing)
        }
        setFinalEmptyView(textToShow)
    }

    /**
     * Method to update an item when a nickname is added, updated or removed from a contact.
     *
     * @param contactHandle Contact ID.
     */
    fun updateContact(contactHandle: Long) {
        adapter.updateItem(contactHandle)
    }

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
                if (managerViewModel.state.value.outgoingParentHandle == INVALID_HANDLE) {
                    control.removeShare().setVisible(true).showAsAction =
                        MenuItem.SHOW_AS_ACTION_ALWAYS
                }
                control.shareOut().setVisible(true).showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                if (managerViewModel.state.value.outgoingTreeDepth > 0) {
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