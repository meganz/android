package mega.privacy.android.domain.usecase.streaming

/**
 * Stop streaming server
 */
fun interface StopStreamingServer {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke()
}