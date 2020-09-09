package mega.privacy.android.app.fragments.homepage

import androidx.recyclerview.widget.DiffUtil.ItemCallback

class NodeDiffCallback : ItemCallback<NodeItem>() {
    override fun areItemsTheSame(oldItem: NodeItem, newItem: NodeItem): Boolean {
        return oldItem.node?.handle == newItem.node?.handle
    }

    override fun areContentsTheSame(oldItem: NodeItem, newItem: NodeItem): Boolean {
        if (newItem.uiDirty) {
            return false
        }

        return true
    }
}
