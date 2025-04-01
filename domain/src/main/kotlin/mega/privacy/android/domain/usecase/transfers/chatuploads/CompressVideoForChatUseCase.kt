package mega.privacy.android.domain.usecase.transfers.chatuploads

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.VideoCompressionState
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.chat.ChatUploadCompressionState
import mega.privacy.android.domain.usecase.chat.ChatUploadNotCompressedReason
import mega.privacy.android.domain.usecase.transfers.GetCacheFileForChatUploadUseCase
import mega.privacy.android.domain.usecase.video.CompressVideoUseCase
import javax.inject.Inject

/**
 * Use case to compress a video that will be attached to a chat before uploading it.
 */
class CompressVideoForChatUseCase @Inject constructor(
    private val defaultSettingsRepository: SettingsRepository,
    private val getCacheFileForChatUploadUseCase: GetCacheFileForChatUploadUseCase,
    private val compressVideoUseCase: CompressVideoUseCase,
) {

    /**
     * Invoke
     *
     * @return the downscaled video or null if it's not scaled.
     */
    suspend operator fun invoke(uriPath: UriPath): Flow<ChatUploadCompressionState> {
        val videoQuality = defaultSettingsRepository.getChatVideoQualityPreference()
        if (videoQuality == VideoQuality.ORIGINAL) return flowOf(
            ChatUploadCompressionState.NotCompressed(
                ChatUploadNotCompressedReason.CompressionNotNeeded
            )
        )
        return getCacheFileForChatUploadUseCase(uriPath)?.let { destination ->
            compressVideoUseCase(
                rootPath = destination.parent,
                original = uriPath,
                newFilePath = destination.absolutePath,
                quality = videoQuality,
            ).mapNotNull { videoCompressionState ->
                when (videoCompressionState) {
                    is VideoCompressionState.Progress -> {
                        ChatUploadCompressionState.Compressing(Progress(videoCompressionState.progress))
                    }

                    else -> null as ChatUploadCompressionState?
                }
            }.onCompletion {
                if (destination.exists()) {
                    emit(ChatUploadCompressionState.Compressed(destination))
                } else {
                    emit(ChatUploadCompressionState.NotCompressed(ChatUploadNotCompressedReason.FailedToCompress))
                }
            }
        } ?: flowOf(
            ChatUploadCompressionState.NotCompressed(ChatUploadNotCompressedReason.NoCacheFile)
        )
    }
}