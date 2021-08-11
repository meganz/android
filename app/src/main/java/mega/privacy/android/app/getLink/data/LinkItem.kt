package mega.privacy.android.app.getLink.data

import androidx.recyclerview.widget.DiffUtil
import nz.mega.sdk.MegaNode
import java.io.File

data class LinkItem(
    val node: MegaNode,
    var thumbnail: File?,
    val name: String,
    val link: String,
    val info: String
) {

    class DiffCallback : DiffUtil.ItemCallback<LinkItem>() {

        override fun areItemsTheSame(oldItem: LinkItem, newItem: LinkItem): Boolean =
            oldItem.node.handle == newItem.node.handle

        override fun areContentsTheSame(oldItem: LinkItem, newItem: LinkItem): Boolean =
            areItemsTheSame(oldItem, newItem)
                    && oldItem.thumbnail == newItem.thumbnail
                    && oldItem.name == newItem.name
                    && oldItem.link == newItem.link
                    && oldItem.info == newItem.info
    }
}
