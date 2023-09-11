package mega.privacy.android.domain.usecase.backup

import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.repository.BackupRepository
import javax.inject.Inject

/**
 * Use Case that saves a backup to local database
 *
 * @property backupRepository [BackupRepository]
 */
class SaveBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {
    /**
     * Invocation function
     * @param backup [Backup]
     */
    suspend operator fun invoke(backup: Backup) = backupRepository.saveBackup(backup)
}
