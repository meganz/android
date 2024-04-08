package mega.privacy.android.feature.devicecenter.ui.model.icon

import mega.privacy.android.icon.pack.R

/**
 * A sealed UI interface that represents different Folder Icons
 */
sealed interface FolderIconType : DeviceCenterUINodeIcon {

    /**
     * Represents a Backup Folder Icon
     */
    data object Backup : FolderIconType {
        override val iconRes = R.drawable.ic_folder_backup_medium_solid

        @Deprecated("Temporary used in order to fix icon color until we change to the new icon set. Will be removed soon.")
        override val applySecondaryColorTint = false
    }

    /**
     * Represents a Camera Uploads Folder Icon
     */
    data object CameraUploads : FolderIconType {
        override val iconRes = R.drawable.ic_folder_camera_uploads_medium_solid

        @Deprecated("Temporary used in order to fix icon color until we change to the new icon set. Will be removed soon.")
        override val applySecondaryColorTint = false
    }

    /**
     * Represents a plain Folder Icon
     */
    data object Folder : FolderIconType {
        override val iconRes = R.drawable.ic_folder_medium_solid

        @Deprecated("Temporary used in order to fix icon color until we change to the new icon set. Will be removed soon.")
        override val applySecondaryColorTint = false
    }

    /**
     * Represents a Sync Folder Icon
     */
    data object Sync : FolderIconType {
        override val iconRes = R.drawable.ic_folder_sync_medium_solid

        @Deprecated("Temporary used in order to fix icon color until we change to the new icon set. Will be removed soon.")
        override val applySecondaryColorTint = false
    }
}