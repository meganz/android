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
    data object Initial : VideoCompressionState

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
         * total Video count
         */
        val totalCount: Int,
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
        val id: Long?,
    ) : VideoCompressionState

    /**
     * Failed
     */
    data class Failed(
        /**
         * record
         */
        val id: Long? = null,
    ) : VideoCompressionState

    /**
     * Video Compression Finished
     */
    data object Finished : VideoCompressionState

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
    data object InsufficientStorage : VideoCompressionState

    /**
     * Compression Cancelled
     */
    data object Cancel : VideoCompressionState
}
