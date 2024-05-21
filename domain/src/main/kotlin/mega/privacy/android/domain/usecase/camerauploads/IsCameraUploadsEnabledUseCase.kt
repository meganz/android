package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Is camera uploads enabled
 *
 */
class IsCameraUploadsEnabledUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {
    /**
     * Invoke
     *
     * @return true if camera uploads is enabled
     */
    suspend operator fun invoke() = cameraUploadsRepository.isCameraUploadsEnabled() ?: false
}
