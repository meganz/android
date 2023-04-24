package mega.privacy.android.app.presentation.verification.view

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.verification.model.SMSVerificationUIState
import mega.privacy.android.core.ui.controls.MegaSpannedText
import mega.privacy.android.core.ui.controls.MegaTextField
import mega.privacy.android.core.ui.model.SpanIndicator
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.blue_400_blue_200
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.core.ui.theme.extensions.white_alpha_087_grey_alpha_087

/**
 * Test tag not now button
 */
const val NOT_NOW_BUTTON_TEST_TAG = "not_now_button_tag"

/**
 * Test tag next button
 */
const val NEXT_BUTTON_TEST_TAG = "next_button_button_tag"

/**
 * Test tag logout text button
 */
const val LOGOUT_BUTTON_TEST_TAG = "log_out_button_button_tag"

/**
 * Compose View of SMSVerification
 */
@Composable
fun SMSVerificationView(
    state: SMSVerificationUIState,
    onRegionSelection: () -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onNotNowClicked: () -> Unit,
    onNextClicked: () -> Unit,
    onLogout: () -> Unit,
    onSMSCodeSent: () -> Unit,
    onConsumeSMSCodeSentFinishedEvent: () -> Unit,
) {
    val systemUiController = rememberSystemUiController()
    systemUiController.setStatusBarColor(
        color = MaterialTheme.colors.blue_400_blue_200,
    )
    LaunchedEffect(
        state.isVerificationCodeSent
    ) {
        if (state.isVerificationCodeSent) {
            onSMSCodeSent()
            onConsumeSMSCodeSentFinishedEvent()
        }
    }

    var shouldShowLogoutDialog by remember {
        mutableStateOf(false)
    }
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxHeight()
            .background(color = MaterialTheme.colors.primary)
    ) {
        ConfirmLogoutDialog(
            shouldShowLogoutDialog,
            onDismiss = { shouldShowLogoutDialog = false },
            onConfirm = {
                shouldShowLogoutDialog = false
                onLogout()
            },
        )
        HeaderView(state)
        InfoView(state)
        RegionSelectionView(state, onRegionSelection)
        MobileNumberInputView(state, onPhoneNumberChange, onNextClicked)
        ActionButtonView(
            state,
            onNotNowClicked,
            onNextClicked,
            onLogout = {
                shouldShowLogoutDialog = true
            },
        )
    }
}

@Composable
private fun ColumnScope.ActionButtonView(
    state: SMSVerificationUIState,
    onNotNowClicked: () -> Unit,
    onNextClicked: () -> Unit,
    onLogout: () -> Unit,
) {
    Row(modifier = Modifier.padding(vertical = 24.dp)) {
        Spacer(Modifier.weight(1f))
        if (state.isUserLocked.not()) {
            TextButton(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .testTag(NOT_NOW_BUTTON_TEST_TAG),
                onClick = {
                    onNotNowClicked()
                }) {
                Text(
                    text = stringResource(id = R.string.verify_account_not_now_button),
                    style = MaterialTheme.typography.button,
                    color = MaterialTheme.colors.secondary
                )
            }
        }
        Button(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .testTag(NEXT_BUTTON_TEST_TAG),
            onClick = onNextClicked,
            enabled = state.isNextEnabled,
            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary)
        ) {
            Text(
                text = stringResource(id = R.string.general_next),
                style = MaterialTheme.typography.button,
                color = MaterialTheme.colors.primary
            )
        }
    }
    if (state.isUserLocked) {
        MegaSpannedText(
            modifier = Modifier
                .align(alignment = Alignment.CenterHorizontally)
                .testTag(LOGOUT_BUTTON_TEST_TAG)
                .clickable(onClick = onLogout),
            value = stringResource(id = R.string.sms_logout),
            baseStyle = MaterialTheme.typography.caption.copy(
                lineHeight = 1.5.em,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onPrimary
            ),
            styles = hashMapOf(
                SpanIndicator('A') to SpanStyle(color = MaterialTheme.colors.secondary)
            )
        )
    }
}

@Composable
private fun InfoView(uiState: SMSVerificationUIState) {
    Text(
        text = uiState.infoText,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 24.dp),
        color = MaterialTheme.colors.textColorSecondary,
        style = MaterialTheme.typography.subtitle1
    )
}

@Composable
private fun HeaderView(uiState: SMSVerificationUIState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.blue_400_blue_200),
    ) {
        Box {
            Text(
                text = uiState.headerText,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(vertical = 44.dp, horizontal = 16.dp),
                color = MaterialTheme.colors.white_alpha_087_grey_alpha_087,
                style = MaterialTheme.typography.h6,
            )
        }
        Image(
            painter = painterResource(id = R.drawable.il_verify_phone_big),
            contentDescription = "Verification Phone Big",
        )
    }
}

@Composable
private fun RegionSelectionView(state: SMSVerificationUIState, onRegionSelection: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable {
                onRegionSelection()
            }
            .padding(start = 16.dp, top = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            modifier = Modifier
                .padding(start = 14.dp, end = 2.dp),
            painter = painterResource(id = R.drawable.ic_country),
            contentDescription = "Country",
            colorFilter = ColorFilter.tint(MaterialTheme.colors.textColorSecondary)
        )
        Box(modifier = Modifier.height(56.dp)) {
            if (state.isCountryCodeValid) {
                Text(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 8.dp),
                    text = stringResource(id = R.string.sms_region_label),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.secondary,
                    style = MaterialTheme.typography.body2
                )
            }
            Text(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(start = 8.dp),
                text = state.countryCodeText,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.textColorSecondary,
                style = MaterialTheme.typography.body1
            )
        }
        Spacer(Modifier.weight(1f))
        Image(
            modifier = Modifier
                .padding(end = 16.dp),
            painter = painterResource(id = R.drawable.ic_g_arrow_next),
            contentDescription = "Arrow Next",
        )
    }
    Divider(
        modifier = Modifier
            .padding(horizontal = 16.dp),
        color = if (state.isCountryCodeValid) MaterialTheme.colors.grey_alpha_012_white_alpha_012 else MaterialTheme.colors.error,
    )
    if (state.isCountryCodeValid.not()) {
        Text(
            modifier = Modifier
                .padding(start = 24.dp, top = 8.dp),
            text = stringResource(id = R.string.verify_account_invalid_country_code),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.error,
            style = MaterialTheme.typography.body1
        )
    }
}

@Composable
private fun MobileNumberInputView(
    state: SMSVerificationUIState,
    onPhoneNumberChange: (phoneNumber: String) -> Unit,
    onNextClicked: () -> Unit,
) {
    Text(
        modifier = Modifier
            .padding(start = 64.dp, top = 8.dp),
        text = if (state.phoneNumber.isNotEmpty()) {
            stringResource(id = R.string.verify_account_phone_number_placeholder)
        } else {
            ""
        },
        textAlign = TextAlign.Start,
        color = if (state.isPhoneNumberValid) MaterialTheme.colors.secondary else MaterialTheme.colors.error,
        style = MaterialTheme.typography.body2
    )
    Row(
        modifier = Modifier
            .height(56.dp)
            .padding(start = 16.dp, end = 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val focusManager = LocalFocusManager.current
        MegaTextField(
            value = state.phoneNumber,
            singleLine = true,
            showBorder = false,
            leadingIcon = {
                Image(
                    painter = painterResource(id = R.drawable.ic_verify_phone),
                    contentDescription = "Phone",
                    colorFilter = ColorFilter.tint(MaterialTheme.colors.textColorSecondary)
                )
            },
            placeholder =
            {
                Text(
                    text = stringResource(id = R.string.verify_account_phone_number_placeholder),
                    color = MaterialTheme.colors.textColorSecondary,
                ).takeIf { (state.phoneNumber.isEmpty()) }
            },
            trailingIcon =
            {
                if (!state.isPhoneNumberValid) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_input_warning),
                        contentDescription = "Input Error",
                        colorFilter = ColorFilter.tint(MaterialTheme.colors.error)
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(0.dp, Color.Transparent))
                .background(color = MaterialTheme.colors.primary),
            onValueChange = {
                onPhoneNumberChange(it)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done // Set IME action label
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onNextClicked()
                }
            ),
            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colors.textColorSecondary)
        )
    }
    Divider(
        modifier = Modifier
            .padding(horizontal = 16.dp),
        color = if (state.isPhoneNumberValid) MaterialTheme.colors.grey_alpha_012_white_alpha_012 else MaterialTheme.colors.error,
    )
    if (!state.isPhoneNumberValid) {
        Text(
            modifier = Modifier
                .padding(start = 24.dp, top = 8.dp),
            text = stringResource(id = R.string.verify_account_invalid_phone_number),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.error,
            style = MaterialTheme.typography.body1
        )
    }
    if (state.phoneNumberErrorText.isNotEmpty() && state.isPhoneNumberValid) {
        Text(
            modifier = Modifier
                .padding(start = 24.dp, top = 8.dp),
            text = state.phoneNumberErrorText,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.error,
            style = MaterialTheme.typography.body1
        )
    }
}

@Composable
private fun ConfirmLogoutDialog(
    isLogout: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (isLogout) {
        LogoutDialog(
            title = stringResource(id = R.string.confirm_logout_from_sms_verification),
            confirmButtonLabel = stringResource(R.string.general_positive_button),
            dismissButtonLabel = stringResource(R.string.general_negative_button),
            shouldDismissOnBackPress = true,
            shouldDismissOnClickOutside = true,
            onDismiss = onDismiss,
            onConfirmButton = onConfirm
        )
    }
}


/**
 * Preview of SMSVerificationView
 */
@Preview
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "DarkSMSVerificationViewPreview"
)
@Composable
private fun SMSVerificationViewPreview() {
    val uiState = SMSVerificationUIState(
        selectedCountryCode = "NZ",
        selectedCountryName = "New Zealand",
        selectedDialCode = "+64",
        countryCodeText = "(NZ+64)New Zealand",
        isUserLocked = true,
    )
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        SMSVerificationView(
            state = uiState,
            {}, {}, {}, {}, {}, {}, {}
        )
    }
}
