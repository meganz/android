package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case to set the Video Quality of Videos to be uploaded
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 */
class SetUploadVideoQualityUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Invocation function
     *
     * @param videoQuality The new [VideoQuality]
     */
    suspend operator fun invoke(videoQuality: VideoQuality) =
        cameraUploadRepository.setUploadVideoQuality(videoQuality)
}