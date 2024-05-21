package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use Case to retrieve the maximum video file size that can be compressed
 *
 * @property cameraUploadsRepository [CameraUploadsRepository]
 */
class GetVideoCompressionSizeLimitUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {

    /**
     * Invocation function
     *
     * @return An [Int] that represents the maximum video file size that can be compressed
     */
    suspend operator fun invoke(): Int =
        cameraUploadsRepository.getVideoCompressionSizeLimit() ?: DEFAULT_SIZE

    /**
     * Default VideoCompression Size Limit
     */
    companion object {
        /**
         * Size in MB
         */
        const val DEFAULT_SIZE = 200
    }
}
