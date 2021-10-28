package mega.privacy.android.app.getLink.data

import androidx.recyclerview.widget.DiffUtil
import nz.mega.sdk.MegaNode
import java.io.File

/**
 * Class used for draw link list items.
 *
 * @property id Unique identifier:
 *              - The MegaNode handle for Data.
 *              - Long get from title for Header.
 *
 * @see [mega.privacy.android.app.getLink.adapter.LinksAdapter].
 */
sealed class LinkItem(val id: Long) {

    /**
     * Data class used for draw link list headers items.
     *
     * @property title Title of the header.
     * @see [mega.privacy.android.app.getLink.adapter.LinkHeaderViewHolder].
     */
    data class Header constructor(val title: String) : LinkItem(title.hashCode().toLong())

    /**
     * Data class used for draw link list data items.
     *
     * @property node      MegaNode exported or pending to be exported.
     * @property thumbnail File containing the thumbnail if exists, null otherwise.
     * @property name      Name of the node.
     * @property link      Link of the node if is exported, null otherwise.
     * @property info      Sized of the node if is a file, folder info is if a folder.
     * @see [mega.privacy.android.app.getLink.adapter.LinkViewHolder].
     */
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
