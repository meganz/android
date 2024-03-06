package mega.privacy.android.domain.usecase.transfers.chatuploads

import mega.privacy.android.domain.repository.FileSystemRepository
import java.io.File
import javax.inject.Inject

/**
 * Get a file for this uri or path that can be accessed by SDK:
 * - If the string is already representing an existing file path it returns the file
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
     * @param uriOrPathString a string representing the Uri
     */
    suspend operator fun invoke(uriOrPathString: String): File? {
        val file = File(uriOrPathString)
        return when {
            file.isFile && file.exists() -> file
            fileSystemRepository.isFileUri(uriOrPathString) -> {
                fileSystemRepository.getFileFromFileUri(uriOrPathString)
            }

            fileSystemRepository.isContentUri(uriOrPathString) -> {
                fileSystemRepository.getFileNameFromUri(uriOrPathString)?.let {
                    getCacheFileForChatUploadUseCase(File(it))?.also { destination ->
                        fileSystemRepository.copyContentUriToFile(uriOrPathString, destination)
                    }
                }
            }

            else -> {
                null
            }
        }
    }
}