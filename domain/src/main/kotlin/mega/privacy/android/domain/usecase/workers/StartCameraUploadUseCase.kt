package mega.privacy.android.domain.usecase.workers

import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use Case to start camera upload
 */
class StartCameraUploadUseCase @Inject constructor(private val cameraUploadsRepository: CameraUploadsRepository) {
    /**
     * invoke
     */
    suspend operator fun invoke() {
        if (cameraUploadsRepository.isCameraUploadsEnabled() == true) {
            cameraUploadsRepository.startCameraUploads()
        }
    }
}
