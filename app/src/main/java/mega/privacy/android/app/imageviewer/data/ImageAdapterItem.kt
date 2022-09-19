package mega.privacy.android.app.imageviewer.data

import androidx.recyclerview.widget.DiffUtil

data class ImageAdapterItem constructor(
    val id: Long,
    val hash: Int,
) {
    class DiffCallback : DiffUtil.ItemCallback<ImageAdapterItem>() {
        override fun areItemsTheSame(oldItem: ImageAdapterItem, newItem: ImageAdapterItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ImageAdapterItem, newItem: ImageAdapterItem) =
            oldItem == newItem
    }
}
