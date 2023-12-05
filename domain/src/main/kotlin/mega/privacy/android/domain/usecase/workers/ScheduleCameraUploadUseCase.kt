package mega.privacy.android.domain.usecase.workers

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case to schedule camera upload
 */
class ScheduleCameraUploadUseCase @Inject constructor(private val cameraUploadRepository: CameraUploadRepository) {
    /**
     * invoke
     */
    suspend operator fun invoke() {
        if (cameraUploadRepository.isCameraUploadsEnabled() == true) {
            cameraUploadRepository.scheduleCameraUploads()
        }
    }
}
