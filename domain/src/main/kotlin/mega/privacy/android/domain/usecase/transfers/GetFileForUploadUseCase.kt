package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.NotEnoughStorageException
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.file.DoesPathHaveSufficientSpaceUseCase
import java.io.File
import javax.inject.Inject

/**
 * Get a file for this uri or path that can be accessed by SDK:
 * - If the string is already representing an existing file path it returns the file
 * - If the Uri is already representing a file it returns the file
 * - If the Uri is a content uri, it makes a copy in the chat cache folder
 */
class GetFileForUploadUseCase @Inject constructor(
    private val getCacheFileForUploadUseCase: GetCacheFileForUploadUseCase,
    private val doesPathHaveSufficientSpaceUseCase: DoesPathHaveSufficientSpaceUseCase,
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke
     *
     * @param uriOrPathString a string representing the Uri
     */
    suspend operator fun invoke(uriOrPathString: String, isChatUpload: Boolean): File? {
        return when {
            fileSystemRepository.isFilePath(uriOrPathString) -> {
                fileSystemRepository.getFileByPath(uriOrPathString)
            }

            fileSystemRepository.isFileUri(uriOrPathString) -> {
                fileSystemRepository.getFileFromFileUri(uriOrPathString)
            }

            fileSystemRepository.isContentUri(uriOrPathString) -> {
                fileSystemRepository.getFileNameFromUri(uriOrPathString)?.let {
                    getCacheFileForUploadUseCase(
                        file = File(it),
                        isChatUpload = isChatUpload,
                    )?.also { destination ->
                        val size = fileSystemRepository.getFileSizeFromUri(it) ?: 0L
                        if (!doesPathHaveSufficientSpaceUseCase(destination.parent, size)) {
                            throw NotEnoughStorageException()
                        }
                        fileSystemRepository.copyContentUriToFile(
                            UriPath(uriOrPathString),
                            destination
                        )
                    }
                }
            }

            else -> {
                null
            }
        }
    }
}