package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.Progress
import java.io.File

/**
 * Chat Upload Compression State
 */
sealed interface ChatUploadCompressionState {
    /**
     * Compressing
     * @property progress the current progress of the compression
     */
    data class Compressing(val progress: Progress) : ChatUploadCompressionState

    /**
     * Compressed
     * @property file the compressed file
     */
    data class Compressed(val file: File) : ChatUploadCompressionState

    /**
     * Not Compressed
     * @property reason the reason why the file was not compressed
     */
    data class NotCompressed(val reason: ChatUploadNotCompressedReason) : ChatUploadCompressionState
}

