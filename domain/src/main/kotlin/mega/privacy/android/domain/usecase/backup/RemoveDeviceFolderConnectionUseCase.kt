package mega.privacy.android.domain.usecase.backup

import mega.privacy.android.domain.entity.backup.BackupRemovalStatus

/**
 * Use case to remove a device folder connection (sync/backup)
 */
interface RemoveDeviceFolderConnectionUseCase {
    /**
     * Removes the folder connection for the given backup ID
     *
     * @param backupId The backup ID to remove
     * @return The removal status
     */
    suspend operator fun invoke(backupId: Long): BackupRemovalStatus
}
