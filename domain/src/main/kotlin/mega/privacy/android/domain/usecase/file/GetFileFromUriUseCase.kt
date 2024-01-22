package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Get a file for this uri:
 * - If the Uri is already representing a file it returns the file
 * - If the Uri is a content uri, it makes a copy in the specified cache folder
 */
class GetFileFromUriUseCase @Inject constructor(
    private val getCacheFileUseCase: GetCacheFileUseCase,
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke
     *
     * @param uriString a string representing the Uri
     * @param cacheFolderName cache folder where the file will be copied if needed (Uri is a content Uri)
     */
    suspend operator fun invoke(uriString: String, cacheFolderName: String): File? {
        return when {
            fileSystemRepository.isFileUri(uriString) -> {
                fileSystemRepository.getFileFromFileUri(uriString)
            }

            fileSystemRepository.isContentUri(uriString) -> {
                val timeStamp =
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = fileSystemRepository.getFileNameFromUri(uriString)
                val fileExtension = fileSystemRepository.getFileExtensionFromUri(uriString)
                val imageFileName =
                    "$fileName$timeStamp".plus(".$fileExtension".takeIf { fileExtension != null })
                getCacheFileUseCase(cacheFolderName, imageFileName)?.also {
                    fileSystemRepository.copyContentUriToFile(uriString, it)
                }
            }

            else -> {
                null
            }
        }
    }
}