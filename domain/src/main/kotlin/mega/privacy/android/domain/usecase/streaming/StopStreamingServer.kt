package mega.privacy.android.domain.usecase.streaming

/**
 * Start streaming server
 */
fun interface StopStreamingServer {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke()
}