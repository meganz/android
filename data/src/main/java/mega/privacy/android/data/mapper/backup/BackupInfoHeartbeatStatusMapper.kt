package mega.privacy.android.data.mapper.backup

import mega.privacy.android.domain.entity.backup.BackupInfoHeartbeatStatus
import nz.mega.sdk.MegaBackupInfo
import javax.inject.Inject

/**
 * Mapper that converts the [Int] value of [MegaBackupInfo.status] into a corresponding [BackupInfoHeartbeatStatus]
 */
internal class BackupInfoHeartbeatStatusMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param sdkHeartbeatStatus The [Int] value from [MegaBackupInfo.status]
     * @return a corresponding [BackupInfoHeartbeatStatus]
     */
    operator fun invoke(sdkHeartbeatStatus: Int) = when (sdkHeartbeatStatus) {
        MegaBackupInfo.BACKUP_STATUS_NOT_INITIALIZED -> BackupInfoHeartbeatStatus.NOT_INITIALIZED
        MegaBackupInfo.BACKUP_STATUS_UPTODATE -> BackupInfoHeartbeatStatus.UPTODATE
        MegaBackupInfo.BACKUP_STATUS_SYNCING -> BackupInfoHeartbeatStatus.SYNCING
        MegaBackupInfo.BACKUP_STATUS_PENDING -> BackupInfoHeartbeatStatus.PENDING
        MegaBackupInfo.BACKUP_STATUS_INACTIVE -> BackupInfoHeartbeatStatus.INACTIVE
        MegaBackupInfo.BACKUP_STATUS_UNKNOWN -> BackupInfoHeartbeatStatus.UNKNOWN
        else -> throw IllegalArgumentException("The sync heartbeat status value $sdkHeartbeatStatus is invalid")
    }
}