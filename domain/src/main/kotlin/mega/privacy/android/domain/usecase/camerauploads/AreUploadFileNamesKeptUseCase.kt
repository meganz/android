package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case that checks whether the File Names are kept or not when uploading content
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 */
class AreUploadFileNamesKeptUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Invocation function
     *
     * @return true if the File Names should be left as is, and false if otherwise
     */
    suspend operator fun invoke(): Boolean =
        cameraUploadRepository.areUploadFileNamesKept() ?: false
}
