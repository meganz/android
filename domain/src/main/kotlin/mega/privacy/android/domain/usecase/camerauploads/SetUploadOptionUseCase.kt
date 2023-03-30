package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case to set the Upload Option of Camera Uploads
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 */
class SetUploadOptionUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Invocation function
     *
     * @param uploadOption The [UploadOption] to set
     */
    suspend operator fun invoke(uploadOption: UploadOption) =
        cameraUploadRepository.setUploadOption(uploadOption)
}