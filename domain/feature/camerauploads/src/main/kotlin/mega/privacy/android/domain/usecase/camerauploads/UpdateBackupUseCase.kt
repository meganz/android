package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.repository.BackupRepository
import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use Case that updates the Name of a specific Backup
 *
 * @property backupRepository [BackupRepository]
 * @property cameraUploadsRepository [BackupRepository]
 */
class UpdateBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
    private val cameraUploadsRepository: CameraUploadsRepository,
) {
    /**
     * Invocation function
     *
     * @param backupId The Backup ID that identifies the Backup to be updated
     * @param backupName The new Backup Name to be updated
     * @param backupType [BackupInfoType]
     * @param targetNode [Long]
     * @param localFolder [String]
     * @param state [BackupState]
     */
    suspend operator fun invoke(
        backupId: Long,
        backupName: String,
        backupType: BackupInfoType,
        targetNode: Long? = null,
        localFolder: String? = null,
        state: BackupState? = null,
    ) {
        backupRepository.updateRemoteBackup(
            backupId = backupId,
            backupName = backupName,
            backupType = backupType,
            targetNode = targetNode ?: -1L,
            localFolder = localFolder,
            state = state ?: BackupState.INVALID
        )
        cameraUploadsRepository.getBackupById(backupId)?.let { backup ->
            cameraUploadsRepository.updateLocalBackup(
                backup.copy(backupName = backupName)
            )
        }
    }
}
