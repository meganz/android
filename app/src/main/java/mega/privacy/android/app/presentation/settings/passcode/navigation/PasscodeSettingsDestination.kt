package mega.privacy.android.app.presentation.settings.passcode.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import mega.privacy.android.app.activities.contract.PassCodeActivityContract
import mega.privacy.android.app.presentation.settings.passcode.PasscodeSettingsViewModel
import mega.privacy.android.app.presentation.settings.passcode.biometric.BiometricAuth
import mega.privacy.android.app.presentation.settings.passcode.biometric.BiometricAuthError
import mega.privacy.android.app.presentation.settings.passcode.view.PasscodeSettingsView
import timber.log.Timber

/**
 * Passcode settings destination
 */
internal const val PasscodeSettingsDestination = "PasscodeSettingsDestination"

internal fun NavGraphBuilder.passCodeSettings(
    navController: NavHostController,
    biometricAuth: BiometricAuth,
    navigateToSelectTimeout: () -> Unit,
) {
    composable(PasscodeSettingsDestination) {
        val viewModel = hiltViewModel<PasscodeSettingsViewModel>()
        val uiState by viewModel.state.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val hasBiometricCapability = remember {
            BiometricManager.from(context).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            ) == BIOMETRIC_SUCCESS
        }

        val launcher = rememberLauncherForActivityResult(
            contract = PassCodeActivityContract()
        ) { isSuccess ->
            if (!isSuccess) {
                Timber.w("Set passcode pin failed when enabling passcode")
            }
        }

        PasscodeSettingsView(
            state = uiState,
            onDisablePasscode = viewModel::disablePasscode,
            onDisableBiometrics = viewModel::disableBiometrics,
            navigateToSetOrChangePasscode = launcher::launch,
            navigateToSelectTimeout = navigateToSelectTimeout,
            hasBiometricCapability = hasBiometricCapability,
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