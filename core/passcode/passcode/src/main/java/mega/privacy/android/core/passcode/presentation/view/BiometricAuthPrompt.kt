package mega.privacy.android.core.passcode.presentation.view

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity

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

/**
 * Code repeated here for now, we will remove it once we have migrated to the compose version of the biometric auth library.
 */
private fun Context.findFragmentActivity(): FragmentActivity? {
    var currentContext = this
    var previousContext: Context? = null
    while (currentContext is ContextWrapper && previousContext != currentContext) {
        if (currentContext is FragmentActivity) {
            return currentContext
        }
        previousContext = currentContext
        currentContext = currentContext.baseContext
    }
    return null
}
