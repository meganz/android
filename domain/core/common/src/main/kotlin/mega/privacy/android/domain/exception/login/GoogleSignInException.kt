package mega.privacy.android.domain.exception.login

/**
 * Exceptions that can occur during Google Sign-In.
 */
sealed class GoogleSignInException : RuntimeException() {
    /**
     * User cancelled the Google Sign-In flow.
     */
    data object Cancelled : GoogleSignInException()

    /**
     * No Google credential available on device.
     */
    data object NoCredential : GoogleSignInException()

    /**
     * Unknown error during Google Sign-In.
     */
    data class Unknown(override val message: String?) : GoogleSignInException()
}
