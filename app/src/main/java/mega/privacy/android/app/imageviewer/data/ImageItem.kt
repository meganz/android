package mega.privacy.android.app.imageviewer.data

import android.net.Uri
import androidx.recyclerview.widget.DiffUtil

data class ImageItem constructor(
    val handle: Long,
    val name: String,
    val isVideo: Boolean = false,
    var thumbnailUri: Uri? = null,
    var previewUri: Uri? = null,
    var fullSizeUri: Uri? = null,
    var transferTag: Int? = null,
    var isFullyLoaded: Boolean = false
) {

    fun getAvailableUri(): Uri? =
        fullSizeUri ?: previewUri ?: thumbnailUri

    class DiffCallback : DiffUtil.ItemCallback<ImageItem>() {
        override fun areItemsTheSame(oldItem: ImageItem, newItem: ImageItem) =
            oldItem.handle == newItem.handle

        override fun areContentsTheSame(oldItem: ImageItem, newItem: ImageItem) =
            oldItem == newItem
    }
}
