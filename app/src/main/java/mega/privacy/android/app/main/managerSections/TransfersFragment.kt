package mega.privacy.android.app.main.managerSections

import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.data.extensions.isBackgroundTransfer
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.fragments.managerFragments.TransfersBaseFragment
import mega.privacy.android.app.fragments.managerFragments.actionMode.TransfersActionBarCallBack
import mega.privacy.android.app.interfaces.MoveTransferInterface
import mega.privacy.android.app.listeners.MoveTransferListener
import mega.privacy.android.app.main.adapters.MegaTransfersAdapter
import mega.privacy.android.app.main.adapters.RotatableAdapter
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.dp2px
import mega.privacy.android.app.utils.Util.noChangeRecyclerViewItemAnimator
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaTransfer.STATE_COMPLETING
import timber.log.Timber
import java.util.Collections
import javax.inject.Inject

/**
 * The Fragment is used for displaying the transfer list.
 */
@AndroidEntryPoint
class TransfersFragment : TransfersBaseFragment(), MegaTransfersAdapter.SelectModeInterface,
    TransfersActionBarCallBack.TransfersActionCallback, MoveTransferInterface {

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    private var adapter: MegaTransfersAdapter? = null

    private val tL = mutableListOf<MegaTransfer>()

    private var actionMode: ActionMode? = null

    private var itemTouchHelper: ItemTouchHelper? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        val v = initView(inflater, container)

        emptyImage.setImageResource(
            if (Util.isScreenInPortrait(requireContext())) {
                R.drawable.empty_transfer_portrait
            } else R.drawable.empty_transfer_landscape)

        var textToShow = StringResourcesUtils.getString(R.string.transfers_empty_new)

        try {
            textToShow = textToShow.replace("[A]",
                "<font color=\'${
                    getColorHexString(requireContext(),
                        R.color.grey_900_grey_100)
                }\'>")
            textToShow = textToShow.replace("[/A]", "</font>")
            textToShow = textToShow.replace("[B]",
                "<font color=\'${
                    getColorHexString(requireContext(),
                        R.color.grey_300_grey_600)
                }\'>")
            textToShow = textToShow.replace("[/B]", "</font>")
        } catch (e: Exception) {
            Timber.w(e, "Exception formatting string")
        }

        emptyText.text = HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY)

        setTransfers()

        adapter = MegaTransfersAdapter(
            requireActivity(), tL, listView, this, transfersManagement)

        adapter?.isMultipleSelect = false
        listView?.adapter = adapter
        listView?.itemAnimator = noChangeRecyclerViewItemAnimator()

        itemTouchHelper = ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
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
                        draggedTransfer = tL[posDragged]
                    }

                    Collections.swap(tL, posDragged, newPosition)
                    adapter?.moveItemData(tL, posDragged, newPosition)

                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

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
                            listView?.removeItemDecoration(itemDecoration)
                        }
                        val animator = viewHolder.itemView.animate()
                        viewHolder.itemView.translationZ =
                            dp2px(2f, resources.displayMetrics).toFloat()
                        viewHolder.itemView.alpha = 0.95f
                        animator.start()

                        addElevation = false
                    }

                    if (resetElevation) {
                        recyclerView.post {
                            listView?.addItemDecoration(itemDecoration)
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

                override fun onSelectedChanged(
                    viewHolder: RecyclerView.ViewHolder?,
                    actionState: Int,
                ) {
                    super.onSelectedChanged(viewHolder, actionState)

                    draggedTransfer?.let {
                        if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                            startMovementRequest(it, newPosition)
                            draggedTransfer = null
                        }
                    }
                }
            }
        )

        enableDragAndDrop()
        return v
    }

    /**
     * Updates the state of a transfer.
     *
     * @param transfer transfer to update
     */
    fun transferUpdate(transfer: MegaTransfer?) {
        tryToUpdateTransfer(transfer).let { transferPosition ->
            if (transferPosition != INVALID_POSITION) {
                adapter?.updateProgress(transferPosition, transfer)
            }
        }
    }

    /**
     * Tries to update a MegaTransfer in the transfers list.
     *
     * @param transfer The MegaTransfer to update.
     * @return The position of the updated transfer if success, INVALID_POSITION otherwise.
     */
    fun tryToUpdateTransfer(transfer: MegaTransfer?): Int {
        transfer?.let {
            try {
                val li = tL.listIterator()
                while (li.hasNext()) {
                    val next = li.next()
                    if (next.tag == transfer.tag) {
                        val index = li.previousIndex()
                        tL[index] = transfer
                        return index
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "IndexOutOfBoundsException trying to update a transfer.")
            }
        }
        return INVALID_POSITION
    }

    /**
     * Changes the status (play/pause) of the button of a transfer.
     *
     * @param tag identifier of the transfer to change the status of the button
     */
    fun changeStatusButton(tag: Int) {
        Timber.d("tag: $tag")

        val li = tL.listIterator()
        var index = 0
        while (li.hasNext()) {
            val next = li.next()
            if (next.tag == tag) {
                index = li.previousIndex()
                break
            }
        }
        megaApi.getTransferByTag(tag)?.let { transfer ->
            tL[index] = transfer
            Timber.d("The transfer with index : $index has been paused/resumed, left: ${tL.size}")
            adapter?.notifyItemChanged(index)
        }
    }

    /**
     * Removes a transfer when finishes.
     *
     * @param transferTag identifier of the transfer to remove
     */
    fun transferFinish(transferTag: Int) {
        val index = tL.indexOfFirst { transfer ->
            transfer.tag == transferTag
        }
        tL.removeIf { transfer ->
            transfer.tag == transferTag
        }
        adapter?.removeItemData(tL, index)

        if (tL.isEmpty()) {
            destroyActionMode()
            setEmptyView(tL.size)
            managerActivity.invalidateOptionsMenu()
        }
    }

    /**
     * Adds a transfer when starts.
     *
     * @param transfer transfer to add
     */
    fun transferStart(transfer: MegaTransfer) {
        if (!transfer.isStreamingTransfer && !transfer.isBackgroundTransfer()) {
            if (tL.isEmpty()) {
                managerActivity.invalidateOptionsMenu()
            }

            tL.add(transfer)
            orderTransfersByDescendingPriority()
            adapter?.addItemData(tL, tL.indexOf(transfer))

            if (tL.size == 1) {
                setEmptyView(tL.size)
            }
        }
    }

    /**
     * Checks if there is any transfer in progress.
     *
     * @return True if there is not any transfer, false otherwise.
     */
    fun isEmpty(): Boolean = tL.isEmpty()

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
            managerActivity.showConfirmationCancelSelectedTransfers(selectedTransfers)
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
    } ?: false

    override fun hideTabs(hide: Boolean) = managerActivity.hideTabs(hide, TransfersTab.PENDING_TAB)

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
        managerActivity.changeAppBarElevation(
            listView?.canScrollVertically(DEFAULT_SCROLL_DIRECTION) == true ||
                    adapter?.isMultipleSelect == true)

    private fun setTransfers() {
        tL.clear()

        managerActivity.transfersInProgress.map { tag ->
            megaApi.getTransferByTag(tag)
        }.filter { transfer ->
            transfer != null && !transfer.isStreamingTransfer && !transfer.isBackgroundTransfer()
        }.map {
            tL.add(it)
        }

        orderTransfersByDescendingPriority()
        setEmptyView(tL.size)
    }

    private fun orderTransfersByDescendingPriority() =
        tL.sortWith { t1: MegaTransfer, t2: MegaTransfer ->
            t1.priority.compareTo(t2.priority)
        }

    /**
     * Launches the request to change the priority of a transfer.
     *
     * @param transfer    MegaTransfer to change its priority.
     * @param newPosition The new position on the list.
     */
    private fun startMovementRequest(transfer: MegaTransfer, newPosition: Int) {
        MoveTransferListener(requireContext(), this).let { moveTransferListener ->
            when (newPosition) {
                0 -> {
                    megaApi.moveTransferToFirst(transfer, moveTransferListener)
                }
                tL.size - 1 -> {
                    megaApi.moveTransferToLast(transfer, moveTransferListener)
                }
                else -> {
                    megaApi.moveTransferBefore(transfer, tL[newPosition + 1], moveTransferListener)
                }
            }
        }
    }

    private fun enableDragAndDrop() {
        listView?.let {
            itemTouchHelper?.attachToRecyclerView(it)
        }
    }

    private fun disableDragAndDrop() =
        itemTouchHelper?.attachToRecyclerView(null)


    private fun reorderTransfersAfterFailedMovement() {
        orderTransfersByDescendingPriority()

        adapter?.setTransfers(tL)
    }

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
        megaApi.getTransferByTag(transferTag).let { transfer ->
            if (transfer != null && transfer.state >= STATE_COMPLETING) {
                val transferPosition = tryToUpdateTransfer(transfer)
                if (transferPosition != INVALID_POSITION) {
                    if (!success) {
                        reorderTransfersAfterFailedMovement()
                        managerActivity.showSnackbar(
                            SNACKBAR_TYPE,
                            getString(R.string.change_of_transfer_priority_failed,
                                transfer.fileName),
                            MEGACHAT_INVALID_HANDLE
                        )
                        adapter?.setTransfers(tL)
                    } else {
                        adapter?.notifyItemChanged(transferPosition)
                    }
                } else {
                    Timber.w("The transfer doesn't exist.")
                }
            } else {
                Timber.w("The transfer doesn't exist, finished or is finishing.")
            }
        }
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