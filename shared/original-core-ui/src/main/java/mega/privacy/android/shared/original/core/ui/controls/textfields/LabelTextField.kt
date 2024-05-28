package mega.privacy.android.shared.original.core.ui.controls.textfields

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TextFieldDefaults.indicatorLine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.preview.TextFieldProvider
import mega.privacy.android.shared.original.core.ui.preview.TextFieldState
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.autofill

/**
 * Text field with label.
 *
 * @param onTextChange    Action required for notifying about text changes.
 * @param label           Label required for showing as placeholder if the field is empty and as label if not.
 * @param imeAction       [ImeAction]
 * @param keyboardActions [KeyboardActions]
 * @param modifier        [Modifier]
 * @param text            Typed text.
 * @param errorText       Error to show if any.
 * @param isEmail         True if the input text should be an email, false otherwise.
 * @param isAutoFocus     True if the input text should be focused automatically from the first time, false otherwise.
 */
@Composable
fun LabelTextField(
    onTextChange: (String) -> Unit,
    label: String,
    imeAction: ImeAction,
    keyboardActions: KeyboardActions,
    modifier: Modifier = Modifier,
    text: String = "",
    errorText: String? = null,
    isEmail: Boolean = false,
    isAutoFocus: Boolean = false,
) {
    // Holds the latest internal TextFieldValue state. We need to keep it to have the correct value
    // of the composition.
    var textFieldValueState by remember { mutableStateOf(TextFieldValue(text = text)) }
    // Holds the latest TextFieldValue that BasicTextField was recomposed with. We couldn't simply
    // pass `TextFieldValue(text = value)` to the LabelTextField because we need to preserve the
    // composition.
    val textFieldValue = textFieldValueState.copy(text = text)

    // Update the TextFieldValue state when either the selection range or the composition range changes
    SideEffect {
        if (
            textFieldValue.selection != textFieldValueState.selection ||
            textFieldValue.composition != textFieldValueState.composition
        ) {
            textFieldValueState = textFieldValue
        }
    }

    // Last String value that either text field was recomposed with or updated in the onValueChange
    // callback. We keep track of it to prevent calling onValueChange(String) for same String when
    // LabelTextField's onValueChange is called multiple times without recomposition in between.
    // E.g. when we change the theme from dark to light or vice versa.
    var lastTextValue by remember(text) { mutableStateOf(text) }

    LabelTextField(
        onTextChange = { newTextFieldValueState ->
            textFieldValueState = newTextFieldValueState

            val stringChangedSinceLastInvocation = lastTextValue != newTextFieldValueState.text
            lastTextValue = newTextFieldValueState.text

            if (stringChangedSinceLastInvocation) {
                onTextChange(newTextFieldValueState.text)
            }
        },
        label = label,
        imeAction = imeAction,
        keyboardActions = keyboardActions,
        modifier = modifier,
        value = textFieldValue,
        errorText = errorText,
        isEmail = isEmail,
        isAutoFocus = isAutoFocus
    )
}

/**
 * Text field with label.
 *
 * @param onTextChange    Action required for notifying about text changes in [TextFieldValue].
 * @param label           Label required for showing as placeholder if the field is empty and as label if not.
 * @param imeAction       [ImeAction]
 * @param keyboardActions [KeyboardActions]
 * @param modifier        [Modifier]
 * @param value           The [androidx.compose.ui.text.input.TextFieldValue] to be shown in the [LabelTextField].
 * @param errorText       Error to show if any.
 * @param isEmail         True if the input text should be an email, false otherwise.
 * @param isAutoFocus     True if the input text should be focused automatically from the first time, false otherwise.
 */
@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
fun LabelTextField(
    onTextChange: (TextFieldValue) -> Unit,
    label: String,
    imeAction: ImeAction,
    keyboardActions: KeyboardActions,
    modifier: Modifier = Modifier,
    value: TextFieldValue = TextFieldValue(text = ""),
    errorText: String? = null,
    isEmail: Boolean = false,
    isAutoFocus: Boolean = false,
) = Column(modifier = modifier) {
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val isError = errorText != null
    val colors = TextFieldDefaults.textFieldColors(
        textColor = MegaOriginalTheme.colors.text.primary,
        backgroundColor = MegaOriginalTheme.colors.background.pageBackground,
        cursorColor = MegaOriginalTheme.colors.border.strongSelected,
        errorCursorColor = MegaOriginalTheme.colors.text.error,
        errorIndicatorColor = MegaOriginalTheme.colors.text.error,
        focusedIndicatorColor = MegaOriginalTheme.colors.border.disabled,
        unfocusedIndicatorColor = MegaOriginalTheme.colors.border.disabled,
        focusedLabelColor = MegaOriginalTheme.colors.text.accent,
        unfocusedLabelColor = MegaOriginalTheme.colors.text.placeholder,
        errorLabelColor = MegaOriginalTheme.colors.text.error,
    )
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    val customTextSelectionColors = TextSelectionColors(
        handleColor = MegaOriginalTheme.colors.border.strongSelected,
        backgroundColor = MegaOriginalTheme.colors.border.strongSelected
    )

    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        BasicTextField(
            value = value,
            onValueChange = onTextChange,
            modifier = Modifier
                .background(Color.Transparent)
                .indicatorLine(true, isError, interactionSource, colors)
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused }
                .autofill(
                    autofillTypes = if (isEmail) listOf(AutofillType.EmailAddress) else emptyList(),
                    onAutoFilled = { onTextChange(value.copy(text = it)) }
                ),
            textStyle = MaterialTheme.typography.subtitle1.copy(color = MegaOriginalTheme.colors.text.primary),
            cursorBrush = SolidColor(colors.cursorColor(isError).value),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isEmail) KeyboardType.Email else KeyboardType.Text,
                imeAction = imeAction
            ),
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            singleLine = true,
        ) {
            TextFieldDefaults.TextFieldDecorationBox(
                value = value.text,
                innerTextField = it,
                enabled = true,
                singleLine = true,
                interactionSource = interactionSource,
                visualTransformation = VisualTransformation.None,
                isError = isError,
                label = {
                    Text(
                        text = label,
                        modifier = Modifier.padding(bottom = if (isFocused) 6.dp else 0.dp),
                        style = when {
                            isError && isFocused -> {
                                MaterialTheme.typography.caption.copy(
                                    color = MegaOriginalTheme.colors.text.error
                                )
                            }

                            isError && value.text.isEmpty() -> {
                                MaterialTheme.typography.body1.copy(
                                    color = MegaOriginalTheme.colors.text.error
                                )
                            }

                            isFocused -> {
                                MaterialTheme.typography.caption.copy(
                                    color = MegaOriginalTheme.colors.text.accent
                                )
                            }

                            value.text.isNotEmpty() -> {
                                MaterialTheme.typography.caption.copy(
                                    color = MegaOriginalTheme.colors.text.placeholder
                                )
                            }

                            else -> {
                                MaterialTheme.typography.body1.copy(
                                    color = MegaOriginalTheme.colors.text.placeholder
                                )
                            }
                        }
                    )
                },
                colors = colors,
                contentPadding = TextFieldDefaults.textFieldWithoutLabelPadding(
                    start = 0.dp, bottom = 7.dp
                )
            )
        }

        LaunchedEffect(Unit) {
            if (isAutoFocus) {
                focusRequester.requestFocus()
            }
        }
    }

    errorText?.apply { ErrorTextTextField(errorText = this) }
}

@CombinedThemePreviews
@Composable
private fun PreviewLabelTextField(
    @PreviewParameter(TextFieldProvider::class) state: TextFieldState,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        var text by remember { mutableStateOf(state.text) }

        LabelTextField(
            onTextChange = { text = it },
            label = "Label",
            imeAction = ImeAction.Default,
            keyboardActions = KeyboardActions(),
            text = state.text,
            errorText = state.error,
        )
    }
}