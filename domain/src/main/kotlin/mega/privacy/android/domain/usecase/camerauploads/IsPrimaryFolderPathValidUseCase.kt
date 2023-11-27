package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import javax.inject.Inject

/**
 * Use Case that checks whether the Primary Folder path is valid or not
 *
 * @property getSecondaryFolderPathUseCase [GetSecondaryFolderPathUseCase]
 * @property fileSystemRepository [FileSystemRepository]
 * @property isSecondaryFolderEnabled [IsSecondaryFolderEnabled]
 */
class IsPrimaryFolderPathValidUseCase @Inject constructor(
    private val getSecondaryFolderPathUseCase: GetSecondaryFolderPathUseCase,
    private val fileSystemRepository: FileSystemRepository,
    private val isSecondaryFolderEnabled: IsSecondaryFolderEnabled,
) {

    /**
     * Invocation function
     *
     * @param path The Primary Folder path, which can be nullable
     * @return true if the Primary Folder exists and is different from the Secondary Folder
     * local path. Otherwise, return false
     */
    suspend operator fun invoke(path: String?) = path?.let { nonNullPath ->
        nonNullPath.isNotBlank()
                && fileSystemRepository.doesFolderExists(nonNullPath)
                && ((nonNullPath != getSecondaryFolderPathUseCase()).takeIf { isSecondaryFolderEnabled() }
            ?: true)
    } ?: false
}
