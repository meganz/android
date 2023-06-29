package mega.privacy.android.domain.usecase.workers

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case to stop camera upload
 */
class StopCameraUploadsUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {
    /**
     * invoke
     * @param shouldReschedule true if the Camera Uploads should be rescheduled at a later time
     */
    suspend operator fun invoke(shouldReschedule: Boolean) {
        if (cameraUploadRepository.isCameraUploadsEnabled()) {
            cameraUploadRepository.setCameraUploadsEnabled(false)
            cameraUploadRepository.stopCameraUploads(shouldReschedule = shouldReschedule)
        }
    }
}
