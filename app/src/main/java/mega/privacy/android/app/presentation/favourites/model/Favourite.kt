package mega.privacy.android.app.presentation.favourites.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import nz.mega.sdk.MegaNode

/**
 * The favourite interface
 */
sealed interface Favourite {
    val handle: Long
    val isFolder: Boolean

    @get:DrawableRes
    val icon: Int
    val name: String

    @get:ColorRes
    val labelColour: Int
    val showLabel: Boolean
    val node: MegaNode
    val hasVersion: Boolean
    val info: String // Replace with folderInfo property on folder, and size and modified date on file
    val isFavourite: Boolean
    val isExported: Boolean
    val isTakenDown: Boolean
    val isAvailableOffline: Boolean
}