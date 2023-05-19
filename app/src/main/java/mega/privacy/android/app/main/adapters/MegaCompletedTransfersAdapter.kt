package mega.privacy.android.app.main.adapters

import android.content.Context
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.utils.ColorUtils.getThemeColor
import mega.privacy.android.app.utils.Constants.THUMB_CORNER_RADIUS_DP
import mega.privacy.android.app.utils.ThumbnailUtils.getRoundedBitmap
import mega.privacy.android.app.utils.ThumbnailUtils.getThumbnailFromCache
import mega.privacy.android.app.utils.ThumbnailUtils.getThumbnailFromFolder
import mega.privacy.android.app.utils.Util.dp2px
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaTransfer.STATE_CANCELLED
import nz.mega.sdk.MegaTransfer.STATE_COMPLETED
import nz.mega.sdk.MegaTransfer.STATE_FAILED
import nz.mega.sdk.MegaTransfer.TYPE_DOWNLOAD
import timber.log.Timber

/**
 * The adapter for mega completed transfers
 */
class MegaCompletedTransfersAdapter(
    private val context: Context,
    transfers: List<CompletedTransfer?>,
    private val megaApi: MegaApiAndroid,
) : RecyclerView.Adapter<TransferViewHolder>() {

    private var completedTransfers: List<CompletedTransfer?>

    init {
        completedTransfers = transfers
    }

    /**
     * Set completed transfers
     *
     * @param transfers List<[CompletedTransfer]>
     */
    fun setCompletedTransfers(transfers: List<CompletedTransfer?>) {
        this.completedTransfers = transfers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransferViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_transfers_list, parent, false)
        val holder = TransferViewHolder(view)
        with(holder) {
            itemLayout = view.findViewById(R.id.transfers_list_item_layout)
            thumbnailIcon = view.findViewById(R.id.transfers_list_thumbnail)
            defaultIcon = view.findViewById(R.id.transfers_list_default_icon)
            iconDownloadUploadView = view.findViewById(R.id.transfers_list_small_icon)
            textViewFileName = view.findViewById(R.id.transfers_list_filename)
            progressText = view.findViewById(R.id.transfers_progress_text)
            speedText = view.findViewById(R.id.transfers_speed_text)
            imageViewCompleted = view.findViewById(R.id.transfers_list_completed_image)
            textViewCompleted = view.findViewById(R.id.transfers_list_completed_text)
            optionReorder = view.findViewById(R.id.transfers_list_option_reorder)
            optionPause = view.findViewById(R.id.transfers_list_option_pause)

            itemLayout.setOnClickListener {
                Timber.d("onClick")
                (context as? ManagerActivity)?.showManageTransferOptionsPanel(
                    getItem(holder.absoluteAdapterPosition))
            }
        }
        view.tag = view
        return holder
    }

    override fun onBindViewHolder(holder: TransferViewHolder, position: Int) {
        getItem(position)?.let { completedTransfer ->
            val fileName = completedTransfer.fileName

            with(holder) {
                textViewFileName.text = fileName
                optionPause.isVisible = false
                optionReorder.isVisible = false
                progressText.isVisible = false
                speedText.isVisible = false

                defaultIcon.setImageResource(MimeTypeList.typeForName(fileName).iconResourceId)
                MimeTypeList.typeForName(fileName).let { mimeTypeList ->
                    if (mimeTypeList.isImage || mimeTypeList.isVideo) {
                        completedTransfer.handle?.let { handle ->
                            val thumbnail =
                                getThumbnailFromCache(handle) ?: megaApi.getNodeByHandle(handle)
                                    ?.let { node ->
                                        getThumbnailFromFolder(node, context)
                                    }
                            if (thumbnail != null)
                                thumbnailIcon.setImageBitmap(
                                    getRoundedBitmap(context,
                                        thumbnail,
                                        dp2px(THUMB_CORNER_RADIUS_DP)))
                            defaultIcon.isVisible = thumbnail == null
                            thumbnailIcon.isVisible = thumbnail != null
                        }
                    }
                }
                iconDownloadUploadView.setImageResource(
                    if (completedTransfer.type == TYPE_DOWNLOAD)
                        R.drawable.ic_download_transfers
                    else
                        R.drawable.ic_upload_transfers
                )
                textViewCompleted.setTextColor(getThemeColor(context,
                    android.R.attr.textColorSecondary))

                val stateParams = imageViewCompleted.layoutParams as RelativeLayout.LayoutParams
                stateParams.marginEnd = dp2px(5f, context.resources.displayMetrics)
                when (completedTransfer.state) {
                    STATE_COMPLETED -> {
                        textViewCompleted.text = completedTransfer.path
                        imageViewCompleted.setColorFilter(ContextCompat.getColor(context,
                            R.color.green_500_300),
                            PorterDuff.Mode.SRC_IN)
                        imageViewCompleted.setImageResource(R.drawable.ic_transfers_completed)
                    }
                    STATE_FAILED -> {
                        textViewCompleted.setTextColor(getThemeColor(context, com.google.android.material.R.attr.colorError))
                        textViewCompleted.text = String.format("%s: %s",
                            context.getString(R.string.failed_label),
                            completedTransfer.error)
                        stateParams.marginEnd = 0
                        imageViewCompleted.setImageBitmap(null)
                    }
                    STATE_CANCELLED -> {
                        textViewCompleted.text = context.getString(R.string.transfer_cancelled)
                        stateParams.marginEnd = 0
                        imageViewCompleted.setImageBitmap(null)
                    }
                    else -> {
                        textViewCompleted.text = context.getString(R.string.transfer_unknown)
                        imageViewCompleted.clearColorFilter()
                        imageViewCompleted.setImageResource(R.drawable.ic_queue)
                    }
                }
                imageViewCompleted.layoutParams = stateParams
            }
        }
    }

    override fun getItemCount(): Int = completedTransfers.size

    /**
     * Get transfer item
     *
     * @param position the item position
     * @return transfer item
     */
    fun getItem(position: Int) = completedTransfers.getOrNull(position)

    override fun getItemId(position: Int) = position.toLong()

    /**
     * Remove item
     *
     * @param position the removed position
     * @param transfers the transfer list after removed
     */
    fun removeItemData(position: Int, transfers: List<CompletedTransfer?>) {
        completedTransfers = transfers
        notifyItemRemoved(position)
    }
}
