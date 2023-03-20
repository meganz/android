package mega.privacy.android.domain.usecase.camerauploads

/**
 * Use Case that sets the maximum video file size that can be compressed
 */
fun interface SetVideoCompressionSizeLimit {

    /**
     * Invocation function
     *
     * @param size The maximum video file size that can be compressed
     */
    suspend operator fun invoke(size: Int)
}