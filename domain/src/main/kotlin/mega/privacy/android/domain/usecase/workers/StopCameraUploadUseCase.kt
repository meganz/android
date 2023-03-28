package mega.privacy.android.domain.usecase.workers

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case to stop camera upload
 */
class StopCameraUploadUseCase @Inject constructor(private val cameraUploadRepository: CameraUploadRepository) {
    /**
     * invoke
     * @param aborted
     */
    suspend operator fun invoke(aborted: Boolean = true) {
        if (cameraUploadRepository.isSyncEnabled()) {
            cameraUploadRepository.fireStopCameraUploadJob(aborted = aborted)
        }
    }
}
