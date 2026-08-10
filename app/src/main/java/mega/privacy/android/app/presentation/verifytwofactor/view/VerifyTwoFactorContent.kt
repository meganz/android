package mega.privacy.android.app.presentation.verifytwofactor.view

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import de.palm.composestateevents.StateEventWithContentTriggered
import mega.android.core.ui.components.MegaClickableText
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.indicators.LargeHUD
import mega.android.core.ui.components.inputfields.VerificationTextInputField
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.devicetype.DeviceType
import mega.android.core.ui.theme.devicetype.LocalDeviceType
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.login.view.tabletScreenWidth
import mega.privacy.android.app.presentation.verifytwofactor.model.VerifyTwoFactorUiState
import mega.privacy.android.app.utils.Constants.CANCEL_ACCOUNT_2FA
import mega.privacy.android.app.utils.Constants.CHANGE_MAIL_2FA
import mega.privacy.android.app.utils.Constants.CHANGE_PASSWORD_2FA
import mega.privacy.android.app.utils.Constants.DISABLE_2FA
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold

internal const val VERIFY_2FA_SCREEN_TAG = "verify_two_factor_screen"
internal const val VERIFY_2FA_PIN_FIELD_TAG = "verify_two_factor_screen:pin_field"
internal const val VERIFY_2FA_LOST_DEVICE_TAG = "verify_two_factor_screen:lost_device"
internal const val VERIFY_2FA_PROGRESS_TAG = "verify_two_factor_screen:progress"

/**
 * Stateless body of the verify-2FA screen.
 *
 * @param state Current UI state.
 * @param onBack Called when the toolbar back button is pressed.
 * @param onPinChanged Called as the user types each digit; full 6-char value passed through.
 * @param onLostAuthenticatorDevice Called when the recovery link is tapped.
 * @param onResultDismissed Called after the result dialog is dismissed (drives `finish()`).
 * @param modifier Modifier applied to the root scaffold.
 */
@Composable
internal fun VerifyTwoFactorContent(
    state: VerifyTwoFactorUiState,
    onBack: () -> Unit,
    onPinChanged: (String) -> Unit,
    onLostAuthenticatorDevice: () -> Unit,
    onResultDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaScaffold(
        modifier = modifier.testTag(VERIFY_2FA_SCREEN_TAG),
        topBar = {
            MegaAppBar(
                appBarType = AppBarType.BACK_NAVIGATION,
                title = stringResource(R.string.settings_2fa),
                subtitle = subtitleFor(state.verifyType),
                enabled = state.isBackEnabled,
                onNavigationPressed = onBack,
            )
        },
    ) { paddingValues ->
        VerifyTwoFactorBody(
            state = state,
            paddingValues = paddingValues,
            onPinChanged = onPinChanged,
            onLostAuthenticatorDevice = onLostAuthenticatorDevice,
        )
    }

    val triggered = state.resultEvent as? StateEventWithContentTriggered
    if (triggered != null) {
        VerifyTwoFactorResultDialog(
            result = triggered.content,
            onDismiss = onResultDismissed,
        )
    }
}

@Composable
private fun VerifyTwoFactorBody(
    state: VerifyTwoFactorUiState,
    paddingValues: PaddingValues,
    onPinChanged: (String) -> Unit,
    onLostAuthenticatorDevice: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val orientation = LocalConfiguration.current.orientation
    val isTablet = LocalDeviceType.current == DeviceType.Tablet
    val isPhoneLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE && !isTablet
    val contentModifier = if (isTablet || isPhoneLandscape) {
        Modifier.width(tabletScreenWidth(orientation))
    } else {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.x16)
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = contentModifier,
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start,
            ) {
                MegaText(
                    modifier = Modifier.padding(vertical = spacing.x24),
                    text = stringResource(R.string.explain_confirm_2fa),
                    textColor = TextColor.Secondary,
                    style = AppTheme.typography.bodyLarge,
                )
                VerificationTextInputField(
                    modifier = Modifier
                        .testTag(VERIFY_2FA_PIN_FIELD_TAG)
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    value = state.pin,
                    isCodeCorrect = if (state.isPinError) false else null,
                    cursorIndex = if (state.isPinError) 0 else -1,
                    errorText = stringResource(R.string.pin_error_2fa),
                    onValueChange = onPinChanged,
                )
                MegaClickableText(
                    modifier = Modifier
                        .testTag(VERIFY_2FA_LOST_DEVICE_TAG)
                        .padding(top = spacing.x24),
                    text = stringResource(R.string.lost_your_authenticator_device),
                    style = AppTheme.typography.bodyMedium,
                    onClick = onLostAuthenticatorDevice,
                )
            }
        }
        if (state.isLoading) {
            LargeHUD(
                modifier = Modifier
                    .testTag(VERIFY_2FA_PROGRESS_TAG)
                    .align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun subtitleFor(verifyType: Int): String? {
    val resId = when (verifyType) {
        CANCEL_ACCOUNT_2FA -> R.string.verify_2fa_subtitle_delete_account
        CHANGE_MAIL_2FA -> R.string.verify_2fa_subtitle_change_email
        CHANGE_PASSWORD_2FA -> R.string.verify_2fa_subtitle_change_password
        DISABLE_2FA -> R.string.verify_2fa_subtitle_diable_2fa
        else -> return null
    }
    return stringResource(resId)
}
