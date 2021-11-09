package mega.privacy.android.app.imageviewer.data

import androidx.recyclerview.widget.DiffUtil
import mega.privacy.android.app.usecase.data.MegaNodeItem

data class ImageItem constructor(
    val handle: Long,
    val name: String,
    val nodeItem: MegaNodeItem? = null,
    val imageResult: ImageResult? = null
) {

    class DiffCallback : DiffUtil.ItemCallback<ImageItem>() {
        override fun areItemsTheSame(oldItem: ImageItem, newItem: ImageItem) =
            oldItem.handle == newItem.handle

        override fun areContentsTheSame(oldItem: ImageItem, newItem: ImageItem) =
            oldItem == newItem
    }
}
