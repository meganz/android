package mega.privacy.android.domain.usecase.transfers.chatuploads

import kotlinx.coroutines.flow.lastOrNull
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.video.CompressVideoUseCase
import java.io.File
import javax.inject.Inject

/**
 * Use case to compress a video that will be attached to a chat before uploading it.
 * The video will be scaled if:
 * - it's mp4 extension
 * - the user settings indicates it should be compressed
 */
class CompressVideoForChatUseCase @Inject constructor(
    private val defaultSettingsRepository: SettingsRepository,
    private val getCacheFileForChatUploadUseCase: GetCacheFileForChatUploadUseCase,
    private val compressVideoUseCase: CompressVideoUseCase,
) {

    /**
     * Invoke
     *
     * @return the downscaled image or null if it's not scaled. 12377928
     */
    suspend operator fun invoke(file: File): File? {
        if (file.extension != "mp4") return null
        val videoQuality = defaultSettingsRepository.getChatVideoQualityPreference()
        if (videoQuality == VideoQuality.ORIGINAL) return null
        return getCacheFileForChatUploadUseCase(file)?.also { destination ->
            compressVideoUseCase(
                rootPath = destination.parent,
                filePath = file.absolutePath,
                newFilePath = destination.absolutePath,
                quality = videoQuality,
            ).lastOrNull()
        }?.takeIf { it.exists() }
    }
}