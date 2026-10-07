package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.ShareTextInfo
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import javax.inject.Inject

/**
 * Create a temporary file in the upload cache folder containing [ShareTextInfo.fileContent]
 * so it can be uploaded as the user's confirmed share-text file.
 *
 * The returned [DocumentEntity] is built from the file just written, so the upload does not go
 * through the shared-uri preparation, which rejects files inside the app's private storage.
 */
class CreateTextFileWithContentUseCase @Inject constructor(
    private val getCacheFileUseCase: GetCacheFileUseCase,
    private val cacheRepository: CacheRepository,
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * @param name File name chosen by the user, including extension.
     * @param fileContent Text file content to write.
     * @return [DocumentEntity] describing the created file, or null if creation failed.
     */
    suspend operator fun invoke(name: String, fileContent: String): DocumentEntity? =
        getCacheFileUseCase(
            cacheRepository.getCacheFolderNameForTransfer(isForChat = false),
            name
        )?.let { tempFile ->
            fileSystemRepository.writeTextToPath(tempFile.absolutePath, fileContent)
            DocumentEntity(
                name = tempFile.name,
                size = tempFile.length(),
                lastModified = tempFile.lastModified(),
                uri = UriPath.fromFile(tempFile),
            )
        }
}
