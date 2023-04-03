package mega.privacy.android.app.presentation.favourites.model

import android.content.Context
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
    val info: (Context) -> String

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