package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use Case that checks whether the File Names are kept or not when uploading content
 *
 * @property cameraUploadsRepository [CameraUploadsRepository]
 */
class AreUploadFileNamesKeptUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {

    /**
     * Invocation function
     *
     * @return true if the File Names should be left as is, and false if otherwise
     */
    suspend operator fun invoke(): Boolean =
        cameraUploadsRepository.areUploadFileNamesKept() ?: false
}
