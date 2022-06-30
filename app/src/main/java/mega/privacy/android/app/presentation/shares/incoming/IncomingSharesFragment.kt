package mega.privacy.android.app.presentation.shares.incoming

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
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.ColorUtils.setImageViewAlphaIfDark
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaNodeUtil.allHaveFullAccess
import mega.privacy.android.app.utils.MegaNodeUtil.areAllFileNodesAndNotTakenDown
import mega.privacy.android.app.utils.MegaNodeUtil.areAllNotTakenDown
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiJava
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        Timber.d("Parent Handle: %s", managerActivity.parentHandleIncoming)

        if (megaApi.rootNode == null)
            return null

        managerActivity.showFabButton()

        val v: View
        if (managerActivity.isList) {
            v = getListView(inflater, container)
            if (adapter == null) {
                adapter = MegaNodeAdapter(context,
                    this,
                    nodes,
                    managerActivity.parentHandleIncoming,
                    recyclerView,
                    Constants.INCOMING_SHARES_ADAPTER,
                    MegaNodeAdapter.ITEM_VIEW_TYPE_LIST,
                    sortByHeaderViewModel)
            }
        } else {
            v = getGridView(inflater, container)
            if (adapter == null) {
                adapter = MegaNodeAdapter(context,
                    this,
                    nodes,
                    managerActivity.parentHandleIncoming,
                    recyclerView,
                    Constants.INCOMING_SHARES_ADAPTER,
                    MegaNodeAdapter.ITEM_VIEW_TYPE_GRID,
                    sortByHeaderViewModel)
            }
            gridLayoutManager.spanSizeLookup =
                adapter.getSpanSizeLookup(gridLayoutManager.spanCount)
        }
        adapter.parentHandle = managerActivity.parentHandleIncoming
        adapter.setListFragment(recyclerView)

        if (managerActivity.parentHandleIncoming == MegaApiJava.INVALID_HANDLE) {
            Timber.w("ParentHandle -1")
            findNodes()
        } else {
            managerActivity.hideTabs(true, SharesTab.INCOMING_TAB)
            val parentNode = megaApi.getNodeByHandle(managerActivity.parentHandleIncoming)
            Timber.d("ParentHandle to find children: %s", managerActivity.parentHandleIncoming)
            nodes = megaApi.getChildren(parentNode, sortOrderManagement.getOrderCloud())
            adapter.setNodes(nodes)
        }

        managerActivity.invalidateOptionsMenu()
        adapter.isMultipleSelect = false
        recyclerView.adapter = adapter
        visibilityFastScroller()
        setEmptyView()
        selectNewlyAddedNodes()
        Timber.d("Deep browser tree: %s", managerActivity.deepBrowserTreeIncoming)
        return v
    }

    override fun activateActionMode() {
        if (!adapter.isMultipleSelect) {
            super.activateActionMode()
            actionMode =
                (activity as AppCompatActivity?)?.startSupportActionMode(ActionBarCallBack(SharesTab.INCOMING_TAB))
        }
    }

    override fun viewerFrom(): Int {
        return Constants.VIEWER_FROM_INCOMING_SHARES
    }

    public override fun refresh() {
        val parentNode: MegaNode
        if (managerActivity.parentHandleIncoming == -1L || megaApi.getNodeByHandle(managerActivity.parentHandleIncoming) == null) {
            findNodes()
        } else {
            parentNode = megaApi.getNodeByHandle(managerActivity.parentHandleIncoming)
            nodes = megaApi.getChildren(parentNode, sortOrderManagement.getOrderCloud())
            adapter.setNodes(nodes)
        }
        managerActivity.invalidateOptionsMenu()
        visibilityFastScroller()
        hideActionMode()
        setEmptyView()
    }

    public override fun itemClick(position: Int) {
        if (adapter.isMultipleSelect) {
            adapter.toggleSelection(position)
            val selectedNodes = adapter.selectedNodes
            if (selectedNodes.size > 0) {
                updateActionModeTitle()
            }
        } else if (nodes[position].isFolder) {
            navigateToFolder(nodes[position])
        } else {
            openFile(nodes[position], Constants.INCOMING_SHARES_ADAPTER, position)
        }
    }

    public override fun navigateToFolder(node: MegaNode) {
        managerActivity.hideTabs(true, SharesTab.INCOMING_TAB)
        managerActivity.increaseDeepBrowserTreeIncoming()
        Timber.d("Is folder deep: %s", managerActivity.deepBrowserTreeIncoming)
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
        managerActivity.parentHandleIncoming = node.handle
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

    fun findNodes() {
        nodes = megaApi.getInShares(sortOrderManagement.getOrderOthers())
        adapter.setNodes(nodes)
        setEmptyView()
    }

    public override fun onBackPressed(): Int {
        Timber.d("deepBrowserTree:%s", managerActivity.deepBrowserTreeIncoming)
        if (adapter == null) {
            return 0
        }
        return if (managerActivity.comesFromNotifications && managerActivity.comesFromNotificationsLevel == managerActivity.deepBrowserTreeIncoming) {
            managerActivity.restoreSharesAfterComingFromNotifications()
            4
        } else {
            managerActivity.decreaseDeepBrowserTreeIncoming()
            managerActivity.invalidateOptionsMenu()
            if (managerActivity.deepBrowserTreeIncoming == 0) {
                //In the beginning of the navigation
                Timber.d("deepBrowserTree==0")
                managerActivity.parentHandleIncoming = MegaApiJava.INVALID_HANDLE
                managerActivity.hideTabs(false, SharesTab.INCOMING_TAB)
                managerActivity.setToolbarTitle()
                findNodes()
                visibilityFastScroller()
                recyclerView.visibility = View.VISIBLE
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
                managerActivity.showFabButton()
                emptyImageView.visibility = View.GONE
                emptyLinearLayout.visibility = View.GONE
                3
            } else if (managerActivity.deepBrowserTreeIncoming > 0) {
                Timber.d("deepTree>0")
                val parentNode =
                    megaApi.getParentNode(megaApi.getNodeByHandle(managerActivity.parentHandleIncoming))
                if (parentNode != null) {
                    recyclerView.visibility = View.VISIBLE
                    emptyImageView.visibility = View.GONE
                    emptyLinearLayout.visibility = View.GONE
                    managerActivity.parentHandleIncoming = parentNode.handle
                    managerActivity.invalidateOptionsMenu()
                    managerActivity.setToolbarTitle()
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
                Timber.d("ELSE deepTree")
                managerActivity.deepBrowserTreeIncoming = 0
                0
            }
        }
    }

    override fun setNodes(nodes: ArrayList<MegaNode>) {
        this.nodes = nodes
        adapter.setNodes(nodes)
    }

    override fun setEmptyView() {
        var textToShow: String? = null
        if (megaApi.rootNode.handle == managerActivity.parentHandleIncoming
            || managerActivity.parentHandleIncoming == -1L
        ) {
            setImageViewAlphaIfDark(context, emptyImageView, ColorUtils.DARK_IMAGE_ALPHA)
            if (Util.isScreenInPortrait(context)) {
                emptyImageView.setImageResource(R.drawable.incoming_shares_empty)
            } else {
                emptyImageView.setImageResource(R.drawable.incoming_empty_landscape)
            }
            textToShow = context.getString(R.string.context_empty_incoming)
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

    /**
     * If user navigates from notification about new nodes added to shared folder select all nodes and scroll to the first node in the list
     */
    private fun selectNewlyAddedNodes() {
        val positions = managerActivity.getPositionsList(nodes)
        if (positions.isNotEmpty()) {
            val firstPosition = Collections.min(positions)
            activateActionMode()
            for (position in positions) {
                if (adapter.isMultipleSelect) {
                    adapter.toggleSelection(position)
                }
            }
            val selectedNodes = adapter.selectedNodes
            if (selectedNodes.size > 0) {
                updateActionModeTitle()
            }
            recyclerView.scrollToPosition(firstPosition)
        }
    }

    private inner class ActionBarCallBack(currentTab: Tab?) : BaseActionBarCallBack(currentTab) {
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            super.onPrepareActionMode(mode, menu)
            val control = CloudStorageOptionControlUtil.Control()
            if (managerActivity.getDeepBrowserTreeIncoming() == 0) {
                control.leaveShare().setVisible(true).showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
            } else if (areAllFileNodesAndNotTakenDown(selected)) {
                control.sendToChat().setVisible(true).showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
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
            if (managerActivity.getDeepBrowserTreeIncoming() > 0 && selected.size > 0 && allHaveFullAccess(
                    selected)
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
            control.trash().isVisible = (managerActivity.getDeepBrowserTreeIncoming() > 0
                    && allHaveFullAccess(selected))
            CloudStorageOptionControlUtil.applyControl(menu, control)
            return true
        }
    }
}