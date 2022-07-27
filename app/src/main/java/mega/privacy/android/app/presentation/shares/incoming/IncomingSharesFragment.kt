package mega.privacy.android.app.presentation.shares.incoming

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
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.ColorUtils.setImageViewAlphaIfDark
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.ORDER_CLOUD
import mega.privacy.android.app.utils.Constants.ORDER_OTHERS
import mega.privacy.android.app.utils.MegaNodeUtil.allHaveFullAccess
import mega.privacy.android.app.utils.MegaNodeUtil.areAllFileNodesAndNotTakenDown
import mega.privacy.android.app.utils.MegaNodeUtil.areAllNotTakenDown
import mega.privacy.android.app.utils.Util
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

    private val viewModel: IncomingSharesViewModel by activityViewModels()

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
            if (managerActivity.isList) getListView(inflater, container)
            else getGridView(inflater, container)

        initAdapter()
        observe()
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

    override fun refresh() {}

    override fun itemClick(position: Int) {
        val actualPosition = position - adapter.placeholderCount

        when {
            // select mode
            adapter.isMultipleSelect -> {
                adapter.toggleSelection(position)
                val selectedNodes = adapter.selectedNodes
                if (selectedNodes.size > 0)
                    updateActionModeTitle()
            }

            // click on a folder
            state().nodes[actualPosition].isFolder ->
                navigateToFolder(state().nodes[actualPosition])

            // click on a file
            else ->
                openFile(
                    state().nodes[actualPosition],
                    Constants.INCOMING_SHARES_ADAPTER,
                    actualPosition
                )
        }
    }

    override fun navigateToFolder(node: MegaNode) {
        Timber.d("Is folder deep: %s", state().incomingTreeDepth)

        val lastFirstVisiblePosition: Int = when {
            managerActivity.isList ->
                mLayoutManager.findFirstCompletelyVisibleItemPosition()

            (recyclerView as NewGridRecyclerView).findFirstCompletelyVisibleItemPosition() == -1 ->
                (recyclerView as NewGridRecyclerView).findFirstVisibleItemPosition()

            else ->
                (recyclerView as NewGridRecyclerView).findFirstCompletelyVisibleItemPosition()
        }

        viewModel.pushToLastPositionState(lastFirstVisiblePosition)
        viewModel.increaseIncomingTreeDepth(node.handle)
        recyclerView.scrollToPosition(0)
        checkScroll()
    }

    override fun onBackPressed(): Int {

        if (adapter == null)
            return 0

        if (managerActivity.comesFromNotifications && managerActivity.comesFromNotificationsLevel == state().incomingTreeDepth) {
            managerActivity.restoreSharesAfterComingFromNotifications()
            return 4
        }

        managerActivity.invalidateOptionsMenu()

        return when {
            state().incomingTreeDepth == 1 -> {
                Timber.d("deepBrowserTree==1")
                viewModel.resetIncomingTreeDepth()

                val lastVisiblePosition = viewModel.popLastPositionStack()

                lastVisiblePosition.takeIf { it > 0 }?.let {
                    if (managerActivity.isList)
                        mLayoutManager.scrollToPositionWithOffset(it, 0)
                    else
                        gridLayoutManager.scrollToPositionWithOffset(it, 0)
                }

                recyclerView.visibility = View.VISIBLE
                emptyImageView.visibility = View.GONE
                emptyLinearLayout.visibility = View.GONE
                3
            }

            state().incomingTreeDepth > 1 -> {
                Timber.d("deepTree>1")

                viewModel.getParentNodeHandle()?.let { parentHandle ->
                    recyclerView.visibility = View.VISIBLE
                    emptyImageView.visibility = View.GONE
                    emptyLinearLayout.visibility = View.GONE
                    viewModel.decreaseIncomingTreeDepth(parentHandle)

                    val lastVisiblePosition = viewModel.popLastPositionStack()

                    lastVisiblePosition.takeIf { it > 0 }?.let {
                        if (managerActivity.isList)
                            mLayoutManager.scrollToPositionWithOffset(it, 0)
                        else
                            gridLayoutManager.scrollToPositionWithOffset(it, 0)
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

    /**
     * Method to update an item when a nickname is added, updated or removed from a contact.
     *
     * @param contactHandle Contact ID.
     */
    override fun updateContact(contactHandle: Long) {
        adapter.updateItem(contactHandle)
    }

    override fun showSortByPanel() {
        val orderType = when (state().incomingTreeDepth) {
            0 -> ORDER_OTHERS
            else -> ORDER_CLOUD
        }
        managerActivity.showNewSortByPanel(orderType)
    }

    override fun viewerFrom() = Constants.VIEWER_FROM_INCOMING_SHARES

    override fun getParentHandle(): Long = state().incomingParentHandle

    /**
     * Observe viewModel
     */
    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.state.collect {
                    Timber.d("Collect ui state")
                    updateNodes(it.nodes)
                    hideTabs(!it.isFirstNavigationLevel())

                    managerActivity.showFabButton()
                    managerActivity.invalidateOptionsMenu()
                    managerActivity.setToolbarTitle()

                    visibilityFastScroller()
                    hideActionMode()
                    setEmptyView(it.isInvalidParentHandle)

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
        adapter.setNodes(mutableListNodes)
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
                state().incomingParentHandle,
                recyclerView,
                Constants.INCOMING_SHARES_ADAPTER,
                if (managerActivity.isList) MegaNodeAdapter.ITEM_VIEW_TYPE_LIST
                else MegaNodeAdapter.ITEM_VIEW_TYPE_GRID,
                sortByHeaderViewModel
            )
        } else {
            adapter.parentHandle = state().incomingParentHandle
            adapter.setListFragment(recyclerView)
        }

        if (!managerActivity.isList)
            gridLayoutManager.spanSizeLookup =
                adapter.getSpanSizeLookup(gridLayoutManager.spanCount)

        adapter.isMultipleSelect = false
        recyclerView.adapter = adapter
    }

    /**
     * Hide/Show shares tab
     *
     * @param hide true if needs to hide shares tabs
     */
    private fun hideTabs(hide: Boolean) {
        managerActivity.hideTabs(hide, SharesTab.INCOMING_TAB)
    }

    /**
     * If user navigates from notification about new nodes added to shared folder select all nodes and scroll to the first node in the list
     */
    private fun selectNewlyAddedNodes() {
        val positions = managerActivity.getPositionsList(state().nodes)
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

    /**
     * Set the empty view and message depending if the parent handle is valid or not
     *
     * @param isInvalidParentHandle true if the parent handle is invalid
     */
    private fun setEmptyView(isInvalidParentHandle: Boolean) {
        var textToShow: String? = null

        if (isInvalidParentHandle) {
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

    private inner class ActionBarCallBack(currentTab: Tab?) : BaseActionBarCallBack(currentTab) {
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            super.onPrepareActionMode(mode, menu)
            val control = CloudStorageOptionControlUtil.Control()

            if (state().incomingTreeDepth == 0) {
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

            if (state().incomingTreeDepth > 0 && selected.size > 0 && allHaveFullAccess(
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
            control.trash().isVisible = (state().incomingTreeDepth > 0
                    && allHaveFullAccess(selected))
            CloudStorageOptionControlUtil.applyControl(menu, control)
            return true
        }
    }
}