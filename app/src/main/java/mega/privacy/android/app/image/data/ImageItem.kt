package mega.privacy.android.app.image.data

import android.net.Uri
import androidx.recyclerview.widget.DiffUtil

data class ImageItem(
    val handle: Long,
    val name: String,
    val uri: Uri
) {

    class DiffCallback : DiffUtil.ItemCallback<ImageItem>() {
        override fun areItemsTheSame(oldItem: ImageItem, newItem: ImageItem) =
            oldItem.handle == newItem.handle

        override fun areContentsTheSame(oldItem: ImageItem, newItem: ImageItem) =
            oldItem == newItem
    }
}
