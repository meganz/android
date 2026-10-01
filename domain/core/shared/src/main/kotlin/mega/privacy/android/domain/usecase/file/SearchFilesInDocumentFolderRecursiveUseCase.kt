package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Get files in document folder
 *
 * @property fileSystemRepository
 */
class SearchFilesInDocumentFolderRecursiveUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke
     *
     * @param folder
     */
    operator fun invoke(folder: UriPath, query: String) =
        fileSystemRepository.searchFilesInDocumentFolderRecursive(folder = folder, query = query)
}