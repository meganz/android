package mega.privacy.android.data.mapper.backup

import mega.privacy.android.domain.entity.backup.BackupInfoType
import nz.mega.sdk.MegaApiJava
import javax.inject.Inject

/**
 * Mapper that converts between [BackupInfoType] and [nz.mega.sdk.MegaBackupInfo.type] values
 */
class BackupInfoTypeIntMapper @Inject constructor() {

    /**
     * Converts the [BackupInfoType] value into the corresponding [nz.mega.sdk.MegaBackupInfo.type] value.
     *
     * @param backupInfoType The [BackupInfoType] value.
     * @return The corresponding [Int] value from [nz.mega.sdk.MegaBackupInfo.type].
     * The values can be any of the BACKUP_TYPE_x Constants from [nz.mega.sdk.MegaApiJava].
     */
    operator fun invoke(backupInfoType: BackupInfoType) = when (backupInfoType) {
        BackupInfoType.INVALID -> MegaApiJava.BACKUP_TYPE_INVALID
        BackupInfoType.TWO_WAY_SYNC -> MegaApiJava.BACKUP_TYPE_TWO_WAY_SYNC
        BackupInfoType.UP_SYNC -> MegaApiJava.BACKUP_TYPE_UP_SYNC
        BackupInfoType.DOWN_SYNC -> MegaApiJava.BACKUP_TYPE_DOWN_SYNC
        BackupInfoType.CAMERA_UPLOADS -> MegaApiJava.BACKUP_TYPE_CAMERA_UPLOADS
        BackupInfoType.MEDIA_UPLOADS -> MegaApiJava.BACKUP_TYPE_MEDIA_UPLOADS
        BackupInfoType.BACKUP_UPLOAD -> MegaApiJava.BACKUP_TYPE_BACKUP_UPLOAD
    }

    /**
     * Converts the [nz.mega.sdk.MegaBackupInfo.type] value into the corresponding [BackupInfoType] value.
     *
     * @param backupInfoType The [nz.mega.sdk.MegaBackupInfo.type] value.
     * The values can be any of the BACKUP_TYPE_x Constants from [nz.mega.sdk.MegaApiJava].
     * @return The corresponding [BackupInfoType].
     */
    operator fun invoke(backupInfoType: Int): BackupInfoType = when (backupInfoType) {
        MegaApiJava.BACKUP_TYPE_TWO_WAY_SYNC -> BackupInfoType.TWO_WAY_SYNC
        MegaApiJava.BACKUP_TYPE_UP_SYNC -> BackupInfoType.UP_SYNC
        MegaApiJava.BACKUP_TYPE_DOWN_SYNC -> BackupInfoType.DOWN_SYNC
        MegaApiJava.BACKUP_TYPE_CAMERA_UPLOADS -> BackupInfoType.CAMERA_UPLOADS
        MegaApiJava.BACKUP_TYPE_MEDIA_UPLOADS -> BackupInfoType.MEDIA_UPLOADS
        MegaApiJava.BACKUP_TYPE_BACKUP_UPLOAD -> BackupInfoType.BACKUP_UPLOAD
        else -> BackupInfoType.INVALID
    }
}
