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
    object EmailInUse : ChangeEmailException()

    /**
     * Already requested
     *
     */
    object AlreadyRequested : ChangeEmailException()

    /**
     * Unknown
     *
     * @param errorCode
     */
    class Unknown(errorCode: Int) : ChangeEmailException()
}