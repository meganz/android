package mega.privacy.android.app.presentation.favourites.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import nz.mega.sdk.MegaNode

/**
 * The favourite Folder entity
 * @param handle handle
 * @param icon icon drawable resource id
 * @param name name
 * @param labelColour label color resource id
 * @param showLabel label icon whether is shown
 * @param node MegaNode
 * @param hasVersion The current item if has version
 * @param info the current file info
 * @param isFavourite whether is favourite
 * @param isExported whether is exported
 * @param isTakenDown whether is taken down
 * @param isAvailableOffline whether is available for offline
 */
data class FavouriteFolder(
    override val handle: Long,
    @DrawableRes override val icon: Int,
    override val name: String,
    @ColorRes override val labelColour: Int,
    override val showLabel: Boolean,
    override val node: MegaNode,
    override val hasVersion: Boolean,
    override val info: String,
    override val isFavourite: Boolean,
    override val isExported: Boolean,
    override val isTakenDown: Boolean,
    override val isAvailableOffline: Boolean
) : Favourite{
    override val isFolder = true
}