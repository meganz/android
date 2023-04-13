package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case that sets whether the Primary Folder is located in an external SD Card or not
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 */
class SetPrimaryFolderInSDCardUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Invocation function
     *
     * @param isInSDCard Whether the Primary Folder is located in an external SD Card or not
     */
    suspend operator fun invoke(isInSDCard: Boolean) =
        cameraUploadRepository.setPrimaryFolderInSDCard(isInSDCard)
}