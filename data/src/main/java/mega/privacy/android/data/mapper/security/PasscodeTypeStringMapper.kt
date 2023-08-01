package mega.privacy.android.data.mapper.security

import mega.privacy.android.domain.entity.passcode.PasscodeType
import javax.inject.Inject

/**
 * Passcode type string mapper
 */
internal class PasscodeTypeStringMapper @Inject constructor() {

    /**
     * Invoke
     *
     * @param passcodeType
     * @return string value for the selected passcode type
     */
    operator fun invoke(passcodeType: PasscodeType) =
        when (passcodeType) {
            is PasscodeType.Biometric -> throw IllegalArgumentException("Biometric passcode types need to be handled. Only fallback type is mapped")
            PasscodeType.Password -> "alphanumeric"
            is PasscodeType.Pin -> passcodeType.digits.toString()
        }
}