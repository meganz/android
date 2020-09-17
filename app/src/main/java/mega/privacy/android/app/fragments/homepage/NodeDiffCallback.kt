package mega.privacy.android.app.fragments.homepage

import androidx.recyclerview.widget.DiffUtil.ItemCallback

class NodeDiffCallback : ItemCallback<NodeItem>() {
    override fun areItemsTheSame(oldItem: NodeItem, newItem: NodeItem) =
        oldItem.node?.handle == newItem.node?.handle

    override fun areContentsTheSame(oldItem: NodeItem, newItem: NodeItem) = !newItem.uiDirty
}
