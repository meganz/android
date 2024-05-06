package mega.privacy.android.domain.usecase.chat

/**
 * Chat Upload Not Compressed Reason
 */
sealed interface ChatUploadNotCompressedReason {
    /**
     * The compression is not needed because the settings are set to original quality or the file is not supported
     */
    data object CompressionNotNeeded : ChatUploadNotCompressedReason

    /**
     * Failed to create cache file
     */
    object NoCacheFile : RuntimeException("Failed to create cache file"),
        ChatUploadNotCompressedReason

    /**
     * Failed to compress file
     */
    object FailedToCompress : RuntimeException("Failed to compress file"),
        ChatUploadNotCompressedReason
}