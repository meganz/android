package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Use case to get the file size (as [FileResult]) from an Uri path if it's a file, or [FolderResult] or [UnknownResult] otherwise.
 */
class GetFileSizeFromUriPathUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * @return [GetFileSizeForUriResult]
     */
    suspend operator fun invoke(uriPath: UriPath) = when {
        fileSystemRepository.isFolderPath(uriPath.value) -> FolderResult
        fileSystemRepository.isFilePath(uriPath.value) -> {
            fileSystemRepository.getFileByPath(uriPath.value)?.length()?.let { FileResult(it) }
                ?: UnknownResult
        }

        fileSystemRepository.isFileUri(uriPath.value) -> {
            fileSystemRepository.getFileFromFileUri(uriPath.value).let {
                when {
                    it.isFile -> FileResult(it.length())
                    it.isDirectory -> FolderResult
                    else -> UnknownResult
                }
            }
        }

        fileSystemRepository.isContentUri(uriPath.value) -> {
            fileSystemRepository.getFileSizeFromUri(uriPath.value)?.let { FileResult(it) }
                ?: UnknownResult
        }

        else -> UnknownResult
    }
}

/**
 * Sealed interface representing the result of attempting to retrieve a file size from a UriPath
 */
sealed interface GetFileSizeForUriResult

/**
 * Represents the case where the file size could not be determined, possibly due to an invalid or unsupported URI.
 */
data object UnknownResult : GetFileSizeForUriResult

/**
 * Represents the case where the UriPath points to a folder, rather than a file.
 */
data object FolderResult : GetFileSizeForUriResult

/**
 * Represents the successful retrieval of a file size.
 *
 * @property sizeInBytes The size of the file in bytes.
 */
data class FileResult(val sizeInBytes: Long) : GetFileSizeForUriResult