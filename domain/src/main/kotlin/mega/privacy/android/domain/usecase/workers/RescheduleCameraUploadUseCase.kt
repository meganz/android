package mega.privacy.android.domain.usecase.workers

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case to reschedule camera upload
 */
class RescheduleCameraUploadUseCase @Inject constructor(private val cameraUploadRepository: CameraUploadRepository) {
    /**
     * invoke
     */
    suspend operator fun invoke() = cameraUploadRepository.rescheduleCameraUpload()
}
