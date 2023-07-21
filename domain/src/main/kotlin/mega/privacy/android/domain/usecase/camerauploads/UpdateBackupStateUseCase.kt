package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case that updates the Backup State of a specific Backup, both remotely and locally
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 */
class UpdateBackupStateUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {
    /**
     * Invocation function
     *
     * @param backupId  The Backup ID that identifies the Backup to be updated
     * @param backupState The new [backupState] of the Backup to be updated
     */
    suspend operator fun invoke(
        backupId: Long,
        backupState: BackupState,
    ) {
        val backupStateInt = cameraUploadRepository.updateRemoteBackupState(
            backupId = backupId,
            backupState = backupState,
        )
        cameraUploadRepository.getBackupById(backupId)?.let { backup ->
            cameraUploadRepository.updateLocalBackup(
                backup.copy(state = backupStateInt)
            )
        }
    }
}