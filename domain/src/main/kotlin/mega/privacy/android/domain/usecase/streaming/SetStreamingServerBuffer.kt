package mega.privacy.android.domain.usecase.streaming

/**
 * Set streaming server buffer
 *
 */
fun interface SetStreamingServerBuffer {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke()
}