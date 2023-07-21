package mega.privacy.android.data.mapper.camerauploads

import mega.privacy.android.domain.entity.BackupState
import nz.mega.sdk.MegaBackupInfo
import javax.inject.Inject

/**
 * Mapper that converts an [Int] into a [BackupState]
 */
internal class BackupStateMapper @Inject constructor() {
    /**
     * Invocation function
     *
     * @param backupStateInt The Backup State returned from the SDK
     * @return a corresponding [BackupState]
     */
    operator fun invoke(backupStateInt: Int) = when (backupStateInt) {
        MegaBackupInfo.BACKUP_STATE_NOT_INITIALIZED -> BackupState.NOT_INITIALIZED
        MegaBackupInfo.BACKUP_STATE_ACTIVE -> BackupState.ACTIVE
        MegaBackupInfo.BACKUP_STATE_FAILED -> BackupState.FAILED
        MegaBackupInfo.BACKUP_STATE_TEMPORARY_DISABLED -> BackupState.TEMPORARILY_DISABLED
        MegaBackupInfo.BACKUP_STATE_DISABLED -> BackupState.DISABLED
        MegaBackupInfo.BACKUP_STATE_PAUSE_UP -> BackupState.PAUSE_UPLOADS
        MegaBackupInfo.BACKUP_STATE_PAUSE_DOWN -> BackupState.PAUSE_DOWNLOADS
        MegaBackupInfo.BACKUP_STATE_PAUSE_FULL -> BackupState.PAUSE_ALL
        MegaBackupInfo.BACKUP_STATE_DELETED -> BackupState.DELETED
        else -> BackupState.INVALID
    }
}