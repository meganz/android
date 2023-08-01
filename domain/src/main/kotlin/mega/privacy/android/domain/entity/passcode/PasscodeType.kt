package mega.privacy.android.domain.entity.passcode

/**
 * Passcode type
 */
sealed interface PasscodeType {

    /**
     * Pin
     *
     * @property digits
     */
    data class Pin(val digits: Int) : PasscodeType

    /**
     * Password
     */
    object Password : PasscodeType

    /**
     * Biometric
     *
     * @property fallback
     */
    data class Biometric(val fallback: PasscodeType) : PasscodeType
}