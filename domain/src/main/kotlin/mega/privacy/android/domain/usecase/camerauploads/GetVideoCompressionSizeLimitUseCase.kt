package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case to retrieve the maximum video file size that can be compressed
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 */
class GetVideoCompressionSizeLimitUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Invocation function
     *
     * @return An [Int] that represents the maximum video file size that can be compressed
     */
    suspend operator fun invoke(): Int = cameraUploadRepository.getVideoCompressionSizeLimit()
}