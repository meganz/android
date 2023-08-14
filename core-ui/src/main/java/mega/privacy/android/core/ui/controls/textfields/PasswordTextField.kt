package mega.privacy.android.core.ui.controls.textfields

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.preview.TextFieldProvider
import mega.privacy.android.core.ui.preview.TextFieldState
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.autofill
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_038
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_038_white_alpha_038

/**
 * Text field for password.
 *
 * @param onTextChange    Action required for notifying about text changes.
 * @param imeAction       [ImeAction]
 * @param keyboardActions [KeyboardActions]
 * @param modifier        [Modifier]
 * @param text            Typed text.
 * @param errorText       Error to show if any.
 */
@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
fun PasswordTextField(
    onTextChange: (String) -> Unit,
    imeAction: ImeAction,
    keyboardActions: KeyboardActions,
    modifier: Modifier = Modifier,
    text: String = "",
    errorText: String? = null,
    hint: String? = null,
) = Column(modifier = modifier) {
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val isError = errorText != null
    val colors = TextFieldDefaults.textFieldColors(
        textColor = MaterialTheme.colors.grey_alpha_012_white_alpha_038,
        backgroundColor = Color.Transparent,
        cursorColor = MaterialTheme.colors.secondary,
        errorCursorColor = MaterialTheme.colors.error,
        errorIndicatorColor = MaterialTheme.colors.error,
        focusedIndicatorColor = MaterialTheme.colors.secondary,
        unfocusedIndicatorColor = MaterialTheme.colors.grey_alpha_012_white_alpha_038,
        focusedLabelColor = MaterialTheme.colors.onPrimary,
        unfocusedLabelColor = MaterialTheme.colors.grey_alpha_038_white_alpha_038,
        errorLabelColor = MaterialTheme.colors.error,
    )
    var isFocused by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    val customTextSelectionColors = TextSelectionColors(
        handleColor = MaterialTheme.colors.secondary,
        backgroundColor = MaterialTheme.colors.secondary
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
                    autofillTypes = listOf(AutofillType.Password),
                    onAutoFilled = onTextChange
                ),
            textStyle = MaterialTheme.typography.subtitle1.copy(color = MaterialTheme.colors.onPrimary),
            cursorBrush = SolidColor(colors.cursorColor(isError).value),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = imeAction
            ),
            keyboardActions = keyboardActions,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            interactionSource = interactionSource,
            singleLine = true,
        ) {
            val hintString = hint ?: stringResource(id = R.string.password_text)
            TextFieldDefaults.TextFieldDecorationBox(
                value = text,
                innerTextField = it,
                enabled = true,
                singleLine = true,
                interactionSource = interactionSource,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                isError = isError,
                label = {
                    Text(
                        text = hintString,
                        modifier = Modifier.padding(bottom = if (isFocused) 6.dp else 0.dp),
                        style = when {
                            isError && isFocused -> MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.error)
                            isError && text.isEmpty() -> MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.error)
                            isFocused -> MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.secondary)
                            text.isNotEmpty() -> MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.grey_alpha_038_white_alpha_038)
                            else -> MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.grey_alpha_038_white_alpha_038)
                        }
                    )
                },
                trailingIcon = {
                    if (isFocused) {
                        Icon(
                            modifier = Modifier
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { showPassword = !showPassword },
                                ),
                            painter = painterResource(id = R.drawable.ic_visibility_outline),
                            tint = if (showPassword) MaterialTheme.colors.secondary else MaterialTheme.colors.grey_alpha_012_white_alpha_038,
                            contentDescription = "see"
                        )
                    }
                },
                colors = colors,
                contentPadding = TextFieldDefaults.textFieldWithoutLabelPadding(
                    start = 0.dp,
                    bottom = 7.dp
                )
            )
        }
    }

    errorText?.apply { ErrorTextTextField(errorText = this) }
}

@CombinedThemePreviews
@Composable
private fun PreviewErrorPasswordTextField(
    @PreviewParameter(TextFieldProvider::class) state: TextFieldState,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        var text by remember { mutableStateOf(state.text) }

        PasswordTextField(
            onTextChange = { text = it },
            imeAction = ImeAction.Default,
            keyboardActions = KeyboardActions(),
            text = state.text,
            errorText = state.error,
        )
    }

}