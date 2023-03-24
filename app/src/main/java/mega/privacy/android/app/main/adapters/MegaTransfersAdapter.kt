package mega.privacy.android.app.main.adapters

import android.content.Context
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.managerSections.TransfersViewModel
import mega.privacy.android.app.presentation.extensions.getStorageState
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.Constants.THUMB_CORNER_RADIUS_DP
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.StorageState
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaTransfer.STATE_ACTIVE
import nz.mega.sdk.MegaTransfer.STATE_COMPLETING
import nz.mega.sdk.MegaTransfer.STATE_PAUSED
import nz.mega.sdk.MegaTransfer.STATE_QUEUED
import nz.mega.sdk.MegaTransfer.STATE_RETRYING
import nz.mega.sdk.MegaTransfer.TYPE_DOWNLOAD
import nz.mega.sdk.MegaTransfer.TYPE_UPLOAD
import timber.log.Timber
import kotlin.math.roundToLong

/**
 * The adapter for transfer section
 */
class MegaTransfersAdapter(
    private val context: Context,
    transfers: List<MegaTransfer>,
    private val listView: RecyclerView,
    private val selectModeInterface: SelectModeInterface,
    private val transfersViewModel: TransfersViewModel,
    private val megaApi: MegaApiAndroid,
    private val megaApiFolder: MegaApiAndroid,
) : RecyclerView.Adapter<TransferViewHolder>(), RotatableAdapter {

    private var transferList: List<MegaTransfer>

    private var multipleSelect: Boolean = false

    private var selectedItems: SparseBooleanArray? = null

    init {
        transferList = transfers
    }

    /**
     * Set transfers
     *
     * @param transfers [MegaTransfer] list
     */
    fun setTransfers(transfers: List<MegaTransfer>) {
        transferList = transfers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransferViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_transfers_list, parent, false)
        val holder = TransferViewHolder(view)
        holder.itemLayout = view.findViewById(R.id.transfers_list_item_layout)
        holder.thumbnailIcon = view.findViewById(R.id.transfers_list_thumbnail)
        holder.defaultIcon = view.findViewById(R.id.transfers_list_default_icon)
        holder.iconDownloadUploadView = view.findViewById(R.id.transfers_list_small_icon)
        holder.textViewFileName = view.findViewById(R.id.transfers_list_filename)
        holder.progressText = view.findViewById(R.id.transfers_progress_text)
        holder.speedText = view.findViewById(R.id.transfers_speed_text)
        holder.imageViewCompleted = view.findViewById(R.id.transfers_list_completed_image)
        holder.textViewCompleted = view.findViewById(R.id.transfers_list_completed_text)
        holder.optionReorder = view.findViewById(R.id.transfers_list_option_reorder)
        holder.optionPause = view.findViewById(R.id.transfers_list_option_pause)

        holder.itemLayout.setOnClickListener {
            Timber.d("Item layout onClick")
            (view?.tag as? TransferViewHolder)?.let { holder ->
                if (isMultipleSelect()) {
                    toggleSelection(holder.absoluteAdapterPosition)
                }
            } ?: Timber.w("Holder is NULL- not action performed")
        }
        holder.optionPause.setOnClickListener {
            (view?.tag as? TransferViewHolder)?.let { holder ->
                getTransferItem(holder.absoluteAdapterPosition)?.let { transfer ->
                    (context as? ManagerActivity)?.pauseIndividualTransfer(transfer)
                }
            } ?: Timber.w("Holder is NULL- not action performed")
        }
        view.tag = view
        return holder
    }

    override fun onBindViewHolder(holder: TransferViewHolder, position: Int) {
        val transfer = getTransferItem(position)
        if (transfer == null) {
            Timber.w("The recovered transfer is NULL - do not update")
            return
        }
        holder.textViewFileName.text = transfer.fileName
        val isItemChecked = isItemChecked(position)
        when (transfer.type) {
            TYPE_DOWNLOAD -> {
                holder.progressText.setTextColor(ContextCompat.getColor(context,
                    R.color.green_500_green_400))
                holder.document = transfer.nodeHandle
                if (!isItemChecked) {
                    holder.iconDownloadUploadView.setImageResource(R.drawable.ic_download_transfers)
                    val nodeFromMegaApi = megaApi.getNodeByHandle(transfer.nodeHandle)
                    val nodeFromMegaApiFolder = megaApiFolder.getNodeByHandle(transfer.nodeHandle)
                    // If node that gets from MegaApi is null, getting the node from megaApiFolder
                    val node = nodeFromMegaApi ?: nodeFromMegaApiFolder
                    if (node != null) {
                        if (node.hasThumbnail()) {
                            ThumbnailUtils.getThumbnailFromCache(node) ?: let {
                                ThumbnailUtils.getThumbnailFromFolder(node, context) ?: let {
                                    try {
                                        ThumbnailUtils.getThumbnailFromMegaTransfer(node,
                                            context,
                                            holder,
                                            megaApi,
                                            this)
                                    } catch (e: Exception) {
                                        Timber.e(e, "Exception getting thumbnail")
                                        null
                                    }
                                }
                            }
                        } else {
                            null
                        }?.let { thumbnail ->
                            holder.thumbnailIcon.setImageBitmap(ThumbnailUtils.getRoundedBitmap(
                                context,
                                thumbnail,
                                Util.dp2px(THUMB_CORNER_RADIUS_DP)))
                            holder.thumbnailIcon.isVisible = true
                            holder.defaultIcon.isVisible = false
                        } ?: showDefaultIcon(
                            holder = holder,
                            drawableId = MimeTypeList.typeForName(transfer.fileName).iconResourceId
                        )
                    } else {
                        // If the node cannot be got from both MegaApi and MegaApiFolder, show default icon
                        showDefaultIcon(
                            holder = holder,
                            drawableId = MimeTypeList.typeForName(transfer.fileName).iconResourceId
                        )
                    }
                }
            }
            TYPE_UPLOAD -> {
                if (!isItemChecked) {
                    holder.iconDownloadUploadView.setImageResource(R.drawable.ic_upload_transfers)
                    showDefaultIcon(
                        holder = holder,
                        drawableId = MimeTypeList.typeForName(transfer.fileName).iconResourceId
                    )
                }
            }
        }

        holder.iconDownloadUploadView.isVisible = !isItemChecked
        if (isItemChecked) {
            showDefaultIcon(holder = holder, drawableId = R.drawable.ic_select_folder)
        }

        holder.optionReorder.isVisible = !isMultipleSelect()
        holder.optionPause.isVisible = !isMultipleSelect()
        if (!isMultipleSelect()) {
            holder.optionPause.setImageResource(R.drawable.ic_pause_grey)
        }

        holder.textViewCompleted.isVisible = false
        holder.imageViewCompleted.isVisible = false
        holder.imageViewCompleted.setImageResource(R.drawable.ic_queue)
        holder.progressText.isVisible = true

        if (megaApi.areTransfersPaused(TYPE_DOWNLOAD) || megaApi.areTransfersPaused(TYPE_UPLOAD)) {
            holder.progressText.text = getProgress(transfer)
            holder.speedText.text = context.getString(R.string.transfer_paused)

            if (!isMultipleSelect() && transfer.state == STATE_PAUSED) {
                holder.optionPause.setImageResource(R.drawable.ic_play_grey)
            }
        } else {
            when (transfer.state) {
                STATE_PAUSED -> {
                    holder.progressText.text = getProgress(transfer)
                    holder.speedText.text = context.getString(R.string.transfer_paused)
                    holder.speedText.isVisible = true

                    if (!isMultipleSelect()) {
                        holder.optionPause.setImageResource(R.drawable.ic_play_grey)
                    }
                }
                STATE_ACTIVE -> {
                    holder.progressText.text = getProgress(transfer)
                    holder.speedText.text = Util.getSpeedString(
                        if (Util.isOnline(context))
                            transfer.speed
                        else
                            0
                    )
                }
                STATE_COMPLETING,
                STATE_RETRYING,
                STATE_QUEUED,
                -> {
                    when {
                        (transfer.type == TYPE_DOWNLOAD && transfersViewModel.isOnTransferOverQuota())
                                || (transfer.type == TYPE_UPLOAD && getStorageState() == StorageState.Red)
                        -> {
                            holder.progressText.setTextColor(ContextCompat.getColor(context,
                                R.color.orange_400_orange_300))
                            holder.progressText.text = String.format("%s %s",
                                getProgress(transfer),
                                if (transfer.type == TYPE_DOWNLOAD)
                                    context.getString(R.string.label_transfer_over_quota)
                                else
                                    context.getString(R.string.label_storage_over_quota)
                            )
                            holder.speedText.isVisible = false
                        }
                        transfer.state == STATE_QUEUED -> {
                            holder.progressText.isVisible = false
                            holder.speedText.isVisible = false
                            holder.imageViewCompleted.isVisible = true
                            holder.textViewCompleted.isVisible = true
                            holder.textViewCompleted.text =
                                context.getString(R.string.transfer_queued)
                        }
                        else -> {
                            holder.progressText.text = getProgress(transfer)
                            holder.speedText.text =
                                context.getString(
                                    if (transfer.state == STATE_COMPLETING)
                                        R.string.transfer_completing
                                    else
                                        R.string.transfer_retrying
                                )

                        }
                    }
                }
                else -> {
                    Timber.d("Default status")
                    holder.progressText.isVisible = false
                    holder.speedText.isVisible = false
                    holder.imageViewCompleted.isVisible = false
                    holder.textViewCompleted.isVisible = false
                    holder.textViewCompleted.text = context.getString(R.string.transfer_unknown)
                    holder.optionPause.isVisible = false
                }
            }
        }

        holder.itemLayout.tag = holder
        holder.optionReorder.tag = holder
        holder.optionPause.tag = holder
    }

    override fun getItemCount(): Int = transferList.size

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getSelectedItems(): MutableList<Int>? =
        selectedItems?.let {
            mutableListOf<Int>().also { items ->
                for (i in 0 until it.size()) {
                    items.add(it.keyAt(i))
                }
            }
        }

    override fun getFolderCount(): Int = 0

    override fun getPlaceholderCount(): Int = 0

    override fun getUnhandledItem(): Int = INVALID_POSITION

    /**
     * Adds a new transfer to the adapter.
     *
     * @param transfers Updated list of transfers.
     * @param position Position of the transfer in the adapter.
     */
    fun addItemData(transfers: List<MegaTransfer>, position: Int) {
        transferList = transfers
        notifyItemInserted(position)
    }

    /**
     * Update the transfer item
     *
     * @param transfer updated transfer item
     * @param position position of the transfer in the adapter.
     */
    fun updateItemState(transfer: MegaTransfer, position: Int) {
        transferList = transferList.toMutableList().also { transfers ->
            transfers[position] = transfer
        }
        notifyItemChanged(position)
    }

    /**
     * Removes a transfer from adapter.
     * Also checks if the transfer to remove is selected. If so, removes it from selected items list
     * and updates the list with the new positions that the rest of selected transfers occupies after
     * the removal.
     *
     * @param transfers Updated list of transfers.
     * @param position Item to remove.
     */
    fun removeItemData(transfers: List<MegaTransfer>, position: Int) {
        transferList = transfers

        if (isItemChecked(position)) {
            selectedItems?.let {
                it.indexOfKey(position).let { nextIndex ->
                    it.delete(position)

                    for (i in nextIndex until it.size()) {
                        val pos = it.keyAt(i)
                        it.delete(pos)
                        it.append(pos - 1, true)
                    }
                }
                selectModeInterface.notifyItemChanged()
            }
        }

        notifyItemRemoved(position)
    }

    /**
     * Moves a transfer.
     *
     * @param transfers          Updated list of transfers.
     * @param oldPosition Old position of the transfer.
     * @param newPosition New position of the transfer.
     */
    fun moveItemData(transfers: List<MegaTransfer>, oldPosition: Int, newPosition: Int) {
        transferList = transfers
        notifyItemMoved(oldPosition, newPosition)
    }

    /**
     * Updates the progress of a transfer.
     *
     * @param position Position of the transfer in the adapter.
     * @param transfer Transfer to which the progress has to be updated.
     */
    fun updateProgress(position: Int, transfer: MegaTransfer) {
        (listView.findViewHolderForLayoutPosition(position) as? TransferViewHolder)?.let { holder ->
            if (!holder.progressText.isVisible) {
                holder.progressText.isVisible = true
                holder.speedText.isVisible = true
                holder.textViewCompleted.isVisible = false
                holder.imageViewCompleted.isVisible = false
            }

            holder.progressText.text = getProgress(transfer)
            holder.speedText.text = if (megaApi.areTransfersPaused(TYPE_DOWNLOAD)
                || megaApi.areTransfersPaused(TYPE_UPLOAD)
            ) {
                context.getString(R.string.transfer_paused)
            } else {
                Util.getSpeedString(
                    if (Util.isOnline(context))
                        transfer.speed
                    else 0)
            }
        } ?: notifyItemChanged(position)
    }

    /**
     * Set multipleSelect
     *
     * @param multipleSelect true is multiple select, otherwise is false
     */
    fun setMultipleSelect(multipleSelect: Boolean) {
        if (this.multipleSelect != multipleSelect) {
            this.multipleSelect = multipleSelect
            notifyDataSetChanged()
        }

        if (this.multipleSelect) {
            selectedItems = SparseBooleanArray()
        } else {
            selectedItems?.clear()
        }
    }

    /**
     * Hide multiple select mode
     */
    fun hideMultipleSelect() {
        setMultipleSelect(false)
        selectModeInterface.destroyActionMode()
    }

    /**
     * Select all
     */
    fun selectAll() {
        for (i in 0 until itemCount) {
            if (!isItemChecked(i)) {
                toggleSelection(i)
            }
        }
    }

    /**
     * Clear selections
     */
    fun clearSelections() {
        for (i in 0 until itemCount) {
            if (isItemChecked(i)) {
                toggleSelection(i)
            }
        }
    }

    /**
     * Get the selected transfers
     *
     * @return selected [MegaTransfer] list
     */
    fun getSelectedTransfers(): List<MegaTransfer>? = selectedItems?.let {
        mutableListOf<MegaTransfer>().let { selectedTransfers ->
            for (i in 0 until it.size()) {
                if (!it.valueAt(i)) {
                    continue
                }
                val selected = it.keyAt(i)
                if (selected < 0 || selected >= transferList.size) {
                    continue
                } else {
                    selectedTransfers.add(transferList[selected])
                }
            }
            selectedTransfers
        }
    }

    /**
     * Get the selected items count
     *
     * @return selected items count
     */
    fun getSelectedItemsCount() = selectedItems?.size() ?: 0

    private fun getTransferItem(position: Int): MegaTransfer? =
        if (position >= 0 && position < transferList.size) {
            transferList[position]
        } else {
            Timber.e("Error: position NOT valid: %s", position)
            null
        }

    /**
     * Checks if select mode is enabled. If so, checks if the position is selected.
     *
     * @param position Position to check.
     * @return True if the position is selected, false otherwise.
     */
    private fun isItemChecked(position: Int): Boolean = selectedItems?.let {
        isMultipleSelect() && position >= 0 && it[position]
    } ?: false

    /**
     * Get multipleSelect
     *
     * @return true is multiple select mode, otherwise is false
     */
    fun isMultipleSelect() = multipleSelect

    /**
     * Get the progress of a transfer.
     *
     * @param transfer transfer to get the progress
     * @return The progress of the transfer.
     */
    private fun getProgress(transfer: MegaTransfer) =
        context.getString(
            R.string.progress_size_indicator,
            if (transfer.totalBytes > 0L) (100.0 * transfer.transferredBytes / transfer.totalBytes).roundToLong() else 0L,
            Util.getSizeString(transfer.totalBytes)
        )

    /**
     * Selects or deselects a transfer with an animation view.
     *
     * @param position Position to select or deselect.
     */
    fun toggleSelection(position: Int) {
        startAnimation(position, putOrDeletePosition(position))
        selectModeInterface.notifyItemChanged()
    }

    /**
     * Applies the animation to the select or deselect action.
     *
     * @param position    Position of the view to animate.
     * @param delete True if the action is deselect, false if it is select.
     */
    private fun startAnimation(position: Int, delete: Boolean) {
        (listView.findViewHolderForLayoutPosition(position) as? TransferViewHolder)?.let { holder ->
            AnimationUtils.loadAnimation(context, R.anim.multiselect_flip).also { flipAnimation ->
                flipAnimation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(p0: Animation?) {
                        if (!delete)
                            notifyItemChanged(position)
                    }

                    override fun onAnimationEnd(p0: Animation?) {
                        if (delete)
                            notifyItemChanged(position)
                    }

                    override fun onAnimationRepeat(p0: Animation?) {
                    }

                })
                if (holder.defaultIcon.isVisible) {
                    holder.defaultIcon.startAnimation(flipAnimation)
                }
                if (holder.thumbnailIcon.isVisible) {
                    holder.thumbnailIcon.startAnimation(flipAnimation)
                }
            }
        } ?: notifyItemChanged(position)
    }

    /**
     * Checks if the current position is selected.
     * If it is, deselects it. If not, selects it.
     *
     * @param position Position to check.
     * @return True if the position is selected, false otherwise.
     */
    private fun putOrDeletePosition(position: Int) =
        selectedItems?.let {
            if (it.get(position, false)) {
                it.delete(position)
                true
            } else {
                it.append(position, true)
                false
            }
        } ?: false

    private fun showDefaultIcon(holder: TransferViewHolder, drawableId: Int) {
        holder.defaultIcon.setImageResource(drawableId)
        holder.thumbnailIcon.isVisible = false
        holder.defaultIcon.isVisible = true
    }
}