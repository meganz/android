package mega.privacy.android.app.presentation.settings.passcode.biometric

import android.content.Context
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.settings.passcode.mapper.BiometricAuthErrorMapper
import mega.privacy.android.shared.original.core.ui.utils.findFragmentActivity
import javax.inject.Inject

/**
 * Biometric auth
 */
class BiometricAuth @Inject constructor(
    private val biometricAuthErrorMapper: BiometricAuthErrorMapper,
    private val passcodeCryptObjectFactory: PasscodeCryptObjectFactory,
) {

    /**
     * Handler
     */
    interface Handler {
        /**
         * On success
         */
        fun onSuccess()

        /**
         * On failure
         */
        fun onFailure()

        /**
         * On error
         *
         * @param error
         */
        fun onError(error: BiometricAuthError)
    }

    /**
     * Show prompt
     *
     * @param handler
     */
    @Composable
    fun AuthPrompt(handler: Handler) {
        val context = LocalContext.current
        val activity = context.findFragmentActivity()
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                handler.onSuccess()
            }

            override fun onAuthenticationFailed() {
                handler.onFailure()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                handler.onError(biometricAuthErrorMapper(errorCode, errString.toString()))
            }
        }

        if (activity == null) {
            handler.onError(BiometricAuthError.NoActivityFound)
        } else {
            BiometricPrompt(
                activity,
                callback
            ).authenticate(
                promptInfo(activity),
                passcodeCryptObjectFactory()
            )
        }
    }

    private fun promptInfo(context: Context) =
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.title_enable_fingerprint))
            .setNegativeButtonText(context.getString(R.string.general_cancel))
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()
}