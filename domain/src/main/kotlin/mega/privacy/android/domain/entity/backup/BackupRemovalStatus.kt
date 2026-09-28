package mega.privacy.android.domain.entity.backup


/**
 * Data class to hold the status of a backup removal operation.
 *
 * @property backupId The ID of the backup.
 * @property isOutdated checks if backup is outdated RemoveBackupFolderUseCase.
 */
data class BackupRemovalStatus(
    val backupId: Long,
    val isOutdated: Boolean
)
