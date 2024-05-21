package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use Case that retrieves the Video Quality of Videos to be uploaded
 *
 * @property cameraUploadsRepository [CameraUploadsRepository]
 */
class GetUploadVideoQualityUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {

    /**
     * Invocation function
     *
     * @return The corresponding [VideoQuality], which can be nullable
     */
    suspend operator fun invoke(): VideoQuality =
        cameraUploadsRepository.getUploadVideoQuality() ?: VideoQuality.ORIGINAL
}
