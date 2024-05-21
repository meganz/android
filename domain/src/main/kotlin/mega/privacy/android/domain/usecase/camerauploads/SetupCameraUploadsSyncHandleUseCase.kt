package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadsRepository
import mega.privacy.android.domain.usecase.backup.SetupOrUpdateCameraUploadsBackupUseCase
import javax.inject.Inject

/**
 * UseCase that  Setup CameraUploads Sync Handle
 *
 * @property cameraUploadsRepository [CameraUploadsRepository]
 * @property setupOrUpdateCameraUploadsBackupUseCase [SetupOrUpdateCameraUploadsBackupUseCase]
 */
class SetupCameraUploadsSyncHandleUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
    private val setupOrUpdateCameraUploadsBackupUseCase: SetupOrUpdateCameraUploadsBackupUseCase,
) {

    /**
     * Invocation function
     *
     * @param handle [Long]
     */
    suspend operator fun invoke(handle: Long) {
        cameraUploadsRepository.setPrimarySyncHandle(handle)
        setupOrUpdateCameraUploadsBackupUseCase(targetNode = handle, localFolder = null)
    }
}
