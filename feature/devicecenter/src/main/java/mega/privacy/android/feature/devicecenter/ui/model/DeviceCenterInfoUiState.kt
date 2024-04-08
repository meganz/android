package mega.privacy.android.feature.devicecenter.ui.model

import androidx.annotation.DrawableRes
import mega.privacy.android.icon.pack.R

/**
 * Data class representing the state of the Device Center Info View
 *
 * @property icon Icon of the item
 * @property applySecondaryColorIconTint If true, applies the secondary color to the icon
 * @property name Name of the item
 * @property numberOfFiles The number of files this item and its sub-folders has
 * @property numberOfFolders The number of folders and sub-folders this item has, including itself
 * @property totalSizeInBytes The size of the item in bytes
 * @property creationTime The creation time
 */
data class DeviceCenterInfoUiState(
    @DrawableRes val icon: Int = R.drawable.ic_folder_medium_solid,
    @Deprecated(
        "Temporary used in order to fix icon color until we change to the new icon set. Will be removed soon."
    ) val applySecondaryColorIconTint: Boolean = false,
    val name: String = String(),
    val numberOfFiles: Int = 0,
    val numberOfFolders: Int = 0,
    val totalSizeInBytes: Long = 0,
    val creationTime: Long = 0,
)