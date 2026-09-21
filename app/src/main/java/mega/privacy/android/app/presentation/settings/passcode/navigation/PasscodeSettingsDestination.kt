package mega.privacy.android.app.presentation.settings.passcode.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import mega.privacy.android.app.presentation.settings.passcode.PasscodeSettingsViewModel
import mega.privacy.android.app.presentation.settings.passcode.biometric.BiometricAuth
import mega.privacy.android.app.presentation.settings.passcode.biometric.BiometricAuthError
import mega.privacy.android.app.presentation.settings.passcode.view.PasscodeSettingsView
import mega.privacy.android.core.passcode.biometric.hasBiometricCapability
import timber.log.Timber

/**
 * Passcode settings destination
 */
internal const val PasscodeSettingsDestination = "PasscodeSettingsDestination"

internal fun NavGraphBuilder.passCodeSettings(
    biometricAuth: BiometricAuth,
    navigateToSetOrChangePasscode: (isEdit: Boolean) -> Unit,
    navigateToSelectTimeout: () -> Unit,
) {
    composable(PasscodeSettingsDestination) {
        val viewModel = hiltViewModel<PasscodeSettingsViewModel>()
        val uiState by viewModel.state.collectAsStateWithLifecycle()
        val context = LocalContext.current
        var biometricCapability by remember { mutableStateOf(false) }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            biometricCapability = hasBiometricCapability(context)
        }

        PasscodeSettingsView(
            state = uiState,
            onDisablePasscode = viewModel::disablePasscode,
            onDisableBiometrics = viewModel::disableBiometrics,
            navigateToSetOrChangePasscode = navigateToSetOrChangePasscode,
            navigateToSelectTimeout = navigateToSelectTimeout,
            hasBiometricCapability = biometricCapability,
            authenticateBiometrics = { onSuccess, onComplete ->
                biometricAuth.AuthPrompt(handler = object : BiometricAuth.Handler {
                    override fun onSuccess() {
                        viewModel.enableBiometrics()
                        onSuccess()
                    }

                    override fun onFailure() {
                        Timber.w("Biometric authentication failed during passcode setup")
                        onComplete()
                    }

                    override fun onError(error: BiometricAuthError) {
                        Timber.e("Biometric auth in passcode settings screen failed with an error, ${error.reason}")
                        onComplete()
                    }

                })
            }
        )
    }
}