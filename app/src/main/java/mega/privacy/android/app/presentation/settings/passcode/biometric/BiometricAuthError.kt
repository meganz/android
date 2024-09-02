package mega.privacy.android.app.presentation.settings.passcode.biometric

/**
 * Biometric auth error
 *
 * @param reason
 */
sealed class BiometricAuthError(open val reason: String) {
    /**
     * No activity found
     */
    data object NoActivityFound :
        BiometricAuthError("No fragment activity found in view hierarchy. BiometricAuth requires a fragmentActivity as host.")

    /**
     * User declined
     */
    data object UserDeclined :
        BiometricAuthError("Authentication process was cancelled by the user")

    /**
     * General error
     *
     * @property reason
     */
    data class GeneralError(override val reason: String) : BiometricAuthError(reason)
}