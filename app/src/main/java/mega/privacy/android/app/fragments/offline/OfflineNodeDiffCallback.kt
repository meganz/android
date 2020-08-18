package mega.privacy.android.app.fragments.offline

import androidx.recyclerview.widget.DiffUtil

class OfflineNodeDiffCallback : DiffUtil.ItemCallback<OfflineNode>() {
    override fun areItemsTheSame(oldItem: OfflineNode, newItem: OfflineNode): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: OfflineNode, newItem: OfflineNode): Boolean {
        return oldItem.node.handle == newItem.node.handle
    }
}
