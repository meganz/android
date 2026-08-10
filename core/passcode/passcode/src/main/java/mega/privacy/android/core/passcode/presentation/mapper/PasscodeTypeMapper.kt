package mega.privacy.android.core.passcode.presentation.mapper

import mega.privacy.android.core.passcode.presentation.model.PasscodeUIType
import mega.privacy.android.domain.entity.passcode.PasscodeType
import javax.inject.Inject

/**
 * Passcode type mapper
 */
class PasscodeTypeMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param type
     * @return Passcode UI type
     */
    operator fun invoke(type: PasscodeType): PasscodeUIType {
        return when (type) {
            is PasscodeType.Biometric -> mapNonBiometricTypes(type.fallback, true)
            else -> mapNonBiometricTypes(type, false)
        }
    }

    private fun mapNonBiometricTypes(
        type: PasscodeType,
        isBiometricFallback: Boolean,
    ): PasscodeUIType {
        return when (type) {
            PasscodeType.Password -> PasscodeUIType.Alphanumeric(isBiometricFallback)
            is PasscodeType.Pin -> PasscodeUIType.Pin(isBiometricFallback, type.digits)
            else -> throw IllegalStateException("A biometric passcode cannot have a biometric fallback")
        }
    }
}
