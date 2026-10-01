package mega.privacy.android.domain.exception

/**
 * Change email exception
 *
 */
sealed class ChangeEmailException : RuntimeException("ChangeEmailException") {
    /**
     * Email in use
     *
     */
    object EmailInUse : ChangeEmailException() {
        private fun readResolve(): Any = EmailInUse
    }

    /**
     * Already requested
     *
     */
    object AlreadyRequested : ChangeEmailException() {
        private fun readResolve(): Any = AlreadyRequested
    }

    /**
     * Unknown
     *
     * @param errorCode
     */
    class Unknown(errorCode: Int) : ChangeEmailException()

    /**
     * Too many attempts.
     */
    data object TooManyAttemptsException : ChangeEmailException() {
        private fun readResolve(): Any = TooManyAttemptsException
    }
}
