package mega.privacy.android.app.fragments.homepage

import androidx.recyclerview.widget.DiffUtil.ItemCallback

class NodeDiffCallback<T : NodeItem> : ItemCallback<T>() {
    override fun areItemsTheSame(oldItem: T, newItem: T) =
        oldItem.node?.handle == newItem.node?.handle

    override fun areContentsTheSame(oldItem: T, newItem: T) = !newItem.uiDirty
}
