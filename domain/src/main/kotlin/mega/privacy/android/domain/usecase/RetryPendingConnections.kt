package mega.privacy.android.domain.usecase

/**
 * Retry pending connections use case.
 */
fun interface RetryPendingConnections {

    /**
     * Invoke.
     *
     * @param disconnect True if should disconnect megaChatApi, false otherwise.
     */
    suspend operator fun invoke(disconnect: Boolean)
}