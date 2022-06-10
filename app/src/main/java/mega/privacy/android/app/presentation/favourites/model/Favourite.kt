package mega.privacy.android.app.presentation.favourites.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import nz.mega.sdk.MegaNode

/**
 * The favourite interface
 * @property handle handle
 * @property isFolder current item whether is folder
 * @property icon icon drawable resource id
 * @property name name
 * @property size size
 * @property label label
 * @property modificationTime modification time
 * @property labelColour label color resource id
 * @property showLabel label icon whether is shown
 * @property node MegaNode
 * @property hasVersion The current item if has version
 * @property info the current file info
 * @property isFavourite whether is favourite
 * @property isExported whether is exported
 * @property isTakenDown whether is taken down
 * @property isAvailableOffline whether is available for offline
 * @property isSelected whether is selected in action mode
 * @property thumbnailPath thumbnail file path
 */
sealed interface Favourite {
    val handle: Long
    val isFolder: Boolean

    @get:DrawableRes
    val icon: Int
    val name: String
    val size: Long
    val label: Int
    val modificationTime: Long

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
    val isSelected: Boolean
    val thumbnailPath: String?
}