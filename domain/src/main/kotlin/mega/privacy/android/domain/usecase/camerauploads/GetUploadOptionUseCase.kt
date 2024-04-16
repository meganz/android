package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case to retrieve the type of content to be uploaded by Camera Uploads
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 */
class GetUploadOptionUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Invocation function
     *
     * @return The existing [UploadOption] that was set. [UploadOption.PHOTOS_AND_VIDEOS] is
     * returned as default if there was no existing [UploadOption] set
     */
    suspend operator fun invoke(): UploadOption =
        cameraUploadRepository.getUploadOption() ?: UploadOption.PHOTOS_AND_VIDEOS
}
