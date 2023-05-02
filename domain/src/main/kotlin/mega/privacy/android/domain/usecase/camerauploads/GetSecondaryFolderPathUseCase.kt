package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use Case that retrieves the Secondary Folder path
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 * @property fileSystemRepository [FileSystemRepository]
 */
class GetSecondaryFolderPathUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invocation function
     *
     * @return The current Secondary Folder path
     */
    suspend operator fun invoke(): String {
        val isInSDCard = cameraUploadRepository.isSecondaryFolderInSDCard()
        return if (isInSDCard) {
            cameraUploadRepository.getSecondaryFolderSDCardUriPath()
        } else {
            getLocalPath()
        }
    }

    /**
     * Retrieves the Secondary Folder local path
     *
     * @return the Secondary Folder local path
     */
    private suspend fun getLocalPath(): String {
        val localPath = cameraUploadRepository.getSecondaryFolderLocalPath()

        return if (localPath.isBlank() ||
            (localPath.isNotBlank() && fileSystemRepository.doesFolderExists(localPath))
        ) {
            localPath
        } else {
            cameraUploadRepository.setSecondaryFolderLocalPath("")
            ""
        }
    }
}