package mega.privacy.android.feature.devicecenter.ui.model

import androidx.annotation.DrawableRes
import mega.privacy.android.feature.devicecenter.R

/**
 * Data class representing the state of the Device Center Info View
 *
 * @property icon Icon of the item
 * @property name Name of the item
 * @property numberOfFiles The number of files this item and its sub-folders has
 * @property numberOfFolders The number of folders and sub-folders this item has, including itself
 * @property totalSizeInBytes The size of the item in bytes
 */
data class DeviceCenterInfoUiState(
    @DrawableRes val icon: Int = R.drawable.ic_device_folder,
    val name: String = String(),
    val numberOfFiles: Int = 0,
    val numberOfFolders: Int = 0,
    val totalSizeInBytes: Long = 0,
)