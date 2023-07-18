package mega.privacy.android.feature.devicecenter.data.mapper

import mega.privacy.android.feature.devicecenter.data.entity.BackupInfoState
import nz.mega.sdk.MegaBackupInfo
import javax.inject.Inject

/**
 * Mapper that converts the [Int] value of [MegaBackupInfo.state] into a corresponding
 * [BackupInfoState]
 */
internal class BackupInfoStateMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param sdkState The [Int] value from [MegaBackupInfo.state]
     * @return a corresponding [BackupInfoState]
     */
    operator fun invoke(sdkState: Int) = when (sdkState) {
        MegaBackupInfo.BACKUP_STATE_NOT_INITIALIZED -> BackupInfoState.NOT_INITIALIZED
        MegaBackupInfo.BACKUP_STATE_ACTIVE -> BackupInfoState.ACTIVE
        MegaBackupInfo.BACKUP_STATE_FAILED -> BackupInfoState.FAILED
        MegaBackupInfo.BACKUP_STATE_TEMPORARY_DISABLED -> BackupInfoState.TEMPORARY_DISABLED
        MegaBackupInfo.BACKUP_STATE_DISABLED -> BackupInfoState.DISABLED
        MegaBackupInfo.BACKUP_STATE_PAUSE_UP -> BackupInfoState.PAUSE_UP
        MegaBackupInfo.BACKUP_STATE_PAUSE_DOWN -> BackupInfoState.PAUSE_DOWN
        MegaBackupInfo.BACKUP_STATE_PAUSE_FULL -> BackupInfoState.PAUSE_FULL
        MegaBackupInfo.BACKUP_STATE_DELETED -> BackupInfoState.DELETED
        else -> throw IllegalArgumentException("The backup state value $sdkState is invalid")
    }
}