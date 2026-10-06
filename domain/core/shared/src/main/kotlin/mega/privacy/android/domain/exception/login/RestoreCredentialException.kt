package mega.privacy.android.domain.exception.login

/**
 * Failure of a restore credential operation.
 */
sealed class RestoreCredentialException(cause: Throwable?) :
    RuntimeException("RestoreCredentialException", cause) {

    /**
     * The device holds no restore credential for the app.
     */
    class NoCredential(cause: Throwable? = null) : RestoreCredentialException(cause)

    /**
     * The request JSON was rejected as malformed or not conforming to the expected format.
     */
    class InvalidRequest(cause: Throwable? = null) : RestoreCredentialException(cause)

    /**
     * The platform could not complete the operation for any other reason.
     */
    class Failed(cause: Throwable? = null) : RestoreCredentialException(cause)
}
