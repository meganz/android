package mega.privacy.android.app.presentation.favourites.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import mega.privacy.android.domain.entity.node.TypedNode
import nz.mega.sdk.MegaNode


/**
 * Favourite
 *
 * @constructor Create empty Favourite
 */
sealed interface Favourite {
    /**
     * Icon
     */
    @get:DrawableRes
    val icon: Int

    /**
     * Label colour
     */
    @get:ColorRes
    val labelColour: Int

    /**
     * Show label
     */
    val showLabel: Boolean

    /**
     * Node
     */
    val node: MegaNode

    /**
     * Info
     */
    val info: String // Replace with folderInfo property on folder, and size and modified date on file

    /**
     * Is selected
     */
    val isSelected: Boolean

    /**
     * Thumbnail path
     */
    val thumbnailPath: String?

    /**
     * Typed node
     */
    val typedNode: TypedNode

    /**
     * Is available offline
     */
    val isAvailableOffline: Boolean
}