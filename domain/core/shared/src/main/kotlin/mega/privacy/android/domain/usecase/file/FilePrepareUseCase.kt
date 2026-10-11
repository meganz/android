package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import javax.inject.Inject

/**
 * Use case which prepares files from a list of UriPaths and returns a list of DocumentEntities.
 *
 * Files that are not backed by a regular file (for instance content generated on the fly and
 * served through a pipe) can't be uploaded directly, as the SDK needs random access to the
 * source. Those are copied to the upload cache folder, which is removed once uploaded.
 */
class FilePrepareUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
    private val cacheRepository: CacheRepository,
    private val getCacheFileUseCase: GetCacheFileUseCase,
) {
    /**
     * invoke to prepare files from a list of UriPaths and returns a list of cache files DocumentEntities.
     */
    suspend operator fun invoke(
        uris: List<UriPath>,
    ): List<DocumentEntity> = fileSystemRepository.getDocumentEntities(uris).map { document ->
        if (document.isFolder || fileSystemRepository.isRegularFile(document.uri)) {
            document
        } else {
            copyToCache(document) ?: document
        }
    }

    private suspend fun copyToCache(document: DocumentEntity): DocumentEntity? = runCatching {
        getCacheFileUseCase(
            cacheRepository.getCacheFolderNameForTransfer(isForChat = false),
            document.name,
        )?.let { cacheFile ->
            fileSystemRepository.copyContentUriToFile(document.uri, cacheFile)
            document.copy(
                uri = UriPath.fromFile(cacheFile),
                size = cacheFile.length(),
            )
        }
    }.getOrNull()
}
