package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case that retrieves the Camera Uploads Secondary Folder handle
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 */
class GetSecondarySyncHandleUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Invocation function
     *
     * @return The Secondary Folder Sync Handle
     */
    suspend operator fun invoke(): Long = with(cameraUploadRepository) {
        getSecondarySyncHandle() ?: getInvalidHandle()
    }
}
