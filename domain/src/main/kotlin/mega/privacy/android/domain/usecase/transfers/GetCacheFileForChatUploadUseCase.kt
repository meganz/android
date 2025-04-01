package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import mega.privacy.android.domain.usecase.file.GetFileExtensionFromUriPathUseCase
import java.io.File
import javax.inject.Inject

/**
 * Returns a file in cache temporary folder to save chat upload modifications to the original file like media compression or rescale. As the original file can be already in this folder a number suffix is added until the file doesn't exist
 */
class GetCacheFileForChatUploadUseCase @Inject constructor(
    private val getCacheFileUseCase: GetCacheFileUseCase,
    private val cacheRepository: CacheRepository,
    private val getFileExtensionFromUriPathUseCase: GetFileExtensionFromUriPathUseCase,
    private val getFileNameFromStringUriUseCase: GetFileNameFromStringUriUseCase,
) {
    /**
     * Invoke
     *
     * @return a file in cache temporary folder that does not exist yet or null if not possible
     */
    suspend operator fun invoke(uriPath: UriPath): File? {
        val extension = getFileExtensionFromUriPathUseCase(uriPath)
        val name = if (uriPath.isPath()) {
            File(uriPath.value).name
        } else {
            getFileNameFromStringUriUseCase(uriPath.value) ?: "tempFile.$extension"
        }
        val nameWithoutExtension = if (extension.isBlank()) name else name.substringBeforeLast(".")
        return (cacheRepository.getCacheFolderNameForTransfer(true)).let { folderName ->
            getCacheFileUseCase(folderName, name)?.takeIf { !it.exists() }
                ?: run {
                    (1..99).firstNotNullOfOrNull { suffix ->
                        val nameWithSuffix = "${nameWithoutExtension}_$suffix"
                            .plus(if (extension.isNotBlank()) ".$extension" else "")
                        getCacheFileUseCase(
                            folderName,
                            nameWithSuffix
                        )?.takeIf { !it.exists() }
                    }
                }
        }
    }
}