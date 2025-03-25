package mega.privacy.android.domain.usecase.file


import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import java.io.File
import javax.inject.Inject

/**
 * Use case to check if a [UriPath] points to a file or folder in the cache directory
 */
class IsUriPathInCacheUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
    private val cacheRepository: CacheRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(uriPath: UriPath): Boolean {
        return fileSystemRepository.getAbsolutePathByContentUri(uriPath.value)?.let {
            cacheRepository.isFileInCacheDirectory(File(it))
        } ?: false // we always can get path from cache uri
    }
}