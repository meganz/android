package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case that sets the new Secondary Folder local path
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 */
class SetSecondaryFolderLocalPathUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Invocation function
     *
     * @param localPath The new Secondary Folder local path
     */
    suspend operator fun invoke(localPath: String) =
        cameraUploadRepository.setSecondaryFolderLocalPath(localPath)
}