package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import java.io.File
import javax.inject.Inject

/**
 * Returns a file in cache temporary folder to save files or file modifications to the original file. As the original file can be already in this folder a number suffix is added until the file doesn't exist
 */
class GetCacheFileForUploadUseCase @Inject constructor(
    private val getCacheFileUseCase: GetCacheFileUseCase,
    private val cacheRepository: CacheRepository,
) {
    /**
     * Invoke
     *
     * @return a file in cache temporary folder that does not exist yet or null if not possible
     */
    operator fun invoke(file: File, isChatUpload: Boolean): File? =
        (cacheRepository.getCacheFolderNameForUpload(isChatUpload)).let { folderName ->
            getCacheFileUseCase(folderName, file.name)?.takeIf { !it.exists() }
                ?: run {
                    (1..99).firstNotNullOfOrNull { suffix ->
                        val nameWithSuffix = "${file.nameWithoutExtension}_$suffix"
                            .plus(if (file.extension.isNotBlank()) ".${file.extension}" else "")
                        getCacheFileUseCase(
                            folderName,
                            nameWithSuffix
                        )?.takeIf { !it.exists() }
                    }
                }
        }
}