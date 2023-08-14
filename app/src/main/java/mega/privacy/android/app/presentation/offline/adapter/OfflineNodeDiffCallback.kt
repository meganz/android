package mega.privacy.android.app.presentation.offline.adapter

import androidx.recyclerview.widget.DiffUtil
import mega.privacy.android.app.presentation.offline.model.OfflineNode

class OfflineNodeDiffCallback : DiffUtil.ItemCallback<OfflineNode>() {
    override fun areItemsTheSame(oldItem: OfflineNode, newItem: OfflineNode) =
        oldItem.node.handle == newItem.node.handle

    override fun areContentsTheSame(oldItem: OfflineNode, newItem: OfflineNode) = !newItem.uiDirty
}
