package mega.privacy.android.feature.devicecenter.data.mapper

import mega.privacy.android.feature.devicecenter.data.entity.SyncState
import nz.mega.sdk.MegaBackupInfo
import javax.inject.Inject

/**
 * Mapper that converts the [Int] value of [MegaBackupInfo.state] into a corresponding
 * [SyncState]
 */
internal class SyncStateMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param sdkState The [Int] value from [MegaBackupInfo.state]
     * @return a corresponding [SyncState]
     */
    operator fun invoke(sdkState: Int) = when (sdkState) {
        MegaBackupInfo.BACKUP_STATE_NOT_INITIALIZED -> SyncState.NOT_INITIALIZED
        MegaBackupInfo.BACKUP_STATE_ACTIVE -> SyncState.ACTIVE
        MegaBackupInfo.BACKUP_STATE_FAILED -> SyncState.FAILED
        MegaBackupInfo.BACKUP_STATE_TEMPORARY_DISABLED -> SyncState.TEMPORARY_DISABLED
        MegaBackupInfo.BACKUP_STATE_DISABLED -> SyncState.DISABLED
        MegaBackupInfo.BACKUP_STATE_PAUSE_UP -> SyncState.PAUSE_UP
        MegaBackupInfo.BACKUP_STATE_PAUSE_DOWN -> SyncState.PAUSE_DOWN
        MegaBackupInfo.BACKUP_STATE_PAUSE_FULL -> SyncState.PAUSE_FULL
        MegaBackupInfo.BACKUP_STATE_DELETED -> SyncState.DELETED
        else -> throw IllegalArgumentException("The sync state value $sdkState is invalid")
    }
}