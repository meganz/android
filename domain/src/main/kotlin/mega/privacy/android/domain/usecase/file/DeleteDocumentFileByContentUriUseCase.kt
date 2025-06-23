package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject


/**
 * Delete a document file  By content URI use case
 *
 */
class DeleteDocumentFileByContentUriUseCase @Inject constructor(
    private val repository: FileSystemRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(uriPath: UriPath) =
        repository.deleteDocumentFileByContentUri(uriPath)
}
