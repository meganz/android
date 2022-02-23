package mega.privacy.android.app.imageviewer.data

import androidx.recyclerview.widget.DiffUtil
import mega.privacy.android.app.usecase.data.MegaNodeItem

/**
 * Data object that encapsulates an item representing an Image.
 *
 * @property handle             Image node handle.
 * @property nodePublicLink     Node public link.
 * @property chatRoomId         Node Chat Message Room Id.
 * @property chatMessageId      Node Chat Message Id.
 * @property nodeItem           Image node item.
 * @property imageResult        Image result containing each Image Uri.
 * @property isOffline          Is Offline node.
 * @property isChatDeletable    Flag indicating if chat node can be deleted.
 */
data class ImageItem constructor(
    val handle: Long,
    val nodePublicLink: String? = null,
    val chatRoomId: Long? = null,
    val chatMessageId: Long? = null,
    val nodeItem: MegaNodeItem? = null,
    val imageResult: ImageResult? = null,
    val isOffline: Boolean = false,
    val isChatDeletable: Boolean = false
) {

    fun isFromChat(): Boolean =
        chatMessageId != null && chatRoomId != null

    class DiffCallback : DiffUtil.ItemCallback<ImageItem>() {
        override fun areItemsTheSame(oldItem: ImageItem, newItem: ImageItem) =
            oldItem.handle == newItem.handle

        override fun areContentsTheSame(oldItem: ImageItem, newItem: ImageItem) =
            oldItem == newItem
                    && oldItem.nodeItem == newItem.nodeItem
                    && oldItem.imageResult == newItem.imageResult
    }
}
