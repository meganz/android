package mega.privacy.android.app.presentation.favourites.model

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import mega.privacy.android.domain.entity.node.TypedFileNode
import nz.mega.sdk.MegaNode

/**
 * The favourite file entity
 */
data class FavouriteFile(
    @DrawableRes override val icon: Int,
    @ColorRes override val labelColour: Int,
    override val showLabel: Boolean,
    override val node: MegaNode,
    override val info: (Context) -> String,
    override val isSelected: Boolean = false,
    override val thumbnailPath: String?,
    override val isAvailableOffline: Boolean,
    override val typedNode: TypedFileNode,
) : Favourite