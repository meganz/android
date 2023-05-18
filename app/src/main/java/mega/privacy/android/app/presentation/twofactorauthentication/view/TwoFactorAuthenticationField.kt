package mega.privacy.android.app.presentation.twofactorauthentication.view

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TextFieldDefaults.indicatorLine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.twofactorauthentication.extensions.getTwoFactorAuthentication
import mega.privacy.android.app.presentation.twofactorauthentication.extensions.getUpdatedTwoFactorAuthentication
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.body2medium
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_038

/**
 * View for typing 2FA pin.
 *
 * @param twoFAPin               Typed 2FA pin.
 * @param on2FAPinChanged        Action when 2FA pin changes.
 * @param on2FAChanged           Action when the entire 2FA changes: Paste option.
 * @param isError                True if the 2FA pin was typed but is not the correct one.
 * @param requestFocus           [StateEvent]
 * @param onRequestFocusConsumed Action when request focus has been consumed.
 * @param modifier               [Modifier].
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TwoFactorAuthenticationField(
    twoFAPin: List<String>,
    on2FAPinChanged: (String, Int) -> Unit,
    on2FAChanged: (String) -> Unit,
    isError: Boolean,
    requestFocus: StateEvent,
    onRequestFocusConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) = Row(modifier = modifier.testTag(TWO_FACTOR_AUTHENTICATION_TEST_TAG)) {
    val (
        focusRequesterFirstPin,
        focusRequesterSecondPin,
        focusRequesterThirdPin,
        focusRequesterFourthPin,
        focusRequesterFifthPin,
        focusRequesterSixthPin,
    ) = FocusRequester.createRefs()

    Row(horizontalArrangement = Arrangement.SpaceBetween) {
        PinTwoFactorAuthentication(
            pin = twoFAPin[FIRST_PIN],
            isError = isError,
            onPinChanged = { on2FAPinChanged(it, FIRST_PIN) },
            onPinRemoved = { on2FAPinChanged("", FIRST_PIN) },
            on2FAChanged = on2FAChanged,
            modifier = Modifier
                .padding(end = 8.dp)
                .focusRequester(focusRequesterFirstPin)
                .focusProperties {
                    next = focusRequesterSecondPin
                    previous = focusRequesterFirstPin
                }
                .testTag(FIRST_PIN_TEST_TAG)
        )
        PinTwoFactorAuthentication(
            pin = twoFAPin[SECOND_PIN],
            isError = isError,
            onPinChanged = { on2FAPinChanged(it, SECOND_PIN) },
            onPinRemoved = { on2FAPinChanged("", FIRST_PIN) },
            on2FAChanged = on2FAChanged,
            modifier = Modifier
                .padding(end = 8.dp)
                .focusRequester(focusRequesterSecondPin)
                .focusProperties {
                    next = focusRequesterThirdPin
                    previous = focusRequesterFirstPin
                }
                .testTag(SECOND_PIN_TEST_TAG)
        )
        PinTwoFactorAuthentication(
            pin = twoFAPin[THIRD_PIN],
            isError = isError,
            onPinChanged = { on2FAPinChanged(it, THIRD_PIN) },
            onPinRemoved = { on2FAPinChanged("", SECOND_PIN) },
            on2FAChanged = on2FAChanged,
            modifier = Modifier
                .padding(end = 25.dp)
                .focusRequester(focusRequesterThirdPin)
                .focusProperties {
                    next = focusRequesterFourthPin
                    previous = focusRequesterSecondPin
                }
                .testTag(THIRD_PIN_TEST_TAG)
        )
        PinTwoFactorAuthentication(
            pin = twoFAPin[FOURTH_PIN],
            isError = isError,
            onPinChanged = { on2FAPinChanged(it, FOURTH_PIN) },
            onPinRemoved = { on2FAPinChanged("", THIRD_PIN) },
            on2FAChanged = on2FAChanged,
            modifier = Modifier
                .padding(end = 8.dp)
                .focusRequester(focusRequesterFourthPin)
                .focusProperties {
                    next = focusRequesterFifthPin
                    previous = focusRequesterThirdPin
                }
                .testTag(FOURTH_PIN_TEST_TAG)
        )
        PinTwoFactorAuthentication(
            pin = twoFAPin[FIFTH_PIN],
            isError = isError,
            onPinChanged = { on2FAPinChanged(it, FIFTH_PIN) },
            onPinRemoved = { on2FAPinChanged("", FOURTH_PIN) },
            on2FAChanged = on2FAChanged,
            modifier = Modifier
                .padding(end = 8.dp)
                .focusRequester(focusRequesterFifthPin)
                .focusProperties {
                    next = focusRequesterSixthPin
                    previous = focusRequesterFourthPin
                }
                .testTag(FIFTH_PIN_TEST_TAG)
        )
        PinTwoFactorAuthentication(
            pin = twoFAPin[SIXTH_PIN],
            isError = isError,
            onPinChanged = { on2FAPinChanged(it, SIXTH_PIN) },
            onPinRemoved = { on2FAPinChanged("", FIFTH_PIN) },
            on2FAChanged = on2FAChanged,
            modifier = Modifier
                .padding(end = 8.dp)
                .focusRequester(focusRequesterSixthPin)
                .focusProperties {
                    next = focusRequesterSixthPin
                    previous = focusRequesterFifthPin
                }
                .testTag(SIXTH_PIN_TEST_TAG)
        )
    }

    EventEffect(event = requestFocus, onConsumed = onRequestFocusConsumed) {
        focusRequesterFirstPin.requestFocus()
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun PinTwoFactorAuthentication(
    pin: String,
    isError: Boolean,
    onPinChanged: (String) -> Unit,
    onPinRemoved: () -> Unit,
    on2FAChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val colors = TextFieldDefaults.textFieldColors(
        textColor = MaterialTheme.colors.onPrimary,
        backgroundColor = Color.Transparent,
        cursorColor = MaterialTheme.colors.secondary,
        errorCursorColor = MaterialTheme.colors.error,
        errorIndicatorColor = MaterialTheme.colors.error,
        focusedIndicatorColor = MaterialTheme.colors.secondary,
        unfocusedIndicatorColor = MaterialTheme.colors.grey_alpha_012_white_alpha_038,
    )
    val customTextSelectionColors = TextSelectionColors(
        handleColor = MaterialTheme.colors.secondary,
        backgroundColor = MaterialTheme.colors.secondary
    )
    val focusManager = LocalFocusManager.current
    val direction = LocalLayoutDirection.current

    LaunchedEffect(
        key1 = pin,
    ) {
        if (pin.isNotEmpty()) {
            focusManager.moveFocus(focusDirection = FocusDirection.Next)
        }
    }

    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        BasicTextField(
            value = TextFieldValue(
                pin,
                if (direction == LayoutDirection.Ltr) TextRange(pin.length) else TextRange.Zero
            ),
            onValueChange = {
                with(it.text) {
                    if (pin != this && (isEmpty() || length == 1)) onPinChanged(this)
                    else if (length == NUMBER_PINS) on2FAChanged(this)
                }
            },
            modifier = modifier
                .background(colors.backgroundColor(true).value)
                .indicatorLine(true, isError, interactionSource, colors)
                .width(32.dp)
                .onKeyEvent {
                    if (pin.isEmpty() && it.key == Key.Backspace) {
                        focusManager.moveFocus(FocusDirection.Previous)
                        onPinRemoved()
                        true
                    } else {
                        false
                    }
                },
            textStyle = MaterialTheme.typography.body2medium
                .copy(
                    color = MaterialTheme.colors.onPrimary,
                    fontSize = 32.sp,
                    textAlign = TextAlign.Center
                ),
            cursorBrush = SolidColor(colors.cursorColor(isError).value),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
            interactionSource = interactionSource,
            singleLine = true,
        ) {
            TextFieldDefaults.TextFieldDecorationBox(
                value = pin,
                innerTextField = it,
                enabled = true,
                singleLine = true,
                interactionSource = interactionSource,
                visualTransformation = VisualTransformation.None,
                isError = isError,
                colors = colors,
                contentPadding = TextFieldDefaults.textFieldWithoutLabelPadding(
                    start = 0.dp,
                    top = 0.dp,
                    end = 0.dp,
                    bottom = 7.dp
                )
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewTwoFactorAuthenticationField() {
    var twoFAPin by remember { mutableStateOf(listOf("", "", "", "", "", "")) }
    var isError by remember { mutableStateOf(false) }
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        TwoFactorAuthenticationField(
            twoFAPin = twoFAPin,
            on2FAPinChanged = { pin, index ->
                twoFAPin = twoFAPin.getUpdatedTwoFactorAuthentication(pin = pin, index = index)
                isError = twoFAPin.none { it.isEmpty() }
            },
            on2FAChanged = { newTwoFA ->
                newTwoFA.getTwoFactorAuthentication()?.apply {
                    twoFAPin = this
                    isError = true
                }
            },
            isError = isError,
            requestFocus = consumed,
            onRequestFocusConsumed = {}
        )
    }
}

internal const val TWO_FACTOR_AUTHENTICATION_TEST_TAG = "TWO_FACTOR_AUTHENTICATION"
internal const val FIRST_PIN = 0
internal const val FIRST_PIN_TEST_TAG = "FIRST_PIN"
internal const val SECOND_PIN = 1
internal const val SECOND_PIN_TEST_TAG = "SECOND_PIN"
internal const val THIRD_PIN = 2
internal const val THIRD_PIN_TEST_TAG = "THIRD_PIN"
internal const val FOURTH_PIN = 3
internal const val FOURTH_PIN_TEST_TAG = "FOURTH_PIN"
internal const val FIFTH_PIN = 4
internal const val FIFTH_PIN_TEST_TAG = "FIFTH_PIN"
internal const val SIXTH_PIN = 5
internal const val SIXTH_PIN_TEST_TAG = "SIXTH_PIN"
internal const val NUMBER_PINS = 6