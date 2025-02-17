package mega.privacy.android.app.presentation.login.view

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaClickableText
import mega.android.core.ui.components.indicators.LargeHUD
import mega.android.core.ui.components.inputfields.DEFAULT_VERIFICATION_INPUT_LENGTH
import mega.android.core.ui.components.inputfields.VerificationTextInputField
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.login.model.LoginState
import mega.privacy.android.app.presentation.login.model.MultiFactorAuthState
import mega.privacy.android.app.presentation.twofactorauthentication.view.TwoFactorAuthenticationField
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.shared.original.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.shared.original.core.ui.controls.progressindicator.MegaCircularProgressIndicator
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews

/**
 * Two Factor Authentication screen.
 * @param state LoginState object.
 * @param on2FAPinChanged Callback to notify the pin changed.
 * @param on2FAChanged Callback to notify the 2FA changed.
 * @param onLostAuthenticatorDevice Callback to notify the user lost the authenticator device.
 * @param onFirstTime2FAConsumed Callback to notify the first time 2FA consumed.
 * @param modifier Modifier.
 */
@Composable
fun TwoFactorAuthentication(
    state: LoginState,
    on2FAPinChanged: (String, Int) -> Unit,
    on2FAChanged: (String) -> Unit,
    onLostAuthenticatorDevice: () -> Unit,
    onFirstTime2FAConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoginNewDesignEnabled) {
        NewTwoFactorAuthentication(
            state = state,
            on2FAChanged = on2FAChanged,
            onLostAuthenticatorDevice = onLostAuthenticatorDevice,
            modifier = modifier
        )
    } else {
        LegacyTwoFactorAuthentication(
            state = state,
            on2FAPinChanged = on2FAPinChanged,
            on2FAChanged = on2FAChanged,
            onLostAuthenticatorDevice = onLostAuthenticatorDevice,
            onFirstTime2FAConsumed = onFirstTime2FAConsumed,
            modifier = modifier
        )
    }
}

@Composable
private fun LegacyTwoFactorAuthentication(
    state: LoginState,
    on2FAPinChanged: (String, Int) -> Unit,
    on2FAChanged: (String) -> Unit,
    onLostAuthenticatorDevice: () -> Unit,
    onFirstTime2FAConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) = Box(
    modifier = modifier
        .fillMaxWidth()
) {
    val scrollState = rememberScrollState()
    val isChecking2FA = state.multiFactorAuthState == MultiFactorAuthState.Checking
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val isError = state.multiFactorAuthState == MultiFactorAuthState.Failed
        MegaText(
            text = stringResource(id = R.string.explain_confirm_2fa),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 40.dp)
                .testTag(ENTER_AUTHENTICATION_CODE_TAG),
            style = MaterialTheme.typography.subtitle1,
            textAlign = TextAlign.Center,
            textColor = TextColor.Secondary,
        )
        TwoFactorAuthenticationField(
            twoFAPin = state.twoFAPin,
            isError = isError,
            on2FAPinChanged = on2FAPinChanged,
            on2FAChanged = on2FAChanged,
            requestFocus = state.isFirstTime2FA,
            onRequestFocusConsumed = onFirstTime2FAConsumed
        )
        if (isError) {
            MegaText(
                text = stringResource(id = R.string.pin_error_2fa),
                modifier = Modifier
                    .padding(start = 10.dp, top = 18.dp, end = 10.dp)
                    .testTag(INVALID_CODE_TAG),
                style = MaterialTheme.typography.caption,
                textColor = TextColor.Error,
            )
        }
        TextMegaButton(
            textId = R.string.lost_your_authenticator_device,
            onClick = onLostAuthenticatorDevice,
            modifier = Modifier
                .padding(top = if (isError) 0.dp else 29.dp, bottom = 40.dp)
                .testTag(LOST_AUTHENTICATION_CODE_TAG)
        )
    }

    if (isChecking2FA) {
        MegaCircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.Center)
                .testTag(TWO_FA_PROGRESS_TEST_TAG)
        )
    }
}

@Composable
private fun NewTwoFactorAuthentication(
    state: LoginState,
    on2FAChanged: (String) -> Unit,
    onLostAuthenticatorDevice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val request = remember { FocusRequester() }
    var code by rememberSaveable { mutableStateOf("") }
    val softKeyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        request.requestFocus()
        softKeyboard?.show()
    }
    AndroidTheme(isDark = state.themeMode.isDarkMode()) {
        Box(modifier = modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState),
            ) {
                MegaText(
                    modifier = Modifier.padding(
                        vertical = LocalSpacing.current.x24
                    ),
                    textColor = TextColor.Secondary,
                    style = AppTheme.typography.bodyLarge,
                    text = stringResource(sharedR.string.multi_factor_auth_login_verification_content)
                )
                VerificationTextInputField(
                    modifier = Modifier
                        .testTag(TWO_FA_INPUT_FIELD_TEST_TAG)
                        .focusRequester(request),
                    value = code,
                    isCodeCorrect = state.multiFactorAuthState?.let { it == MultiFactorAuthState.Passed },
                    errorText = stringResource(sharedR.string.multi_factor_auth_login_verification_input_error_text),
                    onValueChange = {
                        code = it
                        if (code.length == DEFAULT_VERIFICATION_INPUT_LENGTH) {
                            on2FAChanged(code)
                            softKeyboard?.hide()
                        }
                    }
                )
                MegaClickableText(
                    modifier = Modifier
                        .padding(top = LocalSpacing.current.x24),
                    text = stringResource(id = R.string.lost_your_authenticator_device),
                    style = AppTheme.typography.bodyMedium,
                    onClick = onLostAuthenticatorDevice
                )
            }

            if (state.multiFactorAuthState != null && state.multiFactorAuthState != MultiFactorAuthState.Failed) {
                LargeHUD(
                    modifier = Modifier
                        .testTag(TWO_FA_PROGRESS_TEST_TAG)
                        .fillMaxSize()
                        .align(Alignment.Center),
                )
            }
        }
    }
}

/**
 * Tag for the enter authentication code text.
 */
const val TWO_FA_INPUT_FIELD_TEST_TAG = "two_factor_authentication:input_field"

@Composable
@CombinedThemePreviews
private fun NewTwoFactorAuthenticationPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        NewTwoFactorAuthentication(
            state = LoginState(
                isLoginNewDesignEnabled = true,
                themeMode = ThemeMode.System,
                multiFactorAuthState = MultiFactorAuthState.Checking
            ),
            on2FAChanged = {},
            onLostAuthenticatorDevice = {}
        )
    }
}