package mega.privacy.android.domain.usecase.workers

import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use Case to schedule camera upload
 */
class ScheduleCameraUploadUseCase @Inject constructor(private val cameraUploadsRepository: CameraUploadsRepository) {
    /**
     * invoke
     */
    suspend operator fun invoke() {
        if (cameraUploadsRepository.isCameraUploadsEnabled() == true) {
            cameraUploadsRepository.scheduleCameraUploads()
        }
    }
}
