package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use Case that retrieves the Primary Folder path
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 * @property fileSystemRepository [FileSystemRepository]
 * @property setPrimaryFolderLocalPathUseCase [SetPrimaryFolderLocalPathUseCase]
 */
class GetPrimaryFolderPathUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val fileSystemRepository: FileSystemRepository,
    private val setPrimaryFolderLocalPathUseCase: SetPrimaryFolderLocalPathUseCase,
) {
    /**
     * Invocation function
     *
     * @return The current Primary Folder path
     */
    suspend operator fun invoke(): String {
        val isInSDCard = cameraUploadRepository.isPrimaryFolderInSDCard()
        return if (isInSDCard) {
            cameraUploadRepository.getPrimaryFolderSDCardUriPath()
        } else {
            getLocalPath()
        }
    }

    /**
     * Retrieves the Primary Folder local path
     *
     * @return the Primary Folder local path
     */
    private suspend fun getLocalPath(): String {
        val localPath = cameraUploadRepository.getPrimaryFolderLocalPath()
        return if (localPath.isNotBlank() && fileSystemRepository.doesFolderExists(localPath)) {
            localPath
        } else {
            if (fileSystemRepository.doesExternalStorageDirectoryExists()) {
                val localDCIMFolderPath = fileSystemRepository.localDCIMFolderPath
                setPrimaryFolderLocalPathUseCase(localDCIMFolderPath)

                localDCIMFolderPath
            } else {
                localPath
            }
        }
    }
}