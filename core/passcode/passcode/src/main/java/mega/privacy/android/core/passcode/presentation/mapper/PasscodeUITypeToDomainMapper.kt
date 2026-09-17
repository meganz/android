package mega.privacy.android.core.passcode.presentation.mapper

import mega.privacy.android.core.passcode.presentation.model.PasscodeUIType
import mega.privacy.android.domain.entity.passcode.PasscodeType
import javax.inject.Inject

/**
 * Inverse of [PasscodeTypeMapper].
 *
 * Re-wrapping in [PasscodeType.Biometric] is what keeps biometric unlock enabled across a passcode
 * change; writing the bare type would silently turn it off.
 */
class PasscodeUITypeToDomainMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param type
     * @return Passcode type
     */
    operator fun invoke(type: PasscodeUIType): PasscodeType {
        val passcodeType = when (type) {
            is PasscodeUIType.Alphanumeric -> PasscodeType.Password
            is PasscodeUIType.Pin -> PasscodeType.Pin(type.digits)
        }
        return if (type.biometricEnabled) PasscodeType.Biometric(passcodeType) else passcodeType
    }
}
