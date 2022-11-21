package mega.privacy.android.app.presentation.favourites.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import mega.privacy.android.domain.entity.node.TypedFolderNode
import nz.mega.sdk.MegaNode

/**
 * The favourite Folder entity
 */
data class FavouriteFolder(
    @DrawableRes override val icon: Int,
    @ColorRes override val labelColour: Int,
    override val showLabel: Boolean,
    override val node: MegaNode,
    override val info: String,
    override val isSelected: Boolean = false,
    override val isAvailableOffline: Boolean,
    override val typedNode: TypedFolderNode,
) : Favourite {
    override val thumbnailPath: String? = null

}