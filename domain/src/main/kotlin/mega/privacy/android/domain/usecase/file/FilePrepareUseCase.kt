package mega.privacy.android.domain.usecase.file

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.document.toDocumentEntity
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.transfers.GetFileForUploadUseCase
import javax.inject.Inject

/**
 * Use case which prepares files from a list of UriPaths and returns a list of DocumentEntities.
 */
class FilePrepareUseCase @Inject constructor(
    private val getFileForUploadUseCase: GetFileForUploadUseCase,
    private val fileSystemRepository: FileSystemRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * invoke to prepare files from a list of UriPaths and returns a list of cache files DocumentEntities.
     */
    suspend operator fun invoke(
        uris: List<UriPath>,
        isChatUpload: Boolean = false,
    ): List<DocumentEntity> = withContext(ioDispatcher) {
        uris.mapNotNull {
            getFileForUploadUseCase(
                it.value,
                isChatUpload
            )?.toDocumentEntity(customName = fileSystemRepository.getDocumentFileName(it))
        }
    }
}