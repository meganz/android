package mega.privacy.android.domain.entity.passcode

/**
 * Unlock passcode request
 *
 */
sealed interface UnlockPasscodeRequest {
    /**
     * Value
     */
    val value: String

    /**
     * Passcode request
     *
     * @property value
     */
    data class PasscodeRequest(override val value: String) : UnlockPasscodeRequest

    /**
     * Password request
     *
     * @property value
     */
    data class PasswordRequest(override val value: String) : UnlockPasscodeRequest
}
