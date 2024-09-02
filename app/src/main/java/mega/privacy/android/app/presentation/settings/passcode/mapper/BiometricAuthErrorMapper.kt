package mega.privacy.android.app.presentation.settings.passcode.mapper

import androidx.biometric.BiometricPrompt
import mega.privacy.android.app.presentation.settings.passcode.biometric.BiometricAuthError
import javax.inject.Inject

/**
 * Biometric auth error mapper
 */
class BiometricAuthErrorMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param errorCode
     * @param message
     */
    operator fun invoke(errorCode: Int, message: String): BiometricAuthError {
        return when (errorCode) {
            BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_NEGATIVE_BUTTON -> BiometricAuthError.UserDeclined
            else -> BiometricAuthError.GeneralError(message)
        }
    }
}