package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Is camera uploads enabled
 *
 */
class IsCameraUploadsEnabledUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {
    /**
     * Invoke
     *
     * @return true if camera uploads is enabled
     */
    suspend operator fun invoke() = cameraUploadRepository.isCameraUploadsEnabled()
}
