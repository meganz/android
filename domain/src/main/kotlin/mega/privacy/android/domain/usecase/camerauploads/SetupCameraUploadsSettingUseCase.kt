package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * UseCase that  Setup CameraUpload Setting and backup
 *
 * @property cameraUploadsRepository [CameraUploadsRepository]
 * @property removeBackupFolderUseCase [RemoveBackupFolderUseCase]
 */
class SetupCameraUploadsSettingUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
    private val removeBackupFolderUseCase: RemoveBackupFolderUseCase,
) {

    /**
     * Invocation function
     *
     * @param isEnabled [Boolean]
     */
    suspend operator fun invoke(isEnabled: Boolean) {
        cameraUploadsRepository.setCameraUploadsEnabled(isEnabled)
        if (!isEnabled) {
            removeBackupFolderUseCase(CameraUploadFolderType.Primary)
        }
    }
}
