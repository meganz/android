package mega.privacy.android.domain.usecase.transfers.chatuploads

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.chat.ChatUploadCompressionState
import mega.privacy.android.domain.usecase.chat.ChatUploadNotCompressedReason
import mega.privacy.android.domain.usecase.transfers.GetCacheFileForUploadUseCase
import java.io.File
import javax.inject.Inject

/**
 * Use case to downscale an image that will be attached to a chat before uploading and return the state of the compression..
 */
class DownscaleImageForChatUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
    private val getCacheFileForUploadUseCase: GetCacheFileForUploadUseCase,
) {
    /**
     * Invoke
     *
     * @return the state of the compression.
     */
    suspend operator fun invoke(file: File): Flow<ChatUploadCompressionState> {
        return getCacheFileForUploadUseCase(file, true)?.let { destination ->
            fileSystemRepository.downscaleImage(
                file = file,
                destination = destination,
                maxPixels = DOWNSCALE_IMAGES_PX
            )
            flowOf(
                if (destination.exists()) {
                    ChatUploadCompressionState.Compressed(destination)
                } else {
                    ChatUploadCompressionState.NotCompressed(ChatUploadNotCompressedReason.FailedToCompress)
                }
            )
        } ?: flowOf(
            ChatUploadCompressionState.NotCompressed(ChatUploadNotCompressedReason.NoCacheFile)
        )
    }

    companion object {
        internal const val DOWNSCALE_IMAGES_PX = 2000000L
    }
}