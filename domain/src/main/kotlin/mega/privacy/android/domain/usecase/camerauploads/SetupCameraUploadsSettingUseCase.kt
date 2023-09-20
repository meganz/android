package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.backup.SetupCameraUploadsBackupUseCase
import javax.inject.Inject

/**
 * UseCase that  Setup CameraUpload Setting and backup
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 * @property setupCameraUploadsBackupUseCase [SetupCameraUploadsBackupUseCase]
 * @property removeBackupFolderUseCase [RemoveBackupFolderUseCase]
 */
class SetupCameraUploadsSettingUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val setupCameraUploadsBackupUseCase: SetupCameraUploadsBackupUseCase,
    private val removeBackupFolderUseCase: RemoveBackupFolderUseCase,
) {

    /**
     * Invocation function
     *
     * @param isEnabled [Boolean]
     */
    suspend operator fun invoke(isEnabled: Boolean) {
        cameraUploadRepository.setCameraUploadsEnabled(isEnabled)
        if (isEnabled) {
            setupCameraUploadsBackupUseCase(cameraUploadRepository.getCameraUploadsName())
        } else {
            removeBackupFolderUseCase(CameraUploadFolderType.Primary)
        }
    }
}
