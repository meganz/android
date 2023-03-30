package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case that sets the maximum video file size that can be compressed
 */
class SetVideoCompressionSizeLimitUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Invocation function
     *
     * @param size The maximum video file size that can be compressed
     */
    suspend operator fun invoke(size: Int) =
        cameraUploadRepository.setVideoCompressionSizeLimit(size)
}