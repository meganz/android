package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use Case that updates the Backup name of the Primary Folder
 *
 * @property cameraUploadsRepository [CameraUploadsRepository]
 * @property isCameraUploadsEnabledUseCase [IsCameraUploadsEnabledUseCase]
 * @property updateBackupUseCase [UpdateBackupUseCase]
 */
class UpdatePrimaryFolderBackupNameUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
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
            cameraUploadsRepository.getCuBackUp()?.let { backup ->
                updateBackupUseCase(
                    backupId = backup.backupId,
                    backupName = backupName,
                    backupType = BackupInfoType.CAMERA_UPLOADS,
                )
            }
        }
    }
}
