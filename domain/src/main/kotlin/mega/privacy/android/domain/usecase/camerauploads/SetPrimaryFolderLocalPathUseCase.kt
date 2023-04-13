package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case that sets the new Primary Folder local path
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 */
class SetPrimaryFolderLocalPathUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Invocation function
     *
     * @param localPath The new Primary Folder local path
     */
    suspend operator fun invoke(localPath: String) =
        cameraUploadRepository.setPrimaryFolderLocalPath(localPath)
}