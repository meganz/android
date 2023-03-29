package mega.privacy.android.domain.usecase.workers

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case to start camera upload
 */
class StartCameraUploadUseCase @Inject constructor(private val cameraUploadRepository: CameraUploadRepository) {
    /**
     * invoke
     */
    suspend operator fun invoke() {
        if (cameraUploadRepository.isSyncEnabled()) {
            cameraUploadRepository.fireCameraUploadJob()
        }
    }
}
