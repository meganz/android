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

        val view = getListView(inflater, container)

        initAdapter()
        refresh()

        return view
    }

    override fun activateActionMode() {
        if (adapter.isMultipleSelect) return

        super.activateActionMode()
        actionMode =
            (requireActivity() as AppCompatActivity).startSupportActionMode(
                ActionBarCallBack(SharesTab.LINKS_TAB)
            )
    }

    override fun setNodes(nodes: List<MegaNode>) {
        this.nodes = nodes
        adapter.setNodes(nodes)
        setEmptyView()
        visibilityFastScroller()
    }

    override fun setEmptyView() {
        var textToShow: String? = null
        if (isInvalidParentHandle()) {
            setImageViewAlphaIfDark(requireContext(), emptyImageView, ColorUtils.DARK_IMAGE_ALPHA)
            emptyImageView.setImageResource(R.drawable.ic_zero_data_public_links)
            textToShow = requireContext().getString(R.string.context_empty_links)
        }
        setFinalEmptyView(textToShow)
    }

    override fun onBackPressed(): Int {
        Timber.d("deepBrowserTree:%s", managerState().linksTreeDepth)

        if (adapter == null)
            return 0

        managerViewModel.decreaseLinksTreeDepth()

        when {
            managerState().linksTreeDepth == 0 -> {
                //In the beginning of the navigation
                Timber.d("deepBrowserTree==0")
                managerViewModel.setLinksParentHandle(MegaApiJava.INVALID_HANDLE)
                managerActivity.hideTabs(false, SharesTab.LINKS_TAB)

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

            managerState().linksTreeDepth > 0 -> {
                Timber.d("deepTree>0")
                val parentNode =
                    megaApi.getParentNode(
                        megaApi.getNodeByHandle(managerState().linksParentHandle)
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

            }
            else -> {
                managerViewModel.resetLinksTreeDepth()
            }


        }

        return 1
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
                openFile(nodes[position], Constants.LINKS_ADAPTER, position)
        }
    }

    override fun navigateToFolder(node: MegaNode) {
        managerActivity.hideTabs(true, SharesTab.LINKS_TAB)
        managerViewModel.increaseLinksTreeDepth()
        Timber.d("Is folder deep: %s", managerState().linksTreeDepth)

        val lastFirstVisiblePosition: Int = when {
            managerActivity.isList ->
                mLayoutManager.findFirstCompletelyVisibleItemPosition()

            (recyclerView as NewGridRecyclerView).findFirstCompletelyVisibleItemPosition() == -1 ->
                (recyclerView as NewGridRecyclerView).findFirstVisibleItemPosition()

            else ->
                (recyclerView as NewGridRecyclerView).findFirstCompletelyVisibleItemPosition()
        }
        lastPositionStack.push(lastFirstVisiblePosition)
        managerViewModel.setLinksParentHandle(node.handle)

        refresh()
        recyclerView.scrollToPosition(0)
        checkScroll()
    }

    override fun refresh() {
        val order = getLinksOrderCloud(
            sortOrderManagement.getOrderCloud(),
            managerState().isFirstNavigationLevel
        )

        nodes =
            managerState().linksParentHandle.takeUnless { it == -1L || it == MegaApiJava.INVALID_HANDLE }
                ?.let { megaApi.getNodeByHandle(managerState().linksParentHandle) }
                ?.let { megaApi.getChildren(it, order) }
                ?: run {
                    megaApi.getPublicLinks(order)
                }

        adapter.setNodes(nodes)

        managerActivity.showFabButton()
        managerActivity.invalidateOptionsMenu()
        managerActivity.setToolbarTitle()

        visibilityFastScroller()
        hideActionMode()
        setEmptyView()
    }

    /**
     * Initialize the adapter
     */
    private fun initAdapter() {
        if (adapter == null) {
            adapter = MegaNodeAdapter(requireActivity(),
                this,
                nodes,
                managerState().linksParentHandle,
                recyclerView,
                Constants.LINKS_ADAPTER,
                MegaNodeAdapter.ITEM_VIEW_TYPE_LIST,
                sortByHeaderViewModel)
        } else {
            adapter.parentHandle = managerState().linksParentHandle
            adapter.setListFragment(recyclerView)
        }

        adapter.isMultipleSelect = false
        recyclerView.adapter = adapter
    }

    /**
     * Check if the parent handle is valid
     *
     * @return true if the parent handle is valid
     */
    private fun isInvalidParentHandle(): Boolean =
        managerState().linksParentHandle == -1L ||
                managerState().linksParentHandle == MegaApiJava.INVALID_HANDLE ||
                megaApi.getNodeByHandle(managerState().linksParentHandle) == null

    override fun updateContact(contactHandle: Long) {}

    override fun viewerFrom(): Int = Constants.VIEWER_FROM_LINKS

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