package mega.privacy.android.app.presentation.verifytwofactor.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.presentation.verifytwofactor.VerifyTwoFactorViewModel
import mega.privacy.android.app.presentation.verifytwofactor.model.PasswordChangedAction

/**
 * Stateful entry point for the verify-2FA screen. Observes [VerifyTwoFactorViewModel]
 * events and routes them to the activity-side callbacks.
 *
 * @param viewModel ViewModel that owns the PIN state and the dispatch logic.
 * @param onFinish Activity callback to finish() after a result dialog is dismissed.
 * @param onDisableSuccess Activity callback to set `RESULT_OK` after a successful 2FA disable.
 * @param onLogout Activity callback to log out after a password change with `KEY_IS_LOGOUT == true`.
 * @param onNavigateToMyAccount Activity callback to open MyAccount after a non-logout password change.
 */
@Composable
fun VerifyTwoFactorScreen(
    viewModel: VerifyTwoFactorViewModel,
    onFinish: () -> Unit,
    onDisableSuccess: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToMyAccount: (resultCode: Int) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    EventEffect(uiState.disableSuccessEvent, viewModel::onDisableSuccessEventConsumed) {
        onDisableSuccess()
    }
    EventEffect(uiState.logoutEvent, viewModel::onLogoutEventConsumed) {
        onLogout()
    }
    EventEffect(uiState.passwordChangedEvent, viewModel::onPasswordChangedEventConsumed) { action ->
        when (action) {
            is PasswordChangedAction.NavigateToMyAccount -> {
                onNavigateToMyAccount(action.resultCode)
                onFinish()
            }
        }
    }

    VerifyTwoFactorContent(
        state = uiState,
        onBack = onFinish,
        onPinChanged = viewModel::onPinChanged,
        onLostAuthenticatorDevice = { context.launchUrl(uiState.recoveryUrl) },
        onResultDismissed = {
            viewModel.onResultEventConsumed()
            onFinish()
        },
    )
}
