package mega.privacy.android.app.fragments.recent

import androidx.recyclerview.widget.DiffUtil
import mega.privacy.android.app.fragments.homepage.NodeItem

class RecentsBucketDiffCallback : DiffUtil.ItemCallback<NodeItem>() {
    override fun areItemsTheSame(oldItem: NodeItem, newItem: NodeItem) =
        oldItem.node?.handle == newItem.node?.handle

    override fun areContentsTheSame(oldItem: NodeItem, newItem: NodeItem) = !newItem.uiDirty
}