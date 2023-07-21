package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case that updates the Backup name of the Primary Folder
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 * @property isCameraUploadsEnabledUseCase [IsCameraUploadsEnabledUseCase]
 * @property updateBackupNameUseCase [UpdateBackupNameUseCase]
 */
class UpdatePrimaryFolderBackupNameUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
    private val updateBackupNameUseCase: UpdateBackupNameUseCase,
) {
    /**
     * Invocation function
     *
     * @param backupName The new Backup name
     */
    suspend operator fun invoke(backupName: String) {
        if (isCameraUploadsEnabledUseCase() && backupName.isNotBlank()) {
            cameraUploadRepository.getCuBackUp()?.let { backup ->
                updateBackupNameUseCase(
                    backupId = backup.backupId,
                    backupName = backupName,
                )
            }
        }
    }
}