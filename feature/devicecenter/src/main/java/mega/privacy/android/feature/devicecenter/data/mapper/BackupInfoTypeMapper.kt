package mega.privacy.android.feature.devicecenter.data.mapper

import mega.privacy.android.feature.devicecenter.data.entity.BackupInfoType
import nz.mega.sdk.MegaApiJava
import javax.inject.Inject

/**
 * Mapper that converts the [Int] value of [nz.mega.sdk.MegaBackupInfo.type] into a corresponding
 * [BackupInfoType]
 */
internal class BackupInfoTypeMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param sdkType The [Int] value from [nz.mega.sdk.MegaBackupInfo.type]. The values can be any of
     * the BACKUP_TYPE_x Constants from [nz.mega.sdk.MegaApiJava]
     * @return a corresponding [BackupInfoType]
     */
    operator fun invoke(sdkType: Int) = when (sdkType) {
        MegaApiJava.BACKUP_TYPE_INVALID -> BackupInfoType.INVALID
        MegaApiJava.BACKUP_TYPE_TWO_WAY_SYNC -> BackupInfoType.TWO_WAY_SYNC
        MegaApiJava.BACKUP_TYPE_UP_SYNC -> BackupInfoType.UP_SYNC
        MegaApiJava.BACKUP_TYPE_DOWN_SYNC -> BackupInfoType.DOWN_SYNC
        MegaApiJava.BACKUP_TYPE_CAMERA_UPLOADS -> BackupInfoType.CAMERA_UPLOADS
        MegaApiJava.BACKUP_TYPE_MEDIA_UPLOADS -> BackupInfoType.MEDIA_UPLOADS
        MegaApiJava.BACKUP_TYPE_BACKUP_UPLOAD -> BackupInfoType.BACKUP_UPLOAD
        else -> throw IllegalArgumentException("The backup type value $sdkType is invalid")
    }
}