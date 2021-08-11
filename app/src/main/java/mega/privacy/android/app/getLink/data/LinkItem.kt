package mega.privacy.android.app.getLink.data

import androidx.recyclerview.widget.DiffUtil
import nz.mega.sdk.MegaNode

data class LinkItem(
    val node: MegaNode,
    val name: String,
    val link: String,
    val info: String
) {

    class DiffCallback : DiffUtil.ItemCallback<LinkItem>() {

        override fun areItemsTheSame(oldItem: LinkItem, newItem: LinkItem): Boolean =
            oldItem.node.handle == newItem.node.handle

        override fun areContentsTheSame(oldItem: LinkItem, newItem: LinkItem): Boolean =
            oldItem == newItem
    }
}
