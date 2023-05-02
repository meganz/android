package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use Case that checks whether the Primary Folder is set or not
 *
 * @property getPrimaryFolderPathUseCase [GetPrimaryFolderPathUseCase]
 * @property fileSystemRepository [FileSystemRepository]
 */
class IsPrimaryFolderSetUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
    private val getPrimaryFolderPathUseCase: GetPrimaryFolderPathUseCase,
) {
    /**
     * Invocation function
     *
     * When the Primary Folder path is retrieved, check the Folder's existence through
     * [fileSystemRepository]. It is possible that the Primary Folder path is set, but the User
     * manually deleted the Folder
     *
     * @return true if the Primary Folder is set, and false if otherwise
     */
    suspend operator fun invoke(): Boolean {
        val primaryFolderPath = getPrimaryFolderPathUseCase()
        return primaryFolderPath.isNotBlank() && fileSystemRepository.doesFolderExists(
            primaryFolderPath
        )
    }
}
