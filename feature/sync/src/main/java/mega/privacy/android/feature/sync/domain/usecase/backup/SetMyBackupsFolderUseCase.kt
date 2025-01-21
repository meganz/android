package mega.privacy.android.feature.sync.domain.usecase.backup

import mega.privacy.android.domain.repository.BackupRepository
import javax.inject.Inject

/**
 * Use case for creating the special folder for Backups
 */
class SetMyBackupsFolderUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(localizedName: String) =
        backupRepository.setMyBackupsFolder(localizedName = localizedName)
}