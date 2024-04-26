package mega.privacy.android.domain.usecase.transfers.chatuploads

import mega.privacy.android.domain.repository.FileSystemRepository
import java.io.File
import javax.inject.Inject

/**
 * Use case to downscale an image that will be attached to a chat before uploading.
 */
class DownscaleImageForChatUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
    private val getCacheFileForChatUploadUseCase: GetCacheFileForChatUploadUseCase,
) {
    /**
     * Invoke
     *
     * @return the downscaled image or null if it's not scaled.
     */
    suspend operator fun invoke(file: File): File? {
        return getCacheFileForChatUploadUseCase(file)
            ?.also { destination ->
                fileSystemRepository.downscaleImage(
                    file = file,
                    destination = destination,
                    maxPixels = DOWNSCALE_IMAGES_PX
                )
            }?.takeIf { it.exists() }
    }

    companion object {
        internal const val DOWNSCALE_IMAGES_PX = 2000000L
    }
}