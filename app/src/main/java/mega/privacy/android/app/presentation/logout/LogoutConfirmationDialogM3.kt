package mega.privacy.android.app.presentation.logout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.indicators.LargeHUD
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.logout.model.LogoutState
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun LogoutConfirmationDialogM3(
    logoutState: LogoutState,
    onLogout: () -> Unit,
    onDismissed: () -> Unit,
) {
    when (logoutState) {
        is LogoutState.Data -> {
            val confirmationMessage = getConfirmationMessage(
                hasOfflineFiles = logoutState.hasOfflineFiles,
                hasTransfers = logoutState.hasPendingTransfers
            )

            BasicDialog(
                title = stringResource(id = R.string.logout_warning_dialog_title),
                description = confirmationMessage,
                positiveButtonText = stringResource(id = R.string.logout_warning_dialog_positive_button),
                onPositiveButtonClicked =
                    onLogout,
                negativeButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
                onNegativeButtonClicked = onDismissed
            )
        }

        LogoutState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                LargeHUD(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag(LogoutConfirmationDialogM3TestTags.LOADING_HUD)
                )
            }
        }

        LogoutState.Error -> {
            BasicDialog(
                title = stringResource(id = R.string.general_error_word),
                description = stringResource(id = sharedR.string.general_text_error),
                positiveButtonText = stringResource(id = sharedR.string.general_ok),
                onPositiveButtonClicked = onDismissed,
                negativeButtonText = null,
                onNegativeButtonClicked = null
            )
        }

        LogoutState.Success -> {
            // Success state - this might trigger navigation to login screen
            // or the dialog might be dismissed automatically
            // The parent component should handle this state
        }
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

internal object LogoutConfirmationDialogM3TestTags {
    private const val LOGOUT_CONFIRMATION_DIALOG_M3 = "logout_confirmation_dialog_m3"
    const val LOADING_HUD = "$LOGOUT_CONFIRMATION_DIALOG_M3:loading_hud"
}

@CombinedThemePreviews
@Composable
private fun LogoutConfirmationDialogM3Preview() {
    AndroidThemeForPreviews {
        LogoutConfirmationDialogM3(
            logoutState = LogoutState.Data(hasOfflineFiles = true, hasPendingTransfers = true),
            onLogout = {},
            onDismissed = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun LogoutConfirmationDialogM3ErrorPreview() {
    AndroidThemeForPreviews {
        LogoutConfirmationDialogM3(
            logoutState = LogoutState.Error,
            onLogout = {},
            onDismissed = {}
        )
    }
}
