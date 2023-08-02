package mega.privacy.android.app.presentation.passcode.mapper

import mega.privacy.android.app.presentation.passcode.model.PasscodeUIType
import mega.privacy.android.domain.entity.passcode.PasscodeType

/**
 * Passcode type mapper
 */
internal class PasscodeTypeMapper {
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
