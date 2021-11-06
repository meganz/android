package mega.privacy.android.app.imageviewer.data

import android.net.Uri
import androidx.recyclerview.widget.DiffUtil
import mega.privacy.android.app.usecase.data.MegaNodeItem

data class ImageItem constructor(
    val handle: Long,
    val name: String,
    val isVideo: Boolean = false,
    val nodeItem: MegaNodeItem? = null,
    var thumbnailUri: Uri? = null,
    var previewUri: Uri? = null,
    var fullSizeUri: Uri? = null,
    var transferTag: Int? = null,
    var isFullyLoaded: Boolean = false
) {

    fun getHighestResolutionAvailableUri(): Uri? =
        fullSizeUri ?: previewUri ?: thumbnailUri

    fun getLowestResolutionAvailableUri(): Uri? =
        thumbnailUri ?: previewUri ?: fullSizeUri

    class DiffCallback : DiffUtil.ItemCallback<ImageItem>() {
        override fun areItemsTheSame(oldItem: ImageItem, newItem: ImageItem) =
            oldItem.handle == newItem.handle

        override fun areContentsTheSame(oldItem: ImageItem, newItem: ImageItem) =
            oldItem == newItem
    }
}
