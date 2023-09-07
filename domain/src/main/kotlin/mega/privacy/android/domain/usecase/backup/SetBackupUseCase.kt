package mega.privacy.android.domain.usecase.backup

import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.repository.BackupRepository
import javax.inject.Inject

/**
 * Use Case for registering a backup to display in Device Centre
 */
class SetBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    /**
     * Invocation function
     * @param backupType [Int]
     * @param targetNode [Long]
     * @param localFolder [String]
     * @param backupName [String]
     * @param state [Int]
     * @param subState [Int]
     * @return [Backup]
     */
    suspend operator fun invoke(
        backupType: Int, targetNode: Long, localFolder: String, backupName: String,
        state: Int, subState: Int,
    ) = backupRepository.setBackup(
        backupType = backupType,
        targetNode = targetNode,
        localFolder = localFolder,
        backupName = backupName,
        state = state,
        subState = subState,
    )
}
