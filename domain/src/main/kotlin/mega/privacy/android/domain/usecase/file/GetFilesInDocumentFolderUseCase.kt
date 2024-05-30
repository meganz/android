package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Get files in document folder
 *
 * @property fileSystemRepository
 */
class GetFilesInDocumentFolderUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke
     *
     * @param folder
     */
    suspend operator fun invoke(folder: UriPath) =
        fileSystemRepository.getFilesInDocumentFolder(folder)
}