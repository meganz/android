package mega.privacy.android.core.ui.controls.textfields

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.preview.TextFieldProvider
import mega.privacy.android.core.ui.preview.TextFieldState
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.extensions.autofill

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
 */
@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
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
) = Column(modifier = modifier) {
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val isError = errorText != null
    val colors = TextFieldDefaults.textFieldColors(
        textColor = MegaTheme.colors.text.primary,
        backgroundColor = MegaTheme.colors.background.pageBackground,
        cursorColor = MegaTheme.colors.border.strongSelected,
        errorCursorColor = MegaTheme.colors.text.error,
        errorIndicatorColor = MegaTheme.colors.text.error,
        focusedIndicatorColor = MegaTheme.colors.border.disabled,
        unfocusedIndicatorColor = MegaTheme.colors.border.disabled,
        focusedLabelColor = MegaTheme.colors.text.accent,
        unfocusedLabelColor = MegaTheme.colors.text.placeholder,
        errorLabelColor = MegaTheme.colors.text.error,
    )
    var isFocused by remember { mutableStateOf(false) }
    val customTextSelectionColors = TextSelectionColors(
        handleColor = MegaTheme.colors.border.strongSelected,
        backgroundColor = MegaTheme.colors.border.strongSelected
    )

    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .background(Color.Transparent)
                .indicatorLine(true, isError, interactionSource, colors)
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
                .autofill(
                    autofillTypes = if (isEmail) listOf(AutofillType.EmailAddress) else emptyList(),
                    onAutoFilled = onTextChange
                ),
            textStyle = MaterialTheme.typography.subtitle1.copy(color = MegaTheme.colors.text.primary),
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
                value = text,
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
                            isError && isFocused -> MaterialTheme.typography.caption.copy(color = MegaTheme.colors.text.error)
                            isError && text.isEmpty() -> MaterialTheme.typography.body1.copy(color = MegaTheme.colors.text.error)
                            isFocused -> MaterialTheme.typography.caption.copy(color = MegaTheme.colors.text.accent)
                            text.isNotEmpty() -> MaterialTheme.typography.caption.copy(color = MegaTheme.colors.text.placeholder)
                            else -> MaterialTheme.typography.body1.copy(color = MegaTheme.colors.text.placeholder)
                        }
                    )
                },
                colors = colors,
                contentPadding = TextFieldDefaults.textFieldWithoutLabelPadding(
                    start = 0.dp, bottom = 7.dp
                )
            )
        }
    }

    errorText?.apply { ErrorTextTextField(errorText = this) }
}

@CombinedThemePreviews
@Composable
private fun PreviewLabelTextField(
    @PreviewParameter(TextFieldProvider::class) state: TextFieldState,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
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