package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Get a [DocumentEntity] from an [UriPath], or null if it doesn't exist
 */
class GetDocumentEntityUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(uriPath: UriPath) = fileSystemRepository.getDocumentEntity(uriPath)
}