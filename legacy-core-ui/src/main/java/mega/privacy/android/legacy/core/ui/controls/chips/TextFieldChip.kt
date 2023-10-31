package mega.privacy.android.legacy.core.ui.controls.chips

import android.graphics.Rect
import android.view.ViewTreeObserver
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.model.Keyboard
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.preview.TextFieldProvider
import mega.privacy.android.core.ui.preview.TextFieldState
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_038_white_alpha_038
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary

/**
 * Text field Chip
 *
 * @param text              The text in TextButton
 * @param onTextChange      Lambda to receive clicks on this button
 * @param onFocusChange     Detects when is focus
 * @param modifier          [Modifier]
 * @param isDisabled        True, if it's disabled. False, if not
 * @param readOnly          True if it is not a edit text view.
 * @param isSmall           True it's small box. False if not.
 */
@Composable
fun TextFieldChip(
    text: String,
    onTextChange: (String) -> Unit,
    onFocusChange: () -> Unit,
    modifier: Modifier = Modifier,
    isDisabled: Boolean = false,
    readOnly: Boolean = false,
    isSmall: Boolean = true,
) = Box {

    var isFocused by remember { mutableStateOf(true) }
    val isKeyboardOpen by keyboardAsState()

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    if (isKeyboardOpen == Keyboard.Closed) {
        focusManager.clearFocus()
    }

    val customTextSelectionColors = TextSelectionColors(
        handleColor = MaterialTheme.colors.secondary,
        backgroundColor = MaterialTheme.colors.secondary
    )

    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            readOnly = readOnly,
            modifier = modifier
                .focusRequester(focusRequester)
                .onFocusChanged {
                    if (isFocused != it.isFocused) {
                        isFocused = it.isFocused
                        if (isFocused) {
                            onFocusChange()
                        }
                    }
                }
                .testTag(TEST_TAG_TEXT_FIELD_CHIP),
            cursorBrush = SolidColor(MaterialTheme.colors.secondary),
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            textStyle = MaterialTheme.typography.button.copy(
                textAlign = TextAlign.Center,
                color =
                if (isDisabled)
                    MaterialTheme.colors.grey_alpha_038_white_alpha_038
                else
                    MaterialTheme.colors.onPrimary
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                }),
            decorationBox = { innerTextField ->
                Box(
                    modifier = if (isSmall) modifier
                        .width(IntrinsicSize.Min)
                        .height(IntrinsicSize.Min)
                        .widthIn(min = 40.dp)
                        .heightIn(min = 32.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colors.textColorSecondary,
                            shape = RoundedCornerShape(size = 8.dp)
                        )
                        .padding(top = 6.dp, bottom = 6.dp)
                    else modifier
                        .widthIn(min = 40.dp)
                        .heightIn(min = 32.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colors.textColorSecondary,
                            shape = RoundedCornerShape(size = 8.dp)
                        )
                        .padding(top = 6.dp, bottom = 6.dp),
                ) {
                    innerTextField()
                }
            }
        )
    }
}

/**
 * Control keyboard state
 */
@Composable
fun keyboardAsState(): State<Keyboard> {
    val keyboardState = remember { mutableStateOf(Keyboard.Closed) }
    val view = LocalView.current
    DisposableEffect(view) {
        val onGlobalListener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            view.getWindowVisibleDisplayFrame(rect)
            val screenHeight = view.rootView.height
            val keypadHeight = screenHeight - rect.bottom
            keyboardState.value =
                if (keypadHeight > screenHeight * 0.15) Keyboard.Opened else Keyboard.Closed
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(onGlobalListener)

        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalListener)
        }
    }

    return keyboardState
}

@CombinedThemePreviews
@Composable
private fun PreviewTextFieldChip(
    @PreviewParameter(TextFieldProvider::class) state: TextFieldState,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        var text by remember { mutableStateOf("8 Sep 2022") }

        TextFieldChip(
            onTextChange = { text = it },
            text = text,
            modifier = Modifier,
            isDisabled = true,
            isSmall = false,
            onFocusChange = { }
        )
    }
}

internal const val TEST_TAG_TEXT_FIELD_CHIP = "testTagTextFieldChip"
