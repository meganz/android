package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.NotEnoughStorageException
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.PermissionRepository
import mega.privacy.android.domain.usecase.file.DoesUriPathHaveSufficientSpaceUseCase
import java.io.File
import javax.inject.Inject

/**
 * Get a file for this uri or path that can be accessed by SDK:
 * - If the string is already representing an existing file path it returns the file path
 * - If the Uri is already representing a file it returns the file path
 * - If the Uri is a content uri, it makes a copy in the chat cache folder and returns its path
 */
class GetPathForUploadUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke
     *
     * @param originalUriPath a string representing the UriPath of the file or folder to be uploaded
     * @return the uriPath of the file or folder to be uploaded, as String for testing purposes
     */
    suspend operator fun invoke(originalUriPath: UriPath): String? {
        return when {
            fileSystemRepository.isContentUri(originalUriPath.value) -> {
                originalUriPath.value
            }

            fileSystemRepository.isFilePath(originalUriPath.value) -> {
                originalUriPath.value
            }

            fileSystemRepository.isFileUri(originalUriPath.value) -> {
                fileSystemRepository.getFileFromFileUri(originalUriPath.value).absolutePath
            }

            else -> {
                null
            }
        }
    }
}