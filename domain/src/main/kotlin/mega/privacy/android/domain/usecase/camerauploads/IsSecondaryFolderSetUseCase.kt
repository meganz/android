package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use Case that checks whether the Secondary Folder has been established or not
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 * @property fileSystemRepository [FileSystemRepository]
 * @property getSecondaryFolderPathUseCase [GetSecondaryFolderPathUseCase]
 */
class IsSecondaryFolderSetUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val fileSystemRepository: FileSystemRepository,
    private val getSecondaryFolderPathUseCase: GetSecondaryFolderPathUseCase,
) {
    /**
     * Invocation function
     *
     * @return true if the Secondary Folder has been established, and false if otherwise
     */
    suspend operator fun invoke() = if (cameraUploadRepository.isSecondaryFolderInSDCard()) {
        // Check the corresponding DocumentFile's existence
        val uriPath = cameraUploadRepository.getSecondaryFolderSDCardUriPath()
        fileSystemRepository.isFolderInSDCardAvailable(uriPath)
    } else {
        fileSystemRepository.doesFolderExists(getSecondaryFolderPathUseCase())
    }
}
