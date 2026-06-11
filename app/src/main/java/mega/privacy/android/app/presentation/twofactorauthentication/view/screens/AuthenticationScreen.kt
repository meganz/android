package mega.privacy.android.app.presentation.twofactorauthentication.view.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.indicators.LargeHUD
import mega.android.core.ui.components.inputfields.VerificationTextInputField
import mega.android.core.ui.extensions.safeRequestFocus
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.login.view.TWO_FA_PROGRESS_TEST_TAG
import mega.privacy.android.app.presentation.twofactorauthentication.model.AuthenticationState
import mega.privacy.android.app.presentation.twofactorauthentication.model.TwoFactorAuthenticationUIState
import mega.privacy.android.shared.resources.R as sharedR

internal const val AUTHENTICATION_SCREEN_PIN_FIELD_TAG = "authentication_screen:pin_field"

@Composable
internal fun AuthenticationScreen(
    uiState: TwoFactorAuthenticationUIState,
    on2FAChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isError = uiState.authenticationState == AuthenticationState.Failed
    val isChecking = uiState.authenticationState == AuthenticationState.Checking
    val spacing = LocalSpacing.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.safeRequestFocus()
        keyboardController?.show()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.x16),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top,
        ) {
            MegaText(
                modifier = Modifier.padding(top = spacing.x24, bottom = spacing.x24),
                text = stringResource(id = sharedR.string.multi_factor_auth_login_verification_content),
                textColor = TextColor.Secondary,
                style = AppTheme.typography.bodyLarge,
            )
            VerificationTextInputField(
                modifier = Modifier
                    .testTag(AUTHENTICATION_SCREEN_PIN_FIELD_TAG)
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                value = uiState.twoFAPin,
                isCodeCorrect = if (isError) false else null,
                errorText = stringResource(id = R.string.pin_error_2fa),
                onValueChange = on2FAChanged,
            )
        }
        if (isChecking) {
            LargeHUD(
                modifier = Modifier
                    .testTag(TWO_FA_PROGRESS_TEST_TAG)
                    .align(Alignment.Center),
            )
        }
    }
}
