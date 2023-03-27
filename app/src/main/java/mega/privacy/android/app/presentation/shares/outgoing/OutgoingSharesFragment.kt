package mega.privacy.android.app.presentation.shares.outgoing

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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.CustomizedGridLayoutManager
import mega.privacy.android.app.components.PositionDividerItemDecoration
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.main.adapters.MegaNodeAdapter
import mega.privacy.android.app.presentation.contact.authenticitycredendials.AuthenticityCredentialsActivity
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
import mega.privacy.android.app.utils.displayMetrics
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.preference.ViewType
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

    private val viewModel by activityViewModels<OutgoingSharesViewModel>()

    private fun outgoingSharesState() = viewModel.state.value

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
        switchViewType()

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
                ActionBarCallBack(SharesTab.OUTGOING_TAB)
            )
    }

    override fun itemClick(position: Int) {
        val actualPosition = position - 1
        val node = outgoingSharesState().nodes.getOrNull(actualPosition)?.first
        val shareData = outgoingSharesState().nodes.getOrNull(actualPosition)?.second
        when {
            shareData?.isPending == true -> {
                showCanNotVerifyContact(shareData.user)
            }
            shareData?.isVerified == false -> {
                openAuthenticityCredentials(shareData.user)
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
                        Constants.OUTGOING_SHARES_ADAPTER,
                        actualPosition
                    )
                }
        }
    }

    override fun navigateToFolder(node: MegaNode) {
        Timber.d("Is folder deep: %s", outgoingSharesState().outgoingTreeDepth)

        val lastFirstVisiblePosition: Int = when {
            outgoingSharesState().currentViewType == ViewType.LIST ->
                recyclerView?.findFirstCompletelyVisibleItemPosition() ?: 0

            recyclerView?.findFirstCompletelyVisibleItemPosition() == -1 ->
                recyclerView?.findFirstVisibleItemPosition() ?: 0

            else ->
                recyclerView?.findFirstCompletelyVisibleItemPosition() ?: 0
        }

        viewModel.pushToLastPositionStack(lastFirstVisiblePosition)
        viewModel.increaseOutgoingTreeDepth(node.handle)
        recyclerView?.scrollToPosition(0)
        checkScroll()
    }

    override fun onBackPressed(): Int {
        Timber.d("deepBrowserTree: %s", managerActivity?.deepBrowserTreeOutgoing)

        if (megaNodeAdapter == null)
            return 0

        managerActivity?.invalidateOptionsMenu()

        return when {
            outgoingSharesState().outgoingTreeDepth == 1 -> {
                Timber.d("deepBrowserTree==1")
                viewModel.resetOutgoingTreeDepth()

                val lastVisiblePosition = viewModel.popLastPositionStack()

                lastVisiblePosition.takeIf { it > 0 }?.let {
                    recyclerView?.scrollToPosition(it)
                }

                recyclerView?.visibility = View.VISIBLE
                emptyListImageView?.visibility = View.GONE
                3
            }

            outgoingSharesState().outgoingTreeDepth > 1 -> {
                Timber.d("deepTree>1")

                outgoingSharesState().outgoingParentHandle?.let { parentHandle ->
                    recyclerView?.visibility = View.VISIBLE
                    emptyListImageView?.visibility = View.GONE
                    viewModel.decreaseOutgoingTreeDepth(parentHandle)

                    val lastVisiblePosition = viewModel.popLastPositionStack()

                    lastVisiblePosition.takeIf { it > 0 }?.let {
                        recyclerView?.scrollToPosition(it)
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

    override fun showSortByPanel() {
        val orderType = when (outgoingSharesState().outgoingTreeDepth) {
            0 -> ORDER_OTHERS
            else -> ORDER_CLOUD
        }
        managerActivity?.showNewSortByPanel(orderType)
    }

    override val viewerFrom: Int = Constants.VIEWER_FROM_OUTGOING_SHARES
    override val currentSharesTab: SharesTab = SharesTab.OUTGOING_TAB
    override val sortOrder: SortOrder
        get() = outgoingSharesState().sortOrder
    override val parentHandle: Long
        get() = outgoingSharesState().outgoingHandle

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
        if (viewType != outgoingSharesState().currentViewType) {
            viewModel.setCurrentViewType(viewType)
            switchViewType()
        }
    }

    /**
     * Switches how items in the [MegaNodeAdapter] are being displayed, based on the current
     * [ViewType] in [OutgoingSharesViewModel]
     */
    private fun switchViewType() {
        recyclerView?.run {
            when (outgoingSharesState().currentViewType) {
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
     * @param nodes the list of nodes to display with his shareData associated
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
            outgoingSharesState().nodes.map { it.first },
            outgoingSharesState().outgoingHandle,
            recyclerView,
            Constants.OUTGOING_SHARES_ADAPTER,
            if (outgoingSharesState().currentViewType == ViewType.LIST) MegaNodeAdapter.ITEM_VIEW_TYPE_LIST
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
                emptyListImageView?.setImageResource(R.drawable.empty_outgoing_portrait)
            } else {
                emptyListImageView?.setImageResource(R.drawable.empty_outgoing_landscape)
            }
            textToShow = requireContext().getString(R.string.context_empty_outgoing)
        }
        setFinalEmptyView(textToShow)
    }

    private inner class ActionBarCallBack(currentTab: Tab) : BaseActionBarCallBack(currentTab) {
        override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
            super.onPrepareActionMode(actionMode, menu)
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
                if (outgoingSharesState().outgoingHandle == INVALID_HANDLE) {
                    control.removeShare().setVisible(true).showAsAction =
                        MenuItem.SHOW_AS_ACTION_ALWAYS
                }
                control.shareOut().setVisible(true).showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                if (outgoingSharesState().outgoingTreeDepth > 0) {
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

    /**
     * Show cannot verify contact dialog
     * @param email : Email of the user
     */
    private fun showCanNotVerifyContact(email: String?) {
        MaterialAlertDialogBuilder(
            requireContext(),
            R.style.ThemeOverlay_Mega_MaterialAlertDialog
        ).setTitle(getString(R.string.shared_items_contact_not_in_contact_list_dialog_title))
            .setMessage(
                getString(
                    R.string.shared_items_contact_not_in_contact_list_dialog_content,
                    email
                )
            )
            .setPositiveButton(
                getString(R.string.general_ok)
            ) { dialogInterface, _ -> dialogInterface.dismiss() }
            .show()
    }

    /**
     * Open authenticityCredentials screen to verify user
     * @param email : Email of the user
     */
    private fun openAuthenticityCredentials(email: String?) {
        Intent(
            requireActivity(),
            AuthenticityCredentialsActivity::class.java
        ).apply {
            putExtra(Constants.EMAIL, email)
            requireActivity().startActivity(this)
        }
    }
}
