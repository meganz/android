package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use case for updating backup state for camera uploads
 */
class UpdateCameraUploadsBackupUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val updateBackupUseCase: UpdateBackupUseCase,
) {

    /**
     * @param backupName        Backup folder name
     * @param backupState       Backup state
     * @param updateTimeStamp   Update time stamp if required
     */
    suspend operator fun invoke(
        backupName: String,
        backupState: BackupState,
        updateTimeStamp: () -> Unit = {},
    ) {
        if (cameraUploadRepository.isCameraUploadsEnabled()) {
            cameraUploadRepository.getCuBackUp()?.let { backup ->
                if (backupState != backup.state && backup.backupId != cameraUploadRepository.getInvalidHandle()) {
                    updateBackupUseCase(
                        backupId = backup.backupId,
                        localFolder = null,
                        backupName = backupName,
                        backupState = backupState
                    )
                }
            }
            updateTimeStamp()
        }
    }
}
