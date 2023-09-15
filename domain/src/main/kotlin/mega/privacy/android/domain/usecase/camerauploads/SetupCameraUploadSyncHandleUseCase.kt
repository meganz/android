package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.backup.SetupOrUpdateCameraUploadsBackupUseCase
import javax.inject.Inject

/**
 * UseCase that  Setup CameraUpload Sync Handle
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 * @property setupOrUpdateCameraUploadsBackupUseCase [SetupOrUpdateCameraUploadsBackupUseCase]
 */
class SetupCameraUploadSyncHandleUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val setupOrUpdateCameraUploadsBackupUseCase: SetupOrUpdateCameraUploadsBackupUseCase,
) {

    /**
     * Invocation function
     *
     * @param handle [Long]
     */
    suspend operator fun invoke(handle: Long) {
        cameraUploadRepository.setPrimarySyncHandle(handle)
        setupOrUpdateCameraUploadsBackupUseCase(targetNode = handle, localFolder = null)
    }
}
