package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case to update the [BackupState] of the Secondary Folder of Camera Uploads
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 * @property updateBackupStateUseCase [UpdateBackupStateUseCase]
 */
class UpdateSecondaryFolderBackupStateUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val updateBackupStateUseCase: UpdateBackupStateUseCase,
) {
    /**
     * Invocation function
     *
     * @param backupState The new [BackupState] of the Secondary Folder
     */
    suspend operator fun invoke(backupState: BackupState) {
        if (cameraUploadRepository.isSecondaryMediaFolderEnabled() == true) {
            cameraUploadRepository.getMuBackUp()?.let { backup ->
                if (backupState != backup.state && backup.backupId != cameraUploadRepository.getInvalidHandle()) {
                    updateBackupStateUseCase(
                        backupId = backup.backupId,
                        backupState = backupState,
                    )
                }
            }
        }
    }
}
