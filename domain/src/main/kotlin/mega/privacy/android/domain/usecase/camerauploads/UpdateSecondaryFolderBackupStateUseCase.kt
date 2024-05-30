package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use Case to update the [BackupState] of the Secondary Folder of Camera Uploads
 *
 * @property cameraUploadsRepository [CameraUploadsRepository]
 * @property updateBackupStateUseCase [UpdateBackupStateUseCase]
 */
class UpdateSecondaryFolderBackupStateUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
    private val updateBackupStateUseCase: UpdateBackupStateUseCase,
) {
    /**
     * Invocation function
     *
     * @param backupState The new [BackupState] of the Secondary Folder
     */
    suspend operator fun invoke(backupState: BackupState) {
        if (cameraUploadsRepository.isMediaUploadsEnabled() == true) {
            cameraUploadsRepository.getMuBackUp()?.let { backup ->
                if (backupState != backup.state && backup.backupId != cameraUploadsRepository.getInvalidHandle()) {
                    updateBackupStateUseCase(
                        backupId = backup.backupId,
                        backupState = backupState,
                    )
                }
            }
        }
    }
}
