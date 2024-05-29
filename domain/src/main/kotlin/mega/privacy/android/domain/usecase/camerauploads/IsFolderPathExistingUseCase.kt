package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use Case that checks whether or not a specific Local Folder exists in the Device File Explorer
 *
 * @property fileSystemRepository Repository containing all File related operation
 */
class IsFolderPathExistingUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * Invocation function
     *
     * @param path The path of the Local Folder from the Device File Explorer
     * @return true if the specified Local Folder exists
     */
    suspend operator fun invoke(path: String?) =
        !path.isNullOrBlank() && fileSystemRepository.doesFolderExists(path)
}