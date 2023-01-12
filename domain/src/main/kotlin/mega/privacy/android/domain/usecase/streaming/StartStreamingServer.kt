package mega.privacy.android.domain.usecase.streaming

/**
 * Start streaming server
 */
fun interface StartStreamingServer {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke()
}