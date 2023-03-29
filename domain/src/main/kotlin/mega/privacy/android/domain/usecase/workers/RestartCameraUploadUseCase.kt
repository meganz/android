package mega.privacy.android.domain.usecase.workers

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case to restart camera upload
 */
class RestartCameraUploadUseCase @Inject constructor(private val cameraUploadRepository: CameraUploadRepository) {
    /**
     * invoke
     */
    suspend operator fun invoke() = cameraUploadRepository.fireRestartCameraUploadJob()
}
