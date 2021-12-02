package mega.privacy.android.app.gallery.data

import android.text.Spanned
import android.util.Pair
import androidx.recyclerview.widget.DiffUtil
import mega.privacy.android.app.fragments.homepage.NodeItem
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import nz.mega.sdk.MegaNode
import java.io.File
import java.util.stream.Collectors

/**
 * Creates a TYPE_IMAGE or TYPE_VIDEO CuNode.
 *
 * @param node           MegaNode representing the item.
 * @param indexForViewer Index needed on viewers to show the dismiss animation after a drag event.
 * @param thumbnail      Thumbnail or preview of the node if exists, null otherwise.
 * @param type           TYPE_IMAGE if photo, TYPE_VIDEO if video.
 * @param modifyDate     String containing the modified date of the node.
 * @param headerDate     Pair containing the text to show as header in adapter:
 *                          - First: Month.
 *                          - Second: Year if not current year, empty otherwise.
 * @param isSelected       True if the node is selected on list, false otherwise.
 */
data class GalleryItem(
    var node: MegaNode?,
    var indexForViewer: Int,
    var photoIndex: Int,
    var index: Int,
    var thumbnail: File?,
    var type: Int,
    var modifyDate: String,
    var formattedDate: Spanned?,
    var headerDate: Pair<String, String>?,
    var isSelected: Boolean,
    var uiDirty: Boolean
) {

    /**
     * Creates a TYPE_HEADER which represent date.
     *
     * @param modifyDate String containing the complete header date.
     * @param headerDate Pair containing the text to show as header in adapter:
     *                   - First: Month.
     *                   - Second: Year if not current year, empty otherwise.
     */
    constructor(
        modifyDate: String,
        headerDate: Pair<String, String>
    ) : this(
        null, INVALID_POSITION, INVALID_POSITION, INVALID_POSITION, null,
        TYPE_HEADER, modifyDate, null, headerDate, false, false
    )

    class DiffCallback : DiffUtil.ItemCallback<GalleryItem>() {
        override fun areItemsTheSame(oldItem: GalleryItem, newItem: GalleryItem) =
            oldItem.node?.handle == newItem.node?.handle

        override fun areContentsTheSame(oldItem: GalleryItem, newItem: GalleryItem) =
            oldItem == newItem
    }

    fun toNodeItem() = NodeItem(
        node,
        index,
        false,
        modifyDate,
        thumbnail,
        isSelected,
        uiDirty
    )



    companion object {

        @JvmStatic
        fun fromNodeItem(nodeItem: NodeItem) = GalleryItem(
            nodeItem.node,
            INVALID_POSITION,
            INVALID_POSITION,
            nodeItem.index,
            nodeItem.thumbnail,
            TYPE_IMAGE,
            nodeItem.modifiedDate,
            null,
            null,
            nodeItem.selected,
            false
        )

        @JvmStatic
        fun toNodeItems(galleryItems: List<GalleryItem>): List<NodeItem> =
            galleryItems.stream().map {
                NodeItem(
                    it.node,
                    it.index,
                    false,
                    it.modifyDate,
                    it.thumbnail,
                    it.isSelected,
                    it.uiDirty
                )
            }.collect(Collectors.toList())

        /**
         * Three different types of nodes.
         */
        const val TYPE_HEADER = 1
        const val TYPE_IMAGE = 2
        const val TYPE_VIDEO = 3
    }
}