package mega.privacy.android.feature.devicecenter.data.mapper

import mega.privacy.android.feature.devicecenter.data.entity.SyncType
import nz.mega.sdk.MegaApiJava
import javax.inject.Inject

/**
 * Mapper that converts the [Int] value of [nz.mega.sdk.MegaBackupInfo.type] into a corresponding
 * [SyncType]
 */
internal class SyncTypeMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param type The [Int] value from [nz.mega.sdk.MegaBackupInfo.type]. The values can be any of
     * the BACKUP_TYPE_x Constants from [nz.mega.sdk.MegaApiJava]
     * @return a corresponding [SyncType]
     */
    operator fun invoke(type: Int) = when (type) {
        MegaApiJava.BACKUP_TYPE_INVALID -> SyncType.INVALID
        MegaApiJava.BACKUP_TYPE_TWO_WAY_SYNC -> SyncType.TWO_WAY_SYNC
        MegaApiJava.BACKUP_TYPE_UP_SYNC -> SyncType.UP_SYNC
        MegaApiJava.BACKUP_TYPE_DOWN_SYNC -> SyncType.DOWN_SYNC
        MegaApiJava.BACKUP_TYPE_CAMERA_UPLOADS -> SyncType.CAMERA_UPLOADS
        MegaApiJava.BACKUP_TYPE_MEDIA_UPLOADS -> SyncType.MEDIA_UPLOADS
        else -> throw IllegalArgumentException("The sync type value $type is invalid")
    }
}