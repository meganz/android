package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use case for updating the information about a registered backup for Backup Centre
 */
class UpdateBackupUseCase @Inject constructor(private val cameraUploadRepository: CameraUploadRepository) {

    /**
     * invoke function
     * @param backupId      Backup id identifying the backup to be updated
     * @param localFolder   Local path of the folder
     * @param backupState   Backup state
     */
    suspend operator fun invoke(
        backupId: Long,
        localFolder: String?,
        backupName: String,
        backupState: BackupState,
    ) = cameraUploadRepository.updateBackup(
        backupId = backupId,
        backupType = cameraUploadRepository.getInvalidBackupType(),
        targetNode = cameraUploadRepository.getInvalidHandle(),
        localFolder = localFolder,
        backupName = backupName,
        state = backupState,
    ).also {
        cameraUploadRepository.getBackupById(it)?.let { backup ->
            cameraUploadRepository.updateLocalBackup(backup)
        }
    }
}
