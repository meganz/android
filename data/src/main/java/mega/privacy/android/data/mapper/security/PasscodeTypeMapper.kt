package mega.privacy.android.data.mapper.security

import mega.privacy.android.domain.entity.passcode.PasscodeType
import javax.inject.Inject

internal class PasscodeTypeMapper @Inject constructor() {
    operator fun invoke(typeString: String?, biometricsEnabled: Boolean?) =
        getTypeFromString(typeString)?.let {
            if (biometricsEnabled == true) PasscodeType.Biometric(
                it
            ) else it
        }

    private fun getTypeFromString(typeString: String?) = when (typeString) {
        "4" -> PasscodeType.Pin(4)
        "6" -> PasscodeType.Pin(6)
        "alphanumeric" -> PasscodeType.Password
        else -> null
    }
}