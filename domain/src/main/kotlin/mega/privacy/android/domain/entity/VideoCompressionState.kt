package mega.privacy.android.domain.entity


/**
 * Single Video Compression State
 */
sealed interface SingleVideoCompressionState

/**
 * Video Compression State
 */
sealed interface VideoCompressionState {

    /**
     * Initial
     */
    object Initial : VideoCompressionState

    /**
     * Update Progress
     */
    data class Progress(
        /**
         * progress
         */
        val progress: Int,
        /**
         * current Index
         */
        val currentIndex: Int,
        /**
         * total Video Size Processed
         */
        val totalSizeProcessed: Long,
        /**
         * path of the output file
         */
        val path: String,
    ) : VideoCompressionState

    /**
     * Successful
     */
    data class Successful(
        /**
         * record
         */
        val id: Int?,
    ) : VideoCompressionState

    /**
     * Failed
     */
    data class Failed(
        /**
         * record
         */
        val id: Int? = null,
    ) : VideoCompressionState

    /**
     * Video Compression Finished
     */
    object Finished : VideoCompressionState

    /**
     * Finished Compression for a single file
     */
    data class FinishedCompression(
        /**
         * returnedFile [String]
         */
        val returnedFile: String,
        /**
         * isSuccess [Boolean]
         */
        val isSuccess: Boolean,
        /**
         * messageId [Long]
         */
        val messageId: Long?,
    ) : SingleVideoCompressionState, VideoCompressionState

    /**
     * Insufficient Storage
     */
    object InsufficientStorage : VideoCompressionState
}
