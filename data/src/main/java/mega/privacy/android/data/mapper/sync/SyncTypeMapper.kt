package mega.privacy.android.data.mapper.sync

import mega.privacy.android.domain.entity.sync.SyncType
import javax.inject.Inject

/**
 * Mapper that converts the [Int] value of [nz.mega.sdk.MegaBackupInfo.type] into a corresponding
 * [SyncType]
 */
internal class SyncTypeMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param type The [Int] value from [nz.mega.sdk.MegaBackupInfo.type]
     * @return a corresponding [SyncType]
     */
    operator fun invoke(type: Int) = when (type) {
        -1 -> SyncType.INVALID
        0 -> SyncType.TWO_WAY
        1 -> SyncType.UP_SYNC
        2 -> SyncType.DOWN_SYNC
        3 -> SyncType.CAMERA_UPLOAD
        4 -> SyncType.MEDIA_UPLOAD
        5 -> SyncType.BACKUP_UPLOAD
        else -> throw IllegalArgumentException("The sync type value $type is invalid")
    }
}