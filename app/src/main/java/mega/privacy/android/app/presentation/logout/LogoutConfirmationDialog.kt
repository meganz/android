package mega.privacy.android.app.presentation.logout

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.logout.model.LogoutState
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.preview.CombinedThemePreviews

@Composable
internal fun LogoutConfirmationDialog(
    onDismissed: () -> Unit = {},
    logoutViewModel: LogoutViewModel = viewModel(),
) {
    val logoutState by logoutViewModel.state.collectAsStateWithLifecycle()
    ConfirmationDialog(
        logoutState = logoutState,
        logout = logoutViewModel::logout,
        onDismissed = onDismissed
    )

}

@Composable
private fun ConfirmationDialog(
    logoutState: LogoutState,
    logout: () -> Unit,
    onDismissed: () -> Unit,
) {
    when (logoutState) {
        is LogoutState.Data -> {
            val confirmationMessage =
                getConfirmationMessage(
                    hasOfflineFiles = logoutState.hasOfflineFiles,
                    hasTransfers = logoutState.hasPendingTransfers
                )
            ShowDialog(confirmationMessage, logout, onDismissed)
        }

        LogoutState.Loading -> {}
    }
}

@Composable
private fun getConfirmationMessage(hasOfflineFiles: Boolean, hasTransfers: Boolean): String {
    return when {
        hasOfflineFiles && hasTransfers -> stringResource(id = R.string.logout_warning_offline_and_transfers)
        hasOfflineFiles -> stringResource(id = R.string.logout_warning_offline)
        else -> stringResource(id = R.string.logout_warning_transfers)
    }
}

@Composable
private fun ShowDialog(
    confirmationMessage: String,
    logout: () -> Unit,
    onDismissed: () -> Unit,
) {
    MegaAlertDialog(
        text = confirmationMessage,
        confirmButtonText = stringResource(id = R.string.action_logout),
        cancelButtonText = stringResource(id = R.string.general_cancel),
        onConfirm = {
            logout()
            onDismissed()
        },
        onDismiss = onDismissed
    )
}


@CombinedThemePreviews
@Composable
private fun LogoutConfirmationDialogPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ConfirmationDialog(
            logoutState = LogoutState.Data(hasOfflineFiles = true, hasPendingTransfers = true),
            logout = {},
            onDismissed = {}
        )
    }
}