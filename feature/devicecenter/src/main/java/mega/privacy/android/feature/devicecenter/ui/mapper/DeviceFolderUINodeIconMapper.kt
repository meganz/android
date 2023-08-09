package mega.privacy.android.feature.devicecenter.ui.mapper

import androidx.annotation.DrawableRes
import mega.privacy.android.feature.devicecenter.R
import mega.privacy.android.feature.devicecenter.data.entity.BackupInfoType
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
     * @return The correct Device Folder Icon, represented as an [Int]
     */
    @DrawableRes
    operator fun invoke(backupInfoType: BackupInfoType) = when (backupInfoType) {
        BackupInfoType.CAMERA_UPLOADS,
        BackupInfoType.MEDIA_UPLOADS,
        -> R.drawable.ic_device_folder_camera_uploads

        BackupInfoType.INVALID,
        BackupInfoType.UP_SYNC,
        BackupInfoType.DOWN_SYNC,
        BackupInfoType.TWO_WAY_SYNC,
        -> R.drawable.ic_device_folder_sync

        BackupInfoType.BACKUP_UPLOAD -> R.drawable.ic_device_folder_backup
    }
}