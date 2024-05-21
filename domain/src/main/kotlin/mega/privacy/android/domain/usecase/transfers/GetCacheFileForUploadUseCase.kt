package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import java.io.File
import javax.inject.Inject

/**
 * Returns a file in cache temporary folder to save files or file modifications to the original file. As the original file can be already in this folder a number suffix is added until the file doesn't exist
 */
class GetCacheFileForUploadUseCase @Inject constructor(
    private val getCacheFileUseCase: GetCacheFileUseCase,
) {
    /**
     * Invoke
     *
     * @return a file in cache temporary folder that does not exist yet or null if not possible
     */
    operator fun invoke(file: File, isChatUpload: Boolean): File? =
        (if (isChatUpload) CHAT_TEMPORARY_FOLDER else TEMPORARY_FOLDER).let { folder ->
            getCacheFileUseCase(folder, file.name)?.takeIf { !it.exists() }
                ?: run {
                    (1..99).firstNotNullOfOrNull { suffix ->
                        getCacheFileUseCase(
                            folder,
                            "${file.nameWithoutExtension}_$suffix.${file.extension}"
                        )?.takeIf { !it.exists() }
                    }
                }
        }

    companion object {
        internal const val CHAT_TEMPORARY_FOLDER = "chatTempMEGA"
        internal const val TEMPORARY_FOLDER = "tempMEGA"
    }
}