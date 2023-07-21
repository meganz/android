package mega.privacy.android.data.mapper.camerauploads

import mega.privacy.android.domain.entity.BackupState
import nz.mega.sdk.MegaBackupInfo
import javax.inject.Inject

/**
 * Mapper that converts a [BackupState] into an [Integer]
 */
internal class BackupStateIntMapper @Inject constructor() {
    operator fun invoke(backupState: BackupState) = when (backupState) {
        BackupState.INVALID -> -1
        BackupState.NOT_INITIALIZED -> MegaBackupInfo.BACKUP_STATE_NOT_INITIALIZED
        BackupState.ACTIVE -> MegaBackupInfo.BACKUP_STATE_ACTIVE
        BackupState.FAILED -> MegaBackupInfo.BACKUP_STATE_FAILED
        BackupState.TEMPORARILY_DISABLED -> MegaBackupInfo.BACKUP_STATE_TEMPORARY_DISABLED
        BackupState.DISABLED -> MegaBackupInfo.BACKUP_STATE_DISABLED
        BackupState.PAUSE_UPLOADS -> MegaBackupInfo.BACKUP_STATE_PAUSE_UP
        BackupState.PAUSE_DOWNLOADS -> MegaBackupInfo.BACKUP_STATE_PAUSE_DOWN
        BackupState.PAUSE_ALL -> MegaBackupInfo.BACKUP_STATE_PAUSE_FULL
        BackupState.DELETED -> MegaBackupInfo.BACKUP_STATE_DELETED
    }
}
