package mega.privacy.android.data.mapper.backup

import mega.privacy.android.domain.entity.backup.BackupInfoType
import nz.mega.sdk.MegaApiJava
import javax.inject.Inject

/**
 * Mapper that converts the [BackupInfoType] value into a corresponding
 * [nz.mega.sdk.MegaBackupInfo.type] int value
 */
internal class BackupInfoTypeIntMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param backupInfoType [BackupInfoType]
     * @return sdkType The [Int] value from [nz.mega.sdk.MegaBackupInfo.type]. The values can be any of
     * the BACKUP_TYPE_x Constants from [nz.mega.sdk.MegaApiJava]
     */
    operator fun invoke(backupInfoType: BackupInfoType) = when (backupInfoType) {
        BackupInfoType.INVALID -> MegaApiJava.BACKUP_TYPE_INVALID
        BackupInfoType.TWO_WAY_SYNC -> MegaApiJava.BACKUP_TYPE_TWO_WAY_SYNC
        BackupInfoType.UP_SYNC ->MegaApiJava.BACKUP_TYPE_UP_SYNC
        BackupInfoType.DOWN_SYNC -> MegaApiJava.BACKUP_TYPE_DOWN_SYNC
        BackupInfoType.CAMERA_UPLOADS -> MegaApiJava.BACKUP_TYPE_CAMERA_UPLOADS
        BackupInfoType.MEDIA_UPLOADS -> MegaApiJava.BACKUP_TYPE_MEDIA_UPLOADS
        BackupInfoType.BACKUP_UPLOAD -> MegaApiJava.BACKUP_TYPE_BACKUP_UPLOAD
    }
}
