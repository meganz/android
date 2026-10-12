package mega.privacy.android.domain.usecase.backup

import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.backup.BackupInfoType
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
     * @param backupType    [BackupInfoType]
     * @param targetNode    [Long]
     * @param localFolder   [String]
     * @param backupName    [String]
     * @param state         [BackupState]
     * @return              [Backup]

     */
    suspend operator fun invoke(
        backupType: BackupInfoType, targetNode: Long, localFolder: String, backupName: String,
        state: BackupState,
    ) = backupRepository.setBackup(
        backupType = backupType,
        targetNode = targetNode,
        localFolder = localFolder,
        backupName = backupName,
        state = state,
    ).also {
        backupRepository.saveBackup(it)
    }
}
