package mega.privacy.android.feature.devicecenter.ui.mapper

import mega.privacy.android.feature.devicecenter.domain.entity.BackupInfoType
import mega.privacy.android.feature.devicecenter.ui.model.icon.FolderIconType
import javax.inject.Inject

/**
 * UI Mapper class that retrieves the appropriate Device Folder Icon from a Device Folder's
 * [BackupInfoType]
 */
internal class DeviceFolderUINodeIconMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param backupInfoType The [BackupInfoType] from a Device Folder Node
     * @return The correct Device Folder Icon, represented as a [FolderIconType]
     */
    operator fun invoke(backupInfoType: BackupInfoType) = when (backupInfoType) {
        BackupInfoType.CAMERA_UPLOADS,
        BackupInfoType.MEDIA_UPLOADS,
        -> FolderIconType.CameraUploads

        BackupInfoType.INVALID,
        BackupInfoType.UP_SYNC,
        BackupInfoType.DOWN_SYNC,
        BackupInfoType.TWO_WAY_SYNC,
        -> FolderIconType.Sync

        BackupInfoType.BACKUP_UPLOAD -> FolderIconType.Backup
    }
}