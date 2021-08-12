package mega.privacy.android.app.getLink.data

import androidx.recyclerview.widget.DiffUtil
import nz.mega.sdk.MegaNode
import java.io.File

sealed class LinkItem(val id: Long) {

    data class Header constructor(val title: String) : LinkItem(title.hashCode().toLong())

    data class Data constructor(
        val node: MegaNode,
        var thumbnail: File?,
        val name: String,
        var link: String?,
        val info: String
    ) : LinkItem(node.handle)

    class DiffCallback : DiffUtil.ItemCallback<LinkItem>() {

        override fun areItemsTheSame(oldItem: LinkItem, newItem: LinkItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: LinkItem, newItem: LinkItem): Boolean {
            val isSameHeader = oldItem is Header && newItem is Header && oldItem == newItem
            val isSameData = oldItem is Data && newItem is Data
                    && areItemsTheSame(oldItem, newItem)
                    && oldItem.thumbnail == newItem.thumbnail
                    && oldItem.name == newItem.name
                    && oldItem.link == newItem.link
                    && oldItem.info == newItem.info

            return isSameHeader || isSameData
        }
    }
}
