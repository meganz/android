package mega.privacy.android.app.presentation.passcode.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.logout.LogoutConfirmationDialog
import mega.privacy.android.app.presentation.passcode.PasscodeUnlockViewModel
import mega.privacy.android.core.ui.controls.buttons.OutlinedMegaButton
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.core.ui.controls.textfields.PasscodeField
import mega.privacy.android.core.ui.controls.textfields.PasswordTextField
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme

/**
 * Passcode dialog
 *
 * @param passcodeUnlockViewModel
 */
@Composable
fun PasscodeDialog(
    passcodeUnlockViewModel: PasscodeUnlockViewModel = viewModel(),
) {
    val uiState by passcodeUnlockViewModel.state.collectAsStateWithLifecycle()

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        )
    ) {
        DialogContent(
            onPasswordEntered = passcodeUnlockViewModel::unlockWithPassword,
            onPasscodeEntered = passcodeUnlockViewModel::unlockWithPasscode,
            failedAttemptCount = uiState.failedAttempts,
            showLogoutWarning = uiState.logoutWarning
        )
    }
}

@Composable
private fun DialogContent(
    onPasswordEntered: (String) -> Unit,
    onPasscodeEntered: (String) -> Unit,
    failedAttemptCount: Int,
    showLogoutWarning: Boolean,
    usePasswordField: Boolean = false,
) {
    var logoutDialog by rememberSaveable { mutableStateOf(false) }
    var usePassword by rememberSaveable { mutableStateOf(usePasswordField) }

    BackHandler(usePassword) {
        usePassword = false
    }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = if (usePassword) {
                    stringResource(
                        id = R.string.settings_passcode_enter_password_title
                    )
                } else {
                    stringResource(
                        id = R.string.unlock_pin_title
                    )
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (usePassword) {
                var password by remember { mutableStateOf("") }
                PasswordTextField(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .testTag(PASSWORD_FIELD_TAG),
                    onTextChange = { password = it },
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(
                        onDone = {
                            onPasswordEntered(password)
                        }
                    )
                )
            } else {
                PasscodeField(
                    onComplete = onPasscodeEntered,
                    modifier = Modifier.testTag(PASSCODE_FIELD_TAG)
                )
            }
            if (failedAttemptCount > 0) {
                Spacer(modifier = Modifier.height(20.dp))
                FailedAttemptsView(
                    failedAttempts = failedAttemptCount,
                    modifier = Modifier.testTag(FAILED_ATTEMPTS_TAG)
                )
            }
            if (showLogoutWarning) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(id = R.string.pin_lock_alert),
                    modifier = Modifier.padding(
                        horizontal = 40.dp
                    ),
                    textAlign = TextAlign.Center
                )
            }
            if (failedAttemptCount > 0 && !usePassword) {
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedMegaButton(
                    onClick = { logoutDialog = true },
                    textId = R.string.action_logout,
                    modifier = Modifier.testTag(LOGOUT_BUTTON_TAG),
                )
                Spacer(modifier = Modifier.height(20.dp))
                TextMegaButton(
                    onClick = { usePassword = true },
                    textId = R.string.settings_passcode_forgot_passcode_button,
                    modifier = Modifier.testTag(FORGOT_PASSCODE_BUTTON_TAG),
                )
            }
        }
        if (logoutDialog) {
            LogoutConfirmationDialog(
                onDismissed = { logoutDialog = false }
            )
        }
    }
}

internal const val PASSCODE_FIELD_TAG = "passcode_dialog:passcode_field"
internal const val PASSWORD_FIELD_TAG = "passcode_dialog:password_text_field"
internal const val FAILED_ATTEMPTS_TAG = "passcode_dialog:text_field:failed_attempts"
internal const val LOGOUT_BUTTON_TAG = "passcode_dialog:button:log_out"
internal const val FORGOT_PASSCODE_BUTTON_TAG = "passcode_dialog:button:forgot_passcode"

@Composable
private fun FailedAttemptsView(
    failedAttempts: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = pluralStringResource(
            id = R.plurals.passcode_lock_alert_attempts,
            count = failedAttempts,
            failedAttempts,
        ),
        color = MaterialTheme.colors.onError,
        modifier = modifier
            .background(
                color = MaterialTheme.colors.error,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@CombinedThemePreviews
@Composable
private fun PasscodeDialogPreview(
    @PreviewParameter(PasscodeDialogParameterProvider::class) previewParameters: PreviewParameters,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        DialogContent(
            onPasscodeEntered = {},
            onPasswordEntered = {},
            failedAttemptCount = previewParameters.attempts,
            showLogoutWarning = previewParameters.showWarning,
            usePasswordField = previewParameters.usePassword,
        )
    }
}

private class PreviewParameters(
    val attempts: Int = 0,
    val showWarning: Boolean = false,
    val usePassword: Boolean = false,
)

private class PasscodeDialogParameterProvider : PreviewParameterProvider<PreviewParameters> {
    override val values: Sequence<PreviewParameters>
        get() = sequenceOf(
            PreviewParameters(attempts = 3),
            PreviewParameters(attempts = 6, showWarning = true),
            PreviewParameters(usePassword = true),
            PreviewParameters(attempts = 3, usePassword = true),
            PreviewParameters(attempts = 6, showWarning = true, usePassword = true),
        )
}