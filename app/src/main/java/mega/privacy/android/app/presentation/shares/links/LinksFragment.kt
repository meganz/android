package mega.privacy.android.app.presentation.shares.links

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
import mega.privacy.android.app.main.adapters.MegaNodeAdapter
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.presentation.manager.model.Tab
import mega.privacy.android.app.presentation.shares.MegaNodeBaseFragment
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.ColorUtils.setImageViewAlphaIfDark
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaNodeUtil.areAllFileNodesAndNotTakenDown
import mega.privacy.android.app.utils.MegaNodeUtil.areAllNotTakenDown
import mega.privacy.android.app.utils.MegaNodeUtil.canMoveToRubbish
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import timber.log.Timber

/**
 * Links shares page
 */
@AndroidEntryPoint
class LinksFragment : MegaNodeBaseFragment() {

    companion object {

        @JvmStatic
        fun getLinksOrderCloud(orderCloud: Int, isFirstNavigationLevel: Boolean): Int {
            return if (!isFirstNavigationLevel) {
                orderCloud
            } else when (orderCloud) {
                MegaApiJava.ORDER_MODIFICATION_ASC -> MegaApiJava.ORDER_LINK_CREATION_ASC
                MegaApiJava.ORDER_MODIFICATION_DESC -> MegaApiJava.ORDER_LINK_CREATION_DESC
                else -> orderCloud
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        if (megaApi.rootNode == null) {
            return null
        }
        val v = getListView(inflater, container)
        if (adapter == null) {
            adapter = MegaNodeAdapter(requireActivity(),
                this,
                nodes,
                managerViewModel.state.value.linksParentHandle,
                recyclerView,
                Constants.LINKS_ADAPTER,
                MegaNodeAdapter.ITEM_VIEW_TYPE_LIST,
                sortByHeaderViewModel)
        }
        adapter.setListFragment(recyclerView)
        if (managerViewModel.state.value.linksParentHandle == MegaApiJava.INVALID_HANDLE) {
            Timber.w("ParentHandle -1")
            findNodes()
            adapter.parentHandle = MegaApiJava.INVALID_HANDLE
        } else {
            managerActivity.hideTabs(true, SharesTab.LINKS_TAB)
            val parentNode = megaApi.getNodeByHandle(managerViewModel.state.value.linksParentHandle)
            Timber.d("ParentHandle to find children: %s",
                managerViewModel.state.value.linksParentHandle)
            nodes = megaApi.getChildren(parentNode, getLinksOrderCloud(
                sortOrderManagement.getOrderCloud(),
                managerViewModel.state.value.isFirstNavigationLevel))
            adapter.setNodes(nodes)
        }
        adapter.isMultipleSelect = false
        recyclerView.adapter = adapter
        return v
    }

    override fun activateActionMode() {
        if (!adapter.isMultipleSelect) {
            super.activateActionMode()
            actionMode =
                (activity as AppCompatActivity?)?.startSupportActionMode(ActionBarCallBack(SharesTab.LINKS_TAB))
        }
    }

    override fun viewerFrom(): Int = Constants.VIEWER_FROM_LINKS

    private fun findNodes() {
        setNodes(
            megaApi.getPublicLinks(
                getLinksOrderCloud(
                    sortOrderManagement.getOrderCloud(),
                    managerViewModel.state.value.isFirstNavigationLevel
                )
            )
        )
    }

    override fun setNodes(nodes: ArrayList<MegaNode>) {
        this.nodes = nodes
        adapter.setNodes(nodes)
        setEmptyView()
        visibilityFastScroller()
    }

    override fun setEmptyView() {
        var textToShow: String? = null
        if (megaApi.rootNode.handle == managerViewModel.state.value.linksParentHandle
            || managerViewModel.state.value.linksParentHandle == -1L
        ) {
            setImageViewAlphaIfDark(requireContext(), emptyImageView, ColorUtils.DARK_IMAGE_ALPHA)
            emptyImageView.setImageResource(R.drawable.ic_zero_data_public_links)
            textToShow = requireContext().getString(R.string.context_empty_links)
        }
        setFinalEmptyView(textToShow)
    }

    public override fun onBackPressed(): Int {
        if (adapter == null
            || managerViewModel.state.value.linksParentHandle == MegaApiJava.INVALID_HANDLE
            || managerViewModel.state.value.linksTreeDepth <= 0
        )
            return 0

        managerViewModel.decreaseLinksTreeDepth()
        if (managerViewModel.state.value.linksTreeDepth == 0) {
            managerViewModel.setLinksParentHandle(MegaApiJava.INVALID_HANDLE)
            managerActivity.hideTabs(false, SharesTab.LINKS_TAB)
            findNodes()
        } else if (managerViewModel.state.value.linksTreeDepth > 0) {
            var parentNodeLinks =
                megaApi.getNodeByHandle(managerViewModel.state.value.linksParentHandle)
            if (parentNodeLinks != null) {
                parentNodeLinks = megaApi.getParentNode(parentNodeLinks)
                if (parentNodeLinks != null) {
                    managerViewModel.setLinksParentHandle(parentNodeLinks.handle)
                    setNodes(megaApi.getChildren(parentNodeLinks, getLinksOrderCloud(
                        sortOrderManagement.getOrderCloud(),
                        managerViewModel.state.value.isFirstNavigationLevel)))
                }
            }
        } else {
            managerViewModel.resetLinksTreeDepth()
        }
        var lastVisiblePosition = 0
        if (!lastPositionStack.empty()) {
            lastVisiblePosition = lastPositionStack.pop()
        }
        if (lastVisiblePosition >= 0) {
            mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0)
        }
        managerActivity.showFabButton()
        managerActivity.setToolbarTitle()
        managerActivity.invalidateOptionsMenu()
        return 1
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
            openFile(nodes[position], Constants.LINKS_ADAPTER, position)
        }
    }

    public override fun navigateToFolder(node: MegaNode) {
        lastPositionStack.push(mLayoutManager.findFirstCompletelyVisibleItemPosition())
        managerActivity.hideTabs(true, SharesTab.LINKS_TAB)
        managerViewModel.increaseLinksTreeDepth()
        managerViewModel.setLinksParentHandle(node.handle)
        managerActivity.invalidateOptionsMenu()
        managerActivity.setToolbarTitle()
        setNodes(megaApi.getChildren(node, getLinksOrderCloud(
            sortOrderManagement.getOrderCloud(),
            managerViewModel.state.value.isFirstNavigationLevel)))
        recyclerView.scrollToPosition(0)
        checkScroll()
        managerActivity.showFabButton()
    }

    public override fun refresh() {
        hideActionMode()
        if (managerViewModel.state.value.linksParentHandle == MegaApiJava.INVALID_HANDLE
            || megaApi.getNodeByHandle(managerViewModel.state.value.linksParentHandle) == null
        ) {
            findNodes()
        } else {
            val parentNodeLinks =
                megaApi.getNodeByHandle(managerViewModel.state.value.linksParentHandle)
            setNodes(megaApi.getChildren(parentNodeLinks, getLinksOrderCloud(
                sortOrderManagement.getOrderCloud(),
                managerViewModel.state.value.isFirstNavigationLevel)))
        }
    }

    private inner class ActionBarCallBack(currentTab: Tab?) : BaseActionBarCallBack(currentTab) {
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            super.onPrepareActionMode(mode, menu)
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