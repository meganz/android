package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case to retrieve the upload option for Camera Uploads
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 */
class GetUploadOptionUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Invocation function
     *
     * @return [UploadOption]
     */
    suspend operator fun invoke(): UploadOption = cameraUploadRepository.getUploadOption()
}