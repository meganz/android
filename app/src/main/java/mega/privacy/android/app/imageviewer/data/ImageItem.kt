package mega.privacy.android.app.imageviewer.data

import androidx.recyclerview.widget.DiffUtil
import mega.privacy.android.app.usecase.data.MegaNodeItem

/**
 * Data object that encapsulates an item representing an Image.
 *
 * @property handle         Image node handle.
 * @property publicLink     Node public link.
 * @property isOffline      Is Offline node.
 * @property nodeItem       Image node item.
 * @property imageResult    Image result containing each Image Uri.
 */
data class ImageItem constructor(
    val handle: Long,
    val publicLink: String?,
    val isOffline: Boolean,
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
