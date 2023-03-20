package mega.privacy.android.domain.usecase.camerauploads

/**
 * Use Case to retrieve the maximum video file size that can be compressed
 */
fun interface GetVideoCompressionSizeLimit {

    /**
     * Invocation function
     *
     * @return An [Int] that represents the maximum video file size that can be compressed
     */
    suspend operator fun invoke(): Int
}