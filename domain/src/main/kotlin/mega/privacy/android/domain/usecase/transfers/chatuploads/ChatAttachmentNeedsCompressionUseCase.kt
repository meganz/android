package mega.privacy.android.domain.usecase.transfers.chatuploads

import kotlinx.coroutines.flow.first
import mega.privacy.android.domain.entity.ChatImageQuality
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.NetworkRepository
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.file.IsImageFileUseCase
import mega.privacy.android.domain.usecase.file.IsVideoFileUseCase
import java.io.File
import javax.inject.Inject

/**
 * Returns if the file needs to be compressed.
 * For images, will return true if:
 *  - is not gif
 *  - the user settings and wifi connection indicates it should be optimized
 *  - the original resolution is bigger than target maximum resolution
 * For videos, will return true if:
 * - it's mp4 extension
 * - the user settings indicates it should be compressed
 * In other cases returns false
 */
class ChatAttachmentNeedsCompressionUseCase @Inject constructor(
    private val isImageFileUseCase: IsImageFileUseCase,
    private val isVideoFileUseCase: IsVideoFileUseCase,
    private val defaultSettingsRepository: SettingsRepository,
    private val networkRepository: NetworkRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(file: File): Boolean {
        val path = file.absolutePath
        when {
            isImageFileUseCase(path) -> {
                if (file.isGif()) return false
                val imageQuality = defaultSettingsRepository.getChatImageQuality().first()
                return !(imageQuality == ChatImageQuality.Original
                        || (imageQuality == ChatImageQuality.Automatic && networkRepository.isOnWifi()))
            }

            isVideoFileUseCase(path) && VIDEO_COMPRESSION_ISSUE_FIXED -> {
                if (file.extension != "mp4") return false
                val videoQuality = defaultSettingsRepository.getChatVideoQualityPreference()
                return videoQuality != VideoQuality.ORIGINAL
            }
        }
        return false
    }

    private fun File.isGif() = listOf("gif", "webp").contains(extension)

    companion object {
        /**
         * Video compression has an issue with landscape videos, this flag will be removed once solved
         */
        internal const val VIDEO_COMPRESSION_ISSUE_FIXED = false
    }
}