package mega.privacy.android.app.gallery.data

import android.util.Pair
import mega.privacy.android.app.utils.Constants
import nz.mega.sdk.MegaNode
import java.io.File

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
    var thumbnail: File?,
    var type: Int,
    var modifyDate: String,
    var headerDate: Pair<String, String>?,
    var isSelected: Boolean
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
        null, Constants.INVALID_VALUE, null,
        TYPE_HEADER, modifyDate, headerDate, false
    )

    companion object {

        /**
         * Three different types of nodes.
         */
        const val TYPE_HEADER = 1
        const val TYPE_IMAGE = 2
        const val TYPE_VIDEO = 3
    }
}