package mega.privacy.android.domain.usecase.transfers.chatuploads

import mega.privacy.android.domain.repository.FileSystemRepository
import java.io.File
import javax.inject.Inject

/**
 * Get a file for this uri that can be accessed by SDK:
 * - If the Uri is already representing a file it returns the file
 * - If the Uri is a content uri, it makes a copy in the chat cache folder
 */
class GetFileForChatUploadUseCase @Inject constructor(
    private val getCacheFileForChatUploadUseCase: GetCacheFileForChatUploadUseCase,
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke
     *
     * @param uriString a string representing the Uri
     */
    suspend operator fun invoke(uriString: String): File? {
        return when {
            fileSystemRepository.isFileUri(uriString) -> {
                fileSystemRepository.getFileFromFileUri(uriString)
            }

            fileSystemRepository.isContentUri(uriString) -> {
                fileSystemRepository.getFileNameFromUri(uriString)?.let {
                    getCacheFileForChatUploadUseCase(File(it))?.also { destination ->
                        fileSystemRepository.copyContentUriToFile(uriString, destination)
                    }
                }
            }

            else -> {
                null
            }
        }
    }
}