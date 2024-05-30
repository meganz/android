package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use Case that updates the Backup name of the Secondary Folder
 *
 * @property cameraUploadsRepository [CameraUploadsRepository]
 * @property isMediaUploadsEnabledUseCase [IsMediaUploadsEnabledUseCase]
 * @property updateBackupUseCase [UpdateBackupUseCase]
 */
class UpdateSecondaryFolderBackupNameUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
    private val isMediaUploadsEnabledUseCase: IsMediaUploadsEnabledUseCase,
    private val updateBackupUseCase: UpdateBackupUseCase,
) {
    /**
     * Invocation function
     *
     * @param backupName The new Backup name
     */
    suspend operator fun invoke(backupName: String) {
        if (isMediaUploadsEnabledUseCase() && backupName.isNotBlank()) {
            cameraUploadsRepository.getMuBackUp()?.let { backup ->
                updateBackupUseCase(
                    backupId = backup.backupId,
                    backupName = backupName,
                    backupType = BackupInfoType.MEDIA_UPLOADS
                )
            }
        }
    }
}
