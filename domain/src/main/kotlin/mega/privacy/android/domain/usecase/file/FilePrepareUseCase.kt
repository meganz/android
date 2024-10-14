package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use case which prepares files from a list of UriPaths and returns a list of DocumentEntities.
 */
class FilePrepareUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * invoke to prepare files from a list of UriPaths and returns a list of cache files DocumentEntities.
     */
    suspend operator fun invoke(
        uris: List<UriPath>,
    ): List<DocumentEntity> = fileSystemRepository.getDocumentEntities(uris)
}