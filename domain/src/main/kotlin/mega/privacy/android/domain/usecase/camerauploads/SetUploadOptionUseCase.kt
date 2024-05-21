package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use Case to set the Upload Option of Camera Uploads
 *
 * @property cameraUploadsRepository [CameraUploadsRepository]
 */
class SetUploadOptionUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {

    /**
     * Invocation function
     *
     * @param uploadOption The [UploadOption] to set
     */
    suspend operator fun invoke(uploadOption: UploadOption) =
        cameraUploadsRepository.setUploadOption(uploadOption)
}