package mega.privacy.android.domain.usecase.transfers.chatuploads

import kotlinx.coroutines.flow.first
import mega.privacy.android.domain.entity.ChatImageQuality
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.NetworkRepository
import mega.privacy.android.domain.repository.SettingsRepository
import java.io.File
import javax.inject.Inject

/**
 * Use case to downscale an image that will be attached to a chat before uploading.
 * The image will be downscaled if:
 * - is not gif
 * - the user settings and wifi connection indicates it should be optimized
 * - the original resolution is bigger than target maximum resolution
 */
class DownscaleImageForChatUseCase @Inject constructor(
    private val defaultSettingsRepository: SettingsRepository,
    private val networkRepository: NetworkRepository,
    private val fileSystemRepository: FileSystemRepository,
    private val getCacheFileForChatUploadUseCase: GetCacheFileForChatUploadUseCase,
) {
    /**
     * Invoke
     *
     * @return the downscaled image or null if it's not scaled.
     */
    suspend operator fun invoke(file: File): File? {
        if (file.isGif()) return null
        val imageQuality = defaultSettingsRepository.getChatImageQuality().first()
        if (imageQuality == ChatImageQuality.Original
            || (imageQuality == ChatImageQuality.Automatic && networkRepository.isOnWifi())
        ) return null
        return getCacheFileForChatUploadUseCase(file)
            ?.also { destination ->
                fileSystemRepository.downscaleImage(
                    file = file,
                    destination = destination,
                    maxPixels = DOWNSCALE_IMAGES_PX
                )
            }?.takeIf { it.exists() }
    }

    private fun File.isGif() = listOf("gif", "webp").contains(extension)

    companion object {
        internal const val DOWNSCALE_IMAGES_PX = 2000000L
    }
}