package mega.privacy.android.app.presentation.login.view

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.MegaClickableText
import mega.android.core.ui.components.MegaText as MegaText3
import mega.android.core.ui.components.indicators.LargeHUD
import mega.android.core.ui.components.inputfields.DEFAULT_VERIFICATION_INPUT_LENGTH
import mega.android.core.ui.components.inputfields.VerificationTextInputField
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.devicetype.DeviceType
import mega.android.core.ui.theme.devicetype.LocalDeviceType
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.login.model.LoginState
import mega.privacy.android.app.presentation.login.model.MultiFactorAuthState
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.resources.R as sharedR

/**
 * New Two Factor Authentication screen.
 * @param state LoginState object.
 * @param on2FAChanged Callback to notify the 2FA changed.
 * @param onLostAuthenticatorDevice Callback to notify the user lost the authenticator device.
 * @param modifier Modifier.
 */
@Composable
fun NewTwoFactorAuthentication(
    state: LoginState,
    on2FAChanged: (String) -> Unit,
    onLostAuthenticatorDevice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val request = remember { FocusRequester() }
    var code by rememberSaveable { mutableStateOf("") }
    val softKeyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        request.requestFocus()
        softKeyboard?.show()
    }
    val orientation = LocalConfiguration.current.orientation
    val isTablet = LocalDeviceType.current == DeviceType.Tablet
    val isPhoneLandscape =
        orientation == Configuration.ORIENTATION_LANDSCAPE && !isTablet
    val contentModifier = if (isTablet || isPhoneLandscape) {
        Modifier
            .fillMaxHeight()
            .width(tabletScreenWidth(orientation))
    } else {
        Modifier
            .padding(horizontal = LocalSpacing.current.x16)
            .wrapContentHeight()
    }
    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = contentModifier) {
                MegaText3(
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
                    isCodeCorrect = state.multiFactorAuthState?.takeIf {
                        it != MultiFactorAuthState.Checking
                                && it != MultiFactorAuthState.Fixed
                    }?.let { it == MultiFactorAuthState.Passed },
                    errorText = stringResource(sharedR.string.multi_factor_auth_login_verification_input_error_text),
                    onValueChange = {
                        code = it
                        on2FAChanged(code)
                        if (code.length == DEFAULT_VERIFICATION_INPUT_LENGTH) {
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
        }

        if (state.multiFactorAuthState == MultiFactorAuthState.Checking) {
            LargeHUD(
                modifier = Modifier
                    .testTag(TWO_FA_PROGRESS_TEST_TAG)
                    .fillMaxSize()
                    .align(Alignment.Center),
            )
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
                themeMode = ThemeMode.System,
                multiFactorAuthState = MultiFactorAuthState.Checking
            ),
            on2FAChanged = {},
            onLostAuthenticatorDevice = {}
        )
    }
}