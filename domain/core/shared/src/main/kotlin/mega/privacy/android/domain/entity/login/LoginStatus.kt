package mega.privacy.android.domain.entity.login

/**
 * Enum class for defining Login possible statuses.
 */
sealed class LoginStatus {

    /**
     * Login cannot start.
     */
    data object LoginCannotStart : LoginStatus()

    /**
     * Login started.
     */
    data object LoginStarted : LoginStatus()

    /**
     * Login resumed.
     */
    data object LoginResumed : LoginStatus()

    /**
     * Login finished with success.
     */
    data object LoginSucceed : LoginStatus()

    /**
     * Login responded temporary error with retry
     * @property error
     */
    data class LoginWaiting(
        val error: TemporaryWaitingError?,
    ) : LoginStatus()
}