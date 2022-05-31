package mega.privacy.android.app.domain.usecase

/**
 * Retry pending connections use case.
 */
interface RetryPendingConnections {

    /**
     * Invoke.
     *
     * @param disconnect True if should disconnect megaChatApi, false otherwise.
     */
    operator fun invoke(disconnect: Boolean)
}