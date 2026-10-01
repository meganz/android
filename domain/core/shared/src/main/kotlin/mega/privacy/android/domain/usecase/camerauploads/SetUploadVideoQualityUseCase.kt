package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use Case to set the Video Quality of Videos to be uploaded
 *
 * @property cameraUploadsRepository [CameraUploadsRepository]
 */
class SetUploadVideoQualityUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {

    /**
     * Invocation function
     *
     * @param videoQuality The new [VideoQuality]
     */
    suspend operator fun invoke(videoQuality: VideoQuality) =
        cameraUploadsRepository.setUploadVideoQuality(videoQuality)
}