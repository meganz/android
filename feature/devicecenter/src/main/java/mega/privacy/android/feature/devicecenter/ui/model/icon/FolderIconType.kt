package mega.privacy.android.feature.devicecenter.ui.model.icon

import mega.privacy.android.feature.devicecenter.R

/**
 * A sealed UI interface that represents different Folder Icons
 */
sealed interface FolderIconType : DeviceCenterUINodeIcon {

    /**
     * Represents a Backup Folder Icon
     */
    object Backup : FolderIconType {
        override val iconRes = R.drawable.ic_device_folder_backup
        override val applySecondaryColorTint = false
    }

    /**
     * Represents a Camera Uploads Folder Icon
     */
    object CameraUploads : FolderIconType {
        override val iconRes = R.drawable.ic_device_folder_camera_uploads
        override val applySecondaryColorTint = false
    }

    /**
     * Represents a plain Folder Icon
     */
    object Folder : FolderIconType {
        override val iconRes = R.drawable.ic_device_folder
        override val applySecondaryColorTint = false
    }

    /**
     * Represents a Sync Folder Icon
     */
    object Sync : FolderIconType {
        override val iconRes = R.drawable.ic_device_folder_sync
        override val applySecondaryColorTint = false
    }
}