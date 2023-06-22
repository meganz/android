package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case that updates the Backup name of the Primary Folder
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 * @property isCameraUploadsEnabledUseCase [IsCameraUploadsEnabledUseCase]
 * @property updateBackupUseCase [UpdateBackupUseCase]
 */
class UpdatePrimaryFolderBackupNameUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
    private val updateBackupUseCase: UpdateBackupUseCase,
) {
    /**
     * Invocation function
     *
     * @param backupName The new Backup name
     */
    suspend operator fun invoke(backupName: String) {
        if (isCameraUploadsEnabledUseCase() && backupName.isNotBlank()) {
            cameraUploadRepository.getCuBackUp()?.let { backup ->
                updateBackupUseCase(
                    backupId = backup.backupId,
                    localFolder = null,
                    backupName = backupName,
                    backupState = BackupState.INVALID,
                )
            }
        }
    }
}