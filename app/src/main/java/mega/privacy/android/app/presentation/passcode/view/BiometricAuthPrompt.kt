package mega.privacy.android.app.presentation.passcode.view

import android.content.Context
import androidx.biometric.BiometricPrompt
import mega.privacy.android.shared.original.core.ui.utils.findFragmentActivity

/**
 * Biometric auth prompt
 *
 * @param onSuccess
 * @param onError
 * @param onFail
 * @param context
 * @param promptInfo
 * @param cryptObject
 */
internal fun biometricAuthPrompt(
    onSuccess: () -> Unit,
    onError: () -> Unit,
    onFail: () -> Unit,
    context: Context,
    promptInfo: BiometricPrompt.PromptInfo,
    cryptObject: BiometricPrompt.CryptoObject,
) {
    val activity = context.findFragmentActivity()
    if (activity == null) onError()


    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            onFail()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            if (errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                activity?.finish()
            } else {
                onError()
            }
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onSuccess()
        }
    }
    activity?.let { BiometricPrompt(it, callback).authenticate(promptInfo, cryptObject) }
}