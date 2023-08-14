package mega.privacy.android.app.presentation.passcode.model

/**
 * Passcode ui type
 */
sealed interface PasscodeUIType {
    /**
     * Biometric enabled
     */
    val biometricEnabled: Boolean

    /**
     * Alphanumeric
     *
     * @property biometricEnabled
     */
    data class Alphanumeric(override val biometricEnabled: Boolean) : PasscodeUIType

    /**
     * Pin
     *
     * @property biometricEnabled
     * @property digits
     */
    data class Pin(override val biometricEnabled: Boolean, val digits: Int) : PasscodeUIType

}