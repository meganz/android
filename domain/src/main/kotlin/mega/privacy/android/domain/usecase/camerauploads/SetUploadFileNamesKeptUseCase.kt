package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use Case that sets whether the File Names of files to be uploaded will be kept or not
 *
 * @property cameraUploadsRepository [CameraUploadsRepository]
 */
class SetUploadFileNamesKeptUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {

    /**
     * Invocation function
     *
     * @param keepFileNames true if the File Names should now be left as is, and false if otherwise
     */
    suspend operator fun invoke(keepFileNames: Boolean) =
        cameraUploadsRepository.setUploadFileNamesKept(keepFileNames)
}