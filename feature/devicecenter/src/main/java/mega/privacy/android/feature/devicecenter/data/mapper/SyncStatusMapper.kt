package mega.privacy.android.feature.devicecenter.data.mapper

import mega.privacy.android.feature.devicecenter.data.entity.SyncStatus
import nz.mega.sdk.MegaBackupInfo
import javax.inject.Inject

/**
 * Mapper that converts the [Int] value of [MegaBackupInfo.status] into a corresponding [SyncStatus]
 */
internal class SyncStatusMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param sdkStatus The [Int] value from [MegaBackupInfo.status]
     * @return a corresponding [SyncStatus]
     */
    operator fun invoke(sdkStatus: Int) = when (sdkStatus) {
        MegaBackupInfo.BACKUP_STATUS_NOT_INITIALIZED -> SyncStatus.NOT_INITIALIZED
        MegaBackupInfo.BACKUP_STATUS_UPTODATE -> SyncStatus.UPTODATE
        MegaBackupInfo.BACKUP_STATUS_SYNCING -> SyncStatus.SYNCING
        MegaBackupInfo.BACKUP_STATUS_PENDING -> SyncStatus.PENDING
        MegaBackupInfo.BACKUP_STATUS_INACTIVE -> SyncStatus.INACTIVE
        MegaBackupInfo.BACKUP_STATUS_UNKNOWN -> SyncStatus.UNKNOWN
        else -> throw IllegalArgumentException("The sync status value $sdkStatus is invalid")
    }
}