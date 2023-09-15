package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.backup.SetupOrUpdateMediaUploadsBackupUseCase
import javax.inject.Inject

/**
 * UseCase that  Setup Media Upload Sync Handle
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 * @property setupOrUpdateMediaUploadsBackupUseCase [SetupOrUpdateMediaUploadsBackupUseCase]
 */
class SetupMediaUploadSyncHandleUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val setupOrUpdateMediaUploadsBackupUseCase: SetupOrUpdateMediaUploadsBackupUseCase,
) {

    /**
     * Invocation function
     *
     * @param handle [Long]
     */
    suspend operator fun invoke(handle: Long) {
        cameraUploadRepository.setSecondarySyncHandle(handle)
        setupOrUpdateMediaUploadsBackupUseCase(targetNode = handle, localFolder = null)
    }
}
