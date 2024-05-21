package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use Case that retrieves the Camera Uploads Secondary Folder handle
 *
 * @property cameraUploadsRepository [CameraUploadsRepository]
 */
class GetSecondarySyncHandleUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {

    /**
     * Invocation function
     *
     * @return The Secondary Folder Sync Handle
     */
    suspend operator fun invoke(): Long = with(cameraUploadsRepository) {
        getSecondarySyncHandle() ?: getInvalidHandle()
    }
}
