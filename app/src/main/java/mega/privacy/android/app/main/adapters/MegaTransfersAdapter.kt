package mega.privacy.android.app.main.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.LegacyDatabaseHandler
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.core.R as CoreUiR
import mega.privacy.android.app.R
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.managerSections.TransfersViewModel
import mega.privacy.android.app.presentation.extensions.getStorageState
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.Constants.THUMB_CORNER_RADIUS_DP
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaTransfer
import timber.log.Timber
import kotlin.math.roundToLong

/**
 * The adapter for transfer section
 */
class MegaTransfersAdapter(
    private val context: Context,
    private val listView: RecyclerView,
    private val selectModeInterface: SelectModeInterface,
    private val transfersViewModel: TransfersViewModel,
    private val megaApi: MegaApiAndroid,
    private val megaApiFolder: MegaApiAndroid,
    private val dbH: LegacyDatabaseHandler,
) : ListAdapter<Transfer, TransferViewHolder>(TRANSFER_DIFF_CALLBACK), RotatableAdapter {

    private var multipleSelect: Boolean = false
    private val selectedItems = mutableSetOf<Int>()

    override fun submitList(list: List<Transfer>?) {
        super.submitList(list)
        if (isMultipleSelect() && list != null) {
            val newSelectedItem =
                list.mapNotNull { if (selectedItems.contains(it.tag)) it.tag else null }
            val isChanged = newSelectedItem.size == selectedItems.size
            selectedItems.clear()
            selectedItems.addAll(newSelectedItem)
            if (isChanged) {
                selectModeInterface.notifyItemChanged()
            }
        }
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
        val isItemChecked = isItemChecked(transfer)
        when (transfer.transferType) {
            TransferType.TYPE_DOWNLOAD -> {
                holder.progressText.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.green_500_green_400
                    )
                )
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
                                        ThumbnailUtils.getThumbnailFromMegaTransfer(
                                            node,
                                            context,
                                            holder,
                                            megaApi,
                                            this
                                        )
                                    } catch (e: Exception) {
                                        Timber.e(e, "Exception getting thumbnail")
                                        null
                                    }
                                }
                            }
                        } else {
                            null
                        }?.let { thumbnail ->
                            holder.thumbnailIcon.setImageBitmap(
                                ThumbnailUtils.getRoundedBitmap(
                                    context,
                                    thumbnail,
                                    Util.dp2px(THUMB_CORNER_RADIUS_DP)
                                )
                            )
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

            TransferType.TYPE_UPLOAD -> {
                if (!isItemChecked) {
                    holder.iconDownloadUploadView.setImageResource(R.drawable.ic_upload_transfers)
                    showDefaultIcon(
                        holder = holder,
                        drawableId = MimeTypeList.typeForName(transfer.fileName).iconResourceId
                    )
                }
            }

            TransferType.NONE -> {}
        }

        holder.iconDownloadUploadView.isVisible = !isItemChecked
        if (isItemChecked) {
            showDefaultIcon(holder = holder, drawableId = CoreUiR.drawable.ic_select_folder)
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

        if (dbH.transferQueueStatus) {
            holder.progressText.text = getProgress(transfer)
            holder.speedText.text = context.getString(R.string.transfer_paused)

            if (!isMultipleSelect() && transfer.state == TransferState.STATE_PAUSED) {
                holder.optionPause.setImageResource(R.drawable.ic_play_grey)
            }
        } else {
            when (transfer.state) {
                TransferState.STATE_PAUSED -> {
                    holder.progressText.text = getProgress(transfer)
                    holder.speedText.text = context.getString(R.string.transfer_paused)
                    holder.speedText.isVisible = true

                    if (!isMultipleSelect()) {
                        holder.optionPause.setImageResource(R.drawable.ic_play_grey)
                    }
                }

                TransferState.STATE_ACTIVE -> {
                    holder.progressText.text = getProgress(transfer)
                    holder.speedText.text = Util.getSpeedString(
                        if (Util.isOnline(context))
                            transfer.speed
                        else
                            0, context
                    )
                    holder.speedText.isVisible = true
                }

                TransferState.STATE_COMPLETING,
                TransferState.STATE_RETRYING,
                TransferState.STATE_QUEUED,
                -> {
                    when {
                        (transfer.transferType == TransferType.TYPE_DOWNLOAD && transfersViewModel.isOnTransferOverQuota())
                                || (transfer.transferType == TransferType.TYPE_UPLOAD && getStorageState() == StorageState.Red)
                        -> {
                            holder.progressText.setTextColor(
                                ContextCompat.getColor(
                                    context,
                                    R.color.orange_400_orange_300
                                )
                            )
                            holder.progressText.text = String.format(
                                "%s %s",
                                getProgress(transfer),
                                if (transfer.transferType == TransferType.TYPE_DOWNLOAD)
                                    context.getString(R.string.label_transfer_over_quota)
                                else
                                    context.getString(R.string.label_storage_over_quota)
                            )
                            holder.speedText.isVisible = false
                        }

                        transfer.state == TransferState.STATE_QUEUED -> {
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
                                    if (transfer.state == TransferState.STATE_COMPLETING)
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

    override fun getItemCount(): Int = currentList.size

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getSelectedItems(): MutableList<Int> = selectedItems.toMutableList()

    override fun getFolderCount(): Int = 0

    override fun getPlaceholderCount(): Int = 0

    override fun getUnhandledItem(): Int = INVALID_POSITION

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
        selectedItems.clear()
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
        currentList.forEachIndexed { index, transfer ->
            if (!isItemChecked(transfer)) {
                toggleSelection(index)
            }
        }
    }

    /**
     * Clear selections
     */
    fun clearSelections() {
        currentList.forEachIndexed { index, transfer ->
            if (isItemChecked(transfer)) {
                toggleSelection(index)
            }
        }
    }

    /**
     * Get the selected transfers
     *
     * @return selected [MegaTransfer] list
     */
    fun getSelectedTransfers(): List<Transfer> =
        currentList.filter { selectedItems.contains(it.tag) }

    /**
     * Get the selected items count
     *
     * @return selected items count
     */
    fun getSelectedItemsCount() = selectedItems.size

    private fun getTransferItem(position: Int): Transfer? = currentList.getOrNull(position)

    /**
     * Checks if select mode is enabled. If so, checks if the position is selected.
     *
     * @param transfer transfer to check.
     * @return True if the position is selected, false otherwise.
     */
    private fun isItemChecked(transfer: Transfer): Boolean =
        isMultipleSelect() && selectedItems.contains(transfer.tag)

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
    private fun getProgress(transfer: Transfer) =
        context.getString(
            R.string.progress_size_indicator,
            if (transfer.totalBytes > 0L) (100.0 * transfer.transferredBytes / transfer.totalBytes).roundToLong() else 0L,
            Util.getSizeString(transfer.totalBytes, context)
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
    private fun putOrDeletePosition(position: Int) = currentList.getOrNull(position)?.let {
        with(selectedItems.contains(it.tag)) {
            if (this) selectedItems.remove(it.tag) else selectedItems.add(it.tag)
        }
    } ?: false

    private fun showDefaultIcon(holder: TransferViewHolder, drawableId: Int) {
        holder.defaultIcon.setImageResource(drawableId)
        holder.thumbnailIcon.isVisible = false
        holder.defaultIcon.isVisible = true
    }

    companion object {
        private val TRANSFER_DIFF_CALLBACK = object : DiffUtil.ItemCallback<Transfer>() {
            override fun areItemsTheSame(oldItem: Transfer, newItem: Transfer): Boolean =
                oldItem.tag == newItem.tag

            override fun areContentsTheSame(oldItem: Transfer, newItem: Transfer): Boolean =
                oldItem == newItem
        }
    }
}