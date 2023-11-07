package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case that retrieves the Video Quality of Videos to be uploaded
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 */
class GetUploadVideoQualityUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Invocation function
     *
     * @return The corresponding [VideoQuality], which can be nullable
     */
    suspend operator fun invoke(): VideoQuality =
        cameraUploadRepository.getUploadVideoQuality() ?: VideoQuality.ORIGINAL
}
