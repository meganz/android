package mega.privacy.android.app.fragments.offline

import androidx.recyclerview.widget.DiffUtil

class OfflineNodeDiffCallback : DiffUtil.ItemCallback<OfflineNode>() {
    override fun areItemsTheSame(oldItem: OfflineNode, newItem: OfflineNode) =
        oldItem.node.handle == newItem.node.handle

    override fun areContentsTheSame(oldItem: OfflineNode, newItem: OfflineNode) = !newItem.uiDirty
}
