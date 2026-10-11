package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import java.io.File
import javax.inject.Inject

/**
 * Use case to delete a list of files if and only if the file is in the cache folder, checked individually
 */
class DeleteCacheFilesUseCase @Inject constructor(
    private val cacheRepository: CacheRepository,
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke
     * @param pathsToDelete the list of files with its path as [UriPath]
     */
    suspend operator fun invoke(pathsToDelete: List<UriPath>) {
        pathsToDelete
            .map { File(it.value) }
            .filter { cacheRepository.isFileInCacheDirectory(it) }
            .forEach { fileSystemRepository.deleteFile(it) }
    }
}