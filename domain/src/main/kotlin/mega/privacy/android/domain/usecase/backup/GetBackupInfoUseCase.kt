package mega.privacy.android.domain.usecase.backup

import mega.privacy.android.domain.entity.backup.BackupInfo
import mega.privacy.android.domain.repository.BackupRepository
import javax.inject.Inject

/**
 * Use Case that retrieves all of the User's Backup information
 *
 * @property backupRepository [BackupRepository]
 */
class GetBackupInfoUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {

    /**
     * Invocation function
     *
     * @return The User's Backup information
     */
    suspend operator fun invoke(): List<BackupInfo> = backupRepository.getBackupInfo()
}