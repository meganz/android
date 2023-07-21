package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case that updates the Name of a specific Backup
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 */
class UpdateBackupNameUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {
    /**
     * Invocation function
     *
     * @param backupId The Backup ID that identifies the Backup to be updated
     * @param backupName The new Backup Name to be updated
     */
    suspend operator fun invoke(
        backupId: Long,
        backupName: String,
    ) {
        cameraUploadRepository.updateRemoteBackupName(
            backupId = backupId,
            backupName = backupName,
        )
        cameraUploadRepository.getBackupById(backupId)?.let { backup ->
            cameraUploadRepository.updateLocalBackup(
                backup.copy(backupName = backupName)
            )
        }
    }
}