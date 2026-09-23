package mega.privacy.android.core.passcode

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
