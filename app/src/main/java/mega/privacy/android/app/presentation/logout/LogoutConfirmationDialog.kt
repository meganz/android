package mega.privacy.android.app.presentation.logout

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.logout.model.LogoutState
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

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
        hasOfflineFiles && hasTransfers -> stringResource(id = R.string.logout_warning_dialog_offline_and_transfers_message)
        hasOfflineFiles -> stringResource(id = R.string.logout_warning_dialog_offline_message)
        else -> stringResource(id = R.string.logout_warning_dialog_transfers_message)
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
        title = stringResource(id = R.string.logout_warning_dialog_title),
        confirmButtonText = stringResource(id = R.string.logout_warning_dialog_positive_button),
        cancelButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
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
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ConfirmationDialog(
            logoutState = LogoutState.Data(hasOfflineFiles = true, hasPendingTransfers = true),
            logout = {},
            onDismissed = {}
        )
    }
}