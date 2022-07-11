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
import mega.privacy.android.app.presentation.shares.managerState
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.ColorUtils.setImageViewAlphaIfDark
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaNodeUtil.allHaveFullAccess
import mega.privacy.android.app.utils.MegaNodeUtil.areAllFileNodesAndNotTakenDown
import mega.privacy.android.app.utils.MegaNodeUtil.areAllNotTakenDown
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
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

        if (megaApi.rootNode == null)
            return null

        val view =
            if (managerActivity.isList) getListView(inflater, container)
            else getGridView(inflater, container)

        initAdapter()
        refresh()
        selectNewlyAddedNodes()

        return view
    }

    override fun activateActionMode() {
        if (adapter.isMultipleSelect) return

        super.activateActionMode()
        actionMode =
            (requireActivity() as AppCompatActivity).startSupportActionMode(
                ActionBarCallBack(SharesTab.INCOMING_TAB)
            )
    }

    override fun refresh() {
        nodes = if (isInvalidParentHandle()) {
            megaApi.getInShares(sortOrderManagement.getOrderOthers())
        } else {
            val parentNode = megaApi.getNodeByHandle(managerState().incomingParentHandle)
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
                openFile(nodes[position], Constants.INCOMING_SHARES_ADAPTER, position)
        }
    }

    override fun navigateToFolder(node: MegaNode) {
        managerActivity.hideTabs(true, SharesTab.INCOMING_TAB)
        managerViewModel.increaseIncomingTreeDepth()
        Timber.d("Is folder deep: %s", managerState().incomingTreeDepth)

        val lastFirstVisiblePosition: Int = when {
            managerActivity.isList ->
                mLayoutManager.findFirstCompletelyVisibleItemPosition()

            (recyclerView as NewGridRecyclerView).findFirstCompletelyVisibleItemPosition() == -1 ->
                (recyclerView as NewGridRecyclerView).findFirstVisibleItemPosition()

            else ->
                (recyclerView as NewGridRecyclerView).findFirstCompletelyVisibleItemPosition()
        }

        lastPositionStack.push(lastFirstVisiblePosition)
        managerViewModel.setIncomingParentHandle(node.handle)

        refresh()

        recyclerView.scrollToPosition(0)
        checkScroll()
    }

    override fun onBackPressed(): Int {
        Timber.d("deepBrowserTree:%s", managerState().incomingTreeDepth)

        if (adapter == null)
            return 0

        if (managerActivity.comesFromNotifications && managerActivity.comesFromNotificationsLevel == managerState().incomingTreeDepth) {
            managerActivity.restoreSharesAfterComingFromNotifications()
            return 4
        }

        managerViewModel.decreaseIncomingTreeDepth()
        managerActivity.invalidateOptionsMenu()

        return when {
            managerState().incomingTreeDepth == 0 -> {
                //In the beginning of the navigation
                Timber.d("deepBrowserTree==0")
                managerViewModel.setIncomingParentHandle(INVALID_HANDLE)
                managerActivity.hideTabs(false, SharesTab.INCOMING_TAB)

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

            managerState().incomingTreeDepth > 0 -> {
                Timber.d("deepTree>0")
                val parentNode =
                    megaApi.getParentNode(
                        megaApi.getNodeByHandle(managerState().incomingParentHandle)
                    )

                if (parentNode != null) {
                    recyclerView.visibility = View.VISIBLE
                    emptyImageView.visibility = View.GONE
                    emptyLinearLayout.visibility = View.GONE
                    managerViewModel.setIncomingParentHandle(parentNode.handle)

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
                managerViewModel.resetIncomingTreeDepth()
                0
            }
        }
    }

    override fun setNodes(nodes: List<MegaNode>) {}

    override fun setEmptyView() {
        var textToShow: String? = null

        if (isInvalidParentHandle()) {
            setImageViewAlphaIfDark(requireContext(), emptyImageView, ColorUtils.DARK_IMAGE_ALPHA)
            if (Util.isScreenInPortrait(requireContext())) {
                emptyImageView.setImageResource(R.drawable.incoming_shares_empty)
            } else {
                emptyImageView.setImageResource(R.drawable.incoming_empty_landscape)
            }
            textToShow = requireContext().getString(R.string.context_empty_incoming)
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

    override fun viewerFrom() = Constants.VIEWER_FROM_INCOMING_SHARES

    /**
     * Initialize the adapter
     */
    private fun initAdapter() {
        if (adapter == null) {
            adapter = MegaNodeAdapter(
                requireActivity(),
                this,
                nodes,
                managerState().incomingParentHandle,
                recyclerView,
                Constants.INCOMING_SHARES_ADAPTER,
                if (managerActivity.isList) MegaNodeAdapter.ITEM_VIEW_TYPE_LIST
                else MegaNodeAdapter.ITEM_VIEW_TYPE_GRID,
                sortByHeaderViewModel
            )
        } else {
            adapter.parentHandle = managerState().incomingParentHandle
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
        managerState().incomingParentHandle == -1L ||
                managerState().incomingParentHandle == INVALID_HANDLE ||
                megaApi.getNodeByHandle(managerState().incomingParentHandle) == null

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

            if (managerState().incomingTreeDepth == 0) {
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

            if (managerState().incomingTreeDepth > 0 && selected.size > 0 && allHaveFullAccess(
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
            control.trash().isVisible = (managerState().incomingTreeDepth > 0
                    && allHaveFullAccess(selected))
            CloudStorageOptionControlUtil.applyControl(menu, control)
            return true
        }
    }
}