package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use Case that sets the maximum video file size that can be compressed
 */
class SetVideoCompressionSizeLimitUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {

    /**
     * Invocation function
     *
     * @param size The maximum video file size that can be compressed
     */
    suspend operator fun invoke(size: Int) =
        cameraUploadsRepository.setVideoCompressionSizeLimit(size)
}