package mega.privacy.android.domain.usecase.transfers.completed

import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use case to get [DocumentEntity] given a local file string URI.
 *
 */
class GetDownloadParentDocumentFileUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * Invoke
     *
     * @return [DocumentEntity] for the completed download transfer.
     */
    suspend operator fun invoke(fileStringUri: String) =
        fileSystemRepository.getDocumentFileIfContentUri(fileStringUri)
}