package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use Case that checks whether the Primary Folder path is valid or not
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 * @property fileSystemRepository [FileSystemRepository]
 */
class IsSecondaryFolderPathValidUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * Invocation function
     *
     * @param path The Secondary Folder path, which can be nullable
     * @return true if the Secondary Folder exists and is different from the Primary Folder
     * local path. Otherwise, return false
     */
    suspend operator fun invoke(path: String?) = path?.let { nonNullPath ->
        nonNullPath.isNotBlank()
                && fileSystemRepository.doesFolderExists(nonNullPath)
                && nonNullPath != cameraUploadRepository.getPrimaryFolderLocalPath()
    } ?: false
}
