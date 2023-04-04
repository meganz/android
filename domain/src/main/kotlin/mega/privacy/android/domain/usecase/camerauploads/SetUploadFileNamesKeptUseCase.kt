package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case that sets whether the File Names of files to be uploaded will be kept or not
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 */
class SetUploadFileNamesKeptUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Invocation function
     *
     * @param keepFileNames true if the File Names should now be left as is, and false if otherwise
     */
    suspend operator fun invoke(keepFileNames: Boolean) =
        cameraUploadRepository.setUploadFileNamesKept(keepFileNames)
}