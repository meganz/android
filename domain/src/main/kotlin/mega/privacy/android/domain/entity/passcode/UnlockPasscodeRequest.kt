package mega.privacy.android.domain.entity.passcode

/**
 * Unlock passcode request
 *
 */
sealed interface UnlockPasscodeRequest {

    /**
     * Passcode request
     *
     * @property value
     */
    data class PasscodeRequest(val value: String) : UnlockPasscodeRequest

    /**
     * Password request
     *
     * @property value
     */
    data class PasswordRequest(val value: String) : UnlockPasscodeRequest

    /**
     * BiometricRequest
     */
    object BiometricRequest : UnlockPasscodeRequest
}
