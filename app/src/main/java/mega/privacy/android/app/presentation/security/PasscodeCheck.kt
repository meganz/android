package mega.privacy.android.app.presentation.security

/**
 * Passcode check
 *
 * Interface for the legacy Passcode Facade
 */
interface PasscodeCheck {
    /**
     * Disable passcode
     */
    fun disablePasscode()

    /**
     * Enable pass code
     *
     */
    fun enablePassCode()

    /**
     * Can lock
     *
     * @return true if passcode can lock
     */
    fun canLock(): Boolean
}
