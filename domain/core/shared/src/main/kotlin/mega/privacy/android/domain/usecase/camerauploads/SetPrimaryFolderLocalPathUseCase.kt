package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use Case that sets the new Primary Folder local path
 *
 * @property cameraUploadsRepository [CameraUploadsRepository]
 */
class SetPrimaryFolderLocalPathUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {

    /**
     * Invocation function
     *
     * @param localPath The new Primary Folder local path
     */
    suspend operator fun invoke(localPath: String) =
        cameraUploadsRepository.setPrimaryFolderLocalPath(localPath)
}