package mega.privacy.android.app.presentation.passcode.view

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.biometric.BiometricPrompt.CryptoObject
import androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setText
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.findFragmentActivity
import mega.privacy.android.app.presentation.logout.LogoutConfirmationDialog
import mega.privacy.android.app.presentation.passcode.PasscodeUnlockViewModel
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.passcode.model.PasscodeUIType
import mega.privacy.android.app.presentation.passcode.model.PasscodeUnlockState
import mega.privacy.android.core.ui.controls.buttons.OutlinedMegaButton
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.core.ui.controls.textfields.PasscodeField
import mega.privacy.android.core.ui.controls.textfields.PasswordTextField
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.grey_100_alpha_060_dark_grey
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.mobile.analytics.event.ForgotPasscodeButtonPressedEvent
import mega.privacy.mobile.analytics.event.PasscodeBiometricUnlockDialogEvent
import mega.privacy.mobile.analytics.event.PasscodeEnteredEvent
import mega.privacy.mobile.analytics.event.PasscodeLogoutButtonPressedEvent
import mega.privacy.mobile.analytics.event.PasscodeUnlockDialogEvent
import timber.log.Timber

/**
 * Passcode dialog
 *
 * @param passcodeUnlockViewModel
 */
@Composable
internal fun PasscodeView(
    passcodeUnlockViewModel: PasscodeUnlockViewModel = viewModel(),
    biometricAuthIsAvailable: (Context) -> Boolean = ::areBiometricsEnabled,
    showBiometricAuth: (
        onSuccess: () -> Unit,
        onError: () -> Unit,
        onFail: () -> Unit,
        context: Context,
        promptInfo: BiometricPrompt.PromptInfo,
        cryptObject: CryptoObject,
    ) -> Unit = ::launchBiometricPrompt,
    cryptObjectFactory: PasscodeCryptObjectFactory,
) {
    Timber.d("Passcode main UI composed")
    val uiState by passcodeUnlockViewModel.state.collectAsStateWithLifecycle()

    val activity = (LocalContext.current as? Activity)
    BackHandler {
        activity?.finishAffinity()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        when (val currentState = uiState) {
            PasscodeUnlockState.Loading -> {}
            is PasscodeUnlockState.Data -> {
                val context = LocalContext.current
                var showBiometricPrompt: Boolean by remember {
                    mutableStateOf(
                        biometricPreferenceIsEnabled(uiState) && biometricAuthIsAvailable(
                            context
                        )
                    )
                }

                if (showBiometricPrompt) {
                    Timber.d("Show biometrics UI composed")
                    val title = stringResource(id = R.string.title_unlock_fingerprint)
                    val negativeButton = stringResource(R.string.action_use_passcode)
                    val promptInfo = remember {
                        BiometricPrompt.PromptInfo.Builder()
                            .setTitle(title)
                            .setNegativeButtonText(negativeButton)
                            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                            .build()
                    }

                    LaunchedEffect(key1 = Unit) {
                        Analytics.tracker.trackEvent(PasscodeBiometricUnlockDialogEvent)
                        showBiometricAuth(
                            passcodeUnlockViewModel::unlockWithBiometrics,
                            { showBiometricPrompt = false },
                            passcodeUnlockViewModel::onBiometricAuthFailed,
                            context,
                            promptInfo,
                            cryptObjectFactory()
                        )
                    }

                } else {
                    LaunchedEffect(key1 = showBiometricPrompt) {
                        Analytics.tracker.trackEvent(PasscodeUnlockDialogEvent)
                    }
                    PasscodeContent(
                        onPasswordEntered = passcodeUnlockViewModel::unlockWithPassword,
                        onPasscodeEntered = { passcode ->
                            Analytics.tracker.trackEvent(PasscodeEnteredEvent)
                            passcodeUnlockViewModel.unlockWithPasscode(passcode)
                        },
                        failedAttemptCount = currentState.failedAttempts,
                        showLogoutWarning = currentState.logoutWarning,
                        passcodeType = currentState.passcodeType,
                    )
                }
            }
        }
    }
}

private fun launchBiometricPrompt(
    onSuccess: () -> Unit,
    onError: () -> Unit,
    onFail: () -> Unit,
    context: Context,
    promptInfo: BiometricPrompt.PromptInfo,
    cryptObject: CryptoObject,
) {
    val activity = context.findFragmentActivity()
    if (activity == null) onError()


    val callback = object : AuthenticationCallback() {
        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            onFail()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            if (errorCode == ERROR_USER_CANCELED) {
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

private fun biometricPreferenceIsEnabled(uiState: PasscodeUnlockState) =
    (uiState as? PasscodeUnlockState.Data)?.passcodeType?.biometricEnabled == true

@Composable
private fun PasscodeContent(
    onPasswordEntered: (String) -> Unit,
    onPasscodeEntered: (String) -> Unit,
    failedAttemptCount: Int,
    showLogoutWarning: Boolean,
    passcodeType: PasscodeUIType,
    usePasswordField: Boolean = false,
) {
    Timber.d("Passcode content UI composed")
    var logoutDialog by rememberSaveable { mutableStateOf(false) }
    var usePassword by rememberSaveable { mutableStateOf(usePasswordField) }

    BackHandler(usePassword) {
        usePassword = false
    }

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
            ShowPasswordField(onPasswordEntered)
        } else {
            if (passcodeType is PasscodeUIType.Alphanumeric) {
                ShowPasswordField(onPasscodeEntered, "")
            } else if (passcodeType is PasscodeUIType.Pin) {
                PasscodeField(
                    onComplete = onPasscodeEntered,
                    modifier = Modifier
                        .testTag(PASSCODE_FIELD_TAG)
                        .semantics(
                            mergeDescendants = true,
                            properties = {
                                setText {
                                    onPasscodeEntered(it.text)
                                    true
                                }
                            }
                        ),
                    numberOfCharacters = passcodeType.digits
                )
            }
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
                onClick = {
                    Analytics.tracker.trackEvent(PasscodeLogoutButtonPressedEvent)
                    logoutDialog = true
                },
                textId = R.string.action_logout,
                modifier = Modifier.testTag(LOGOUT_BUTTON_TAG),
                rounded = false,
            )
            Spacer(modifier = Modifier.height(20.dp))
            TextMegaButton(
                onClick = {
                    Analytics.tracker.trackEvent(ForgotPasscodeButtonPressedEvent)
                    usePassword = true
                },
                textId = R.string.settings_passcode_forgot_passcode_button,
                modifier = Modifier.testTag(FORGOT_PASSCODE_BUTTON_TAG),
            )
        }
    }
    if (logoutDialog) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.grey_100_alpha_060_dark_grey,
        ) {}
        LogoutConfirmationDialog(
            onDismissed = { logoutDialog = false }
        )
    }
}

@Composable
private fun ShowPasswordField(onPasswordEntered: (String) -> Unit, hintText: String? = null) {
    var password by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(key1 = Unit) {
        focusRequester.requestFocus()
    }
    PasswordTextField(
        modifier = Modifier
            .fillMaxWidth(0.5f)
            .testTag(PASSWORD_FIELD_TAG)
            .focusRequester(focusRequester),
        onTextChange = { password = it },
        text = password,
        imeAction = ImeAction.Done,
        keyboardActions = KeyboardActions(
            onDone = {
                onPasswordEntered(password)
            }
        ),
        hint = hintText
    )
}

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

private fun areBiometricsEnabled(context: Context) = BiometricManager.from(context).canAuthenticate(
    BiometricManager.Authenticators.BIOMETRIC_STRONG
) == BiometricManager.BIOMETRIC_SUCCESS

internal const val PASSCODE_FIELD_TAG = "passcode_dialog:passcode_field"
internal const val PASSWORD_FIELD_TAG = "passcode_dialog:password_text_field"
internal const val FAILED_ATTEMPTS_TAG = "passcode_dialog:text_field:failed_attempts"
internal const val LOGOUT_BUTTON_TAG = "passcode_dialog:button:log_out"
internal const val FORGOT_PASSCODE_BUTTON_TAG = "passcode_dialog:button:forgot_passcode"

@CombinedThemePreviews
@Composable
private fun PasscodeDialogPreview(
    @PreviewParameter(PasscodeDialogParameterProvider::class) previewParameters: PreviewParameters,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        PasscodeContent(
            onPasscodeEntered = {},
            onPasswordEntered = {},
            failedAttemptCount = previewParameters.attempts,
            showLogoutWarning = previewParameters.showWarning,
            usePasswordField = previewParameters.usePassword,
            passcodeType = previewParameters.passcodeType,
        )
    }
}

private class PreviewParameters(
    val attempts: Int = 0,
    val showWarning: Boolean = false,
    val usePassword: Boolean = false,
    val passcodeType: PasscodeUIType = PasscodeUIType.Pin(false, 4),
)

private class PasscodeDialogParameterProvider : PreviewParameterProvider<PreviewParameters> {
    override val values: Sequence<PreviewParameters>
        get() = sequenceOf(
            PreviewParameters(attempts = 3),
            PreviewParameters(attempts = 6, showWarning = true),
            PreviewParameters(usePassword = true),
            PreviewParameters(attempts = 3, usePassword = true),
            PreviewParameters(attempts = 6, showWarning = true, usePassword = true),
            PreviewParameters(attempts = 3, passcodeType = PasscodeUIType.Alphanumeric(false)),
            PreviewParameters(
                attempts = 6,
                showWarning = true,
                passcodeType = PasscodeUIType.Alphanumeric(false)
            ),
            PreviewParameters(attempts = 3, passcodeType = PasscodeUIType.Pin(false, 6)),
            PreviewParameters(
                attempts = 6,
                showWarning = true,
                passcodeType = PasscodeUIType.Pin(false, 6)
            ),
        )
}