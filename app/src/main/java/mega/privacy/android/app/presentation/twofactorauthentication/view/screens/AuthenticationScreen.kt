package mega.privacy.android.app.presentation.twofactorauthentication.view.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.login.view.TWO_FA_PROGRESS_TEST_TAG
import mega.privacy.android.app.presentation.twofactorauthentication.model.AuthenticationState
import mega.privacy.android.app.presentation.twofactorauthentication.model.TwoFactorAuthenticationUIState
import mega.privacy.android.app.presentation.twofactorauthentication.view.TwoFactorAuthenticationField
import mega.privacy.android.core.ui.controls.progressindicator.MegaCircularProgressIndicator
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary

@Composable
internal fun AuthenticationScreen(
    uiState: TwoFactorAuthenticationUIState,
    on2FAPinChanged: (String, Int) -> Unit,
    on2FAChanged: (String) -> Unit,
    onFirstTime2FAConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) = Box(
    modifier = modifier
        .fillMaxSize()
) {
    val scrollState = rememberScrollState()
    val isChecking2FA = uiState.authenticationState == AuthenticationState.Checking
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val isError = uiState.authenticationState == AuthenticationState.Failed

        Text(
            text = stringResource(id = R.string.explain_confirm_2fa),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 40.dp),
            style = MaterialTheme.typography.subtitle1.copy(color = MaterialTheme.colors.textColorSecondary),
            textAlign = TextAlign.Center
        )
        TwoFactorAuthenticationField(
            twoFAPin = uiState.twoFAPin,
            isError = isError,
            on2FAPinChanged = on2FAPinChanged,
            on2FAChanged = on2FAChanged,
            requestFocus = uiState.isFirstTime2FA,
            onRequestFocusConsumed = onFirstTime2FAConsumed
        )
        if (isError) {
            Text(
                text = stringResource(id = R.string.pin_error_2fa),
                modifier = Modifier.padding(start = 10.dp, top = 18.dp, end = 10.dp),
                style = MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.error)
            )
        }
    }
    if (isChecking2FA) {
        MegaCircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.Center)
                .testTag(TWO_FA_PROGRESS_TEST_TAG)
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewAuthenticationScreen() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        AuthenticationScreen(
            uiState = TwoFactorAuthenticationUIState(),
            on2FAPinChanged = { _, _ -> },
            on2FAChanged = {},
            onFirstTime2FAConsumed = { })
    }
}