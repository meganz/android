package mega.privacy.android.feature.sync.domain.usecase.backup

import mega.privacy.android.domain.repository.BackupRepository
import javax.inject.Inject

/**
 * Use case for checking if the special folder for Backups exists
 */
class MyBackupsFolderExistsUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {

    /**
     * Invoke
     *
     * @return True if Backups folder exists, False otherwise
     */
    suspend operator fun invoke() = backupRepository.myBackupsFolderExists()
}