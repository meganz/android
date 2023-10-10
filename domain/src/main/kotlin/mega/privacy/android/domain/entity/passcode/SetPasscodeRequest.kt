package mega.privacy.android.domain.entity.passcode

/**
 * Set passcode request
 *
 * @property type
 * @property passcode
 */
data class SetPasscodeRequest(
    val type: PasscodeType,
    val passcode: String
)
