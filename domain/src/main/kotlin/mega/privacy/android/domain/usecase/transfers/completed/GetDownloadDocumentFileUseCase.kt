package mega.privacy.android.domain.usecase.transfers.completed

import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use case to get [DocumentEntity] given a local file string URI and file name.
 *
 */
class GetDownloadDocumentFileUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * Invoke
     *
     * @return [DocumentEntity] for the download transfer.
     */
    suspend operator fun invoke(fileStringUri: String, fileName: String) =
        fileSystemRepository.getDocumentFileIfContentUri(fileStringUri, fileName)
}