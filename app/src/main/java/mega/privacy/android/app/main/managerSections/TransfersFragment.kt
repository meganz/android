package mega.privacy.android.app.main.managerSections

import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mega.privacy.android.app.R
import mega.privacy.android.app.data.extensions.isBackgroundTransfer
import mega.privacy.android.app.fragments.managerFragments.TransfersBaseFragment
import mega.privacy.android.app.fragments.managerFragments.actionMode.TransfersActionBarCallBack
import mega.privacy.android.app.interfaces.MoveTransferInterface
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.adapters.MegaTransfersAdapter
import mega.privacy.android.app.main.adapters.RotatableAdapter
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.dp2px
import mega.privacy.android.app.utils.Util.noChangeRecyclerViewItemAnimator
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaTransfer
import timber.log.Timber

/**
 * The Fragment is used for displaying the transfer list.
 */
@AndroidEntryPoint
class TransfersFragment : TransfersBaseFragment(), MegaTransfersAdapter.SelectModeInterface,
    TransfersActionBarCallBack.TransfersActionCallback, MoveTransferInterface {

    private var adapter: MegaTransfersAdapter? = null

    private var actionMode: ActionMode? = null

    private var itemTouchHelper: ItemTouchHelper? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return initView(inflater, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.transfersEmptyImage.setImageResource(
            if (Util.isScreenInPortrait(requireContext())) {
                R.drawable.empty_transfer_portrait
            } else R.drawable.empty_transfer_landscape)

        binding.transfersEmptyText.text = TextUtil.formatEmptyScreenText(requireContext(),
            getString(R.string.transfers_empty_new))

        setupFlow()
        viewModel.setActiveTransfers((requireActivity() as ManagerActivity).transfersInProgress)

        binding.transfersListView.let { recyclerView ->
            adapter = MegaTransfersAdapter(requireActivity(),
                viewModel.getActiveTransfers(),
                recyclerView,
                this,
                viewModel)

            adapter?.isMultipleSelect = false
            recyclerView.adapter = adapter
            recyclerView.itemAnimator = noChangeRecyclerViewItemAnimator()
        }

        itemTouchHelper = ItemTouchHelper(initItemTouchHelperCallback(
            dragDirs = ItemTouchHelper.UP or ItemTouchHelper.DOWN))

        enableDragAndDrop()
    }

    /**
     * Updates the state of a transfer.
     *
     * @param transfer transfer to update
     */
    fun transferUpdate(transfer: MegaTransfer?) =
        transfer?.let {
            viewModel.getUpdatedTransferPosition(it).let { transferPosition ->
                if (transferPosition != INVALID_POSITION) {
                    viewModel.updateActiveTransfer(transferPosition, it)
                    adapter?.updateProgress(transferPosition, it)
                }
            }
        }

    /**
     * Changes the status (play/pause) of the button of a transfer.
     *
     * @param tag identifier of the transfer to change the status of the button
     */
    fun changeStatusButton(tag: Int) = viewModel.activeTransferChangeStatus(tag)

    /**
     * Removes a transfer when finishes.
     *
     * @param transferTag identifier of the transfer to remove
     */
    fun transferFinish(transferTag: Int) = viewModel.activeTransferFinished(transferTag)

    /**
     * Adds a transfer when starts.
     *
     * @param transfer transfer to add
     */
    fun transferStart(transfer: MegaTransfer) {
        if (!transfer.isStreamingTransfer && !transfer.isBackgroundTransfer()) {
            if (viewModel.getActiveTransfers().isEmpty()) {
                requireActivity().invalidateOptionsMenu()
            }
            viewModel.activeTransferStart(transfer)
        }
    }

    /**
     * Check whether is in select mode after changing tab or drawer item.
     */
    fun checkSelectModeAfterChangeTabOrDrawerItem() {
        adapter?.run {
            if (isMultipleSelect) {
                destroyActionMode()
            }
        }
    }

    override fun onCreateActionMode() = updateElevation()

    override fun onDestroyActionMode() {
        clearSelections()
        adapter?.hideMultipleSelect()
        updateElevation()
    }

    override fun cancelTransfers() {
        adapter?.run {
            (requireActivity() as ManagerActivity).showConfirmationCancelSelectedTransfers(
                selectedTransfers)
        }
    }

    override fun selectAll() {
        adapter?.selectAll()
    }

    override fun clearSelections() {
        adapter?.clearSelections()
    }

    override fun getSelectedTransfers() = adapter?.selectedItemsCount ?: 0

    override fun areAllTransfersSelected() = adapter?.run {
        selectedItemsCount == itemCount
    } == true

    override fun hideTabs(hide: Boolean) =
        (requireActivity() as ManagerActivity).hideTabs(hide, TransfersTab.PENDING_TAB)

    override fun movementFailed(transferTag: Int) =
        finishMovement(success = false, transferTag = transferTag)

    override fun movementSuccess(transferTag: Int) =
        finishMovement(success = true, transferTag = transferTag)

    override fun destroyActionMode() {
        actionMode?.finish()

        enableDragAndDrop()
    }

    override fun notifyItemChanged() = updateActionModeTitle()


    override fun getAdapter(): RotatableAdapter? = adapter

    override fun activateActionMode() {
        adapter?.let {
            if (!it.isMultipleSelect) {
                it.isMultipleSelect = true
                actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(
                    TransfersActionBarCallBack(this))
                updateActionModeTitle()
                disableDragAndDrop()
            }
        }
    }

    override fun multipleItemClick(position: Int) {
        adapter?.toggleSelection(position)
    }

    override fun updateActionModeTitle() {
        if (actionMode != null && activity != null && adapter != null) {
            val count = adapter?.selectedItemsCount
            val title: String = if (count == 0) {
                getString(R.string.title_select_transfers)
            } else {
                count.toString()
            }
            actionMode?.title = title
            actionMode?.invalidate()
        } else {
            Timber.w("RETURN: null values")
        }
    }

    override fun updateElevation() =
        (requireActivity() as ManagerActivity).changeAppBarElevation(
            binding.transfersListView.canScrollVertically(DEFAULT_SCROLL_DIRECTION) ||
                    adapter?.isMultipleSelect == true)

    private fun setupFlow() {
        viewModel.activeState.flowWithLifecycle(
            viewLifecycleOwner.lifecycle,
            Lifecycle.State.RESUMED
        ).onEach { transfersState ->
            when (transfersState) {
                is ActiveTransfersState.TransfersUpdated -> {
                    setEmptyView(transfersState.newTransfers.size)
                }
                is ActiveTransfersState.TransferUpdated -> {
                    adapter?.updateProgress(transfersState.index, transfersState.updatedTransfer)
                }
                is ActiveTransfersState.TransferMovementFinishedUpdated -> {
                    if (transfersState.success) {
                        adapter?.notifyItemChanged(transfersState.pos)
                    } else {
                        (requireActivity() as ManagerActivity).showSnackbar(
                            SNACKBAR_TYPE,
                            getString(R.string.change_of_transfer_priority_failed,
                                transfersState.newTransfers[transfersState.pos].fileName),
                            MEGACHAT_INVALID_HANDLE
                        )
                        adapter?.setTransfers(transfersState.newTransfers)
                    }
                }
                is ActiveTransfersState.TransferStartUpdated -> {
                    val transfers = transfersState.newTransfers
                    adapter?.addItemData(transfers,
                        transfers.indexOf(transfersState.updatedTransfer))

                    if (transfers.size == 1) {
                        setEmptyView(transfers.size)
                    }
                }
                is ActiveTransfersState.TransferFinishedUpdated -> {
                    val transfers = transfersState.newTransfers
                    Timber.d("new transfer is ${transfers.joinToString { it.fileName }}")
                    adapter?.removeItemData(transfers, transfersState.index)
                    if (transfers.isEmpty()) {
                        activateActionMode()
                        destroyActionMode()
                        setEmptyView(transfers.size)
                        requireActivity().invalidateOptionsMenu()
                    }
                }
                is ActiveTransfersState.TransferChangeStatusUpdated -> {
                    val index = transfersState.index
                    viewModel.updateActiveTransfer(index, transfersState.transfer)
                    adapter?.notifyItemChanged(index)
                }
                else -> {}
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun initItemTouchHelperCallback(
        dragDirs: Int,
        swipeDirs: Int = 0,
    ): ItemTouchHelper.SimpleCallback =
        object : ItemTouchHelper.SimpleCallback(dragDirs, swipeDirs) {
            private var addElevation = true
            private var resetElevation = false
            private var draggedTransfer: MegaTransfer? = null
            private var newPosition = 0

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ): Boolean {
                val posDragged = viewHolder.absoluteAdapterPosition
                newPosition = target.absoluteAdapterPosition

                if (draggedTransfer == null) {
                    draggedTransfer = viewModel.getActiveTransfer(posDragged)
                }
                adapter?.moveItemData(
                    viewModel.activeTransfersSwap(posDragged, newPosition), posDragged, newPosition)
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean,
            ) {
                if (addElevation) {
                    recyclerView.post {
                        binding.transfersListView.removeItemDecoration(itemDecoration)
                    }
                    val animator = viewHolder.itemView.animate()
                    viewHolder.itemView.translationZ = dp2px(2f, resources.displayMetrics).toFloat()
                    viewHolder.itemView.alpha = 0.95f
                    animator.start()

                    addElevation = false
                }

                if (resetElevation) {
                    recyclerView.post {
                        binding.transfersListView.addItemDecoration(itemDecoration)
                    }
                    val animator = viewHolder.itemView.animate()
                    viewHolder.itemView.translationZ = 0f
                    viewHolder.itemView.alpha = 1f
                    animator.start()

                    addElevation = true
                    resetElevation = false
                }
                super.onChildDraw(c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive)
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
            ) {
                super.clearView(recyclerView, viewHolder)
                // Drag finished, elevation should be removed.
                resetElevation = true
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)

                draggedTransfer?.let {
                    if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                        startMovementRequest(it, newPosition)
                        draggedTransfer = null
                    }
                }
            }
        }

    /**
     * Launches the request to change the priority of a transfer.
     *
     * @param transfer    MegaTransfer to change its priority.
     * @param newPosition The new position on the list.
     */
    private fun startMovementRequest(transfer: MegaTransfer, newPosition: Int) {
        viewModel.moveTransfer(transfer,
            newPosition,
            OptionalMegaRequestListenerInterface(onRequestFinish = { megaRequest, megaError ->
                if (megaRequest.type == MegaRequest.TYPE_MOVE_TRANSFER) {
                    if (megaError.errorCode != MegaError.API_OK) {
                        Timber.e("Error changing transfer priority: ${
                            StringResourcesUtils.getTranslatedErrorString(megaError)
                        }")
                        this.movementFailed(megaRequest.transferTag)
                    } else {
                        this.movementSuccess(megaRequest.transferTag)
                    }
                }
            }))

    }

    private fun enableDragAndDrop() {
        itemTouchHelper?.attachToRecyclerView(binding.transfersListView)
    }

    private fun disableDragAndDrop() =
        itemTouchHelper?.attachToRecyclerView(null)


    /**
     * Updates the UI in consequence after a transfer movement.
     * The update depends on if the movement finished with or without success.
     * If it finished with success, simply update the transfer in the transfers list and in adapter.
     * If not, reverts the movement, leaving the transfer in the same position it has before made the change.
     *
     * @param success     True if the movement finished with success, false otherwise.
     * @param transferTag Identifier of the transfer.
     */
    private fun finishMovement(success: Boolean, transferTag: Int) {
        viewModel.activeTransferFinishMovement(success, transferTag)
    }

    companion object {

        /**
         * Generate a new instance for [TransfersFragment]
         *
         * @return new [TransfersFragment] instance
         */
        @JvmStatic
        fun newInstance(): TransfersFragment = TransfersFragment()
    }
}