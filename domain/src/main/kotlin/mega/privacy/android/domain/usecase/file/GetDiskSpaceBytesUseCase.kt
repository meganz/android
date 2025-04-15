package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Get disk space at the given uriPath location.
 * Uri path should be resolved to a path to get the disk space:
 * - If the uriPath represents a path or a file Uri, it will use the path to get the disk space
 * - Otherwise it will try to get the path from the Uri
 * - If it's not possible to get the path it will return null
 */
class GetDiskSpaceBytesUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke
     * @param uriPath the URI or file path to process.
     * @return The number of available bytes on the disk at the resolved path, or null if the path could not be resolved.
     */
    suspend operator fun invoke(uriPath: UriPath) = with(fileSystemRepository) {
        when {
            uriPath.isPath() -> uriPath.value
            isFileUri(uriPath.value) -> getFileFromFileUri(uriPath.value).absolutePath
            else -> {
                // Try to get the path from a URI. It's not always possible to get the path of an URI
                getAbsolutePathByContentUri(uriPath.value) // try to get the path using document file api
                    ?: getExternalPathByUri(uriPath.value) // try to build the path from external content:// uri
            }
        }?.let { path ->
            getDiskSpaceBytes(path)
        }
    }
}