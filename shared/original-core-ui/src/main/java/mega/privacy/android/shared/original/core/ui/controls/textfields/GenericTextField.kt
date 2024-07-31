package mega.privacy.android.shared.original.core.ui.controls.textfields

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TextFieldDefaults.indicatorLine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
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

/**
 * Text field generic.
 *
 * @param placeholder     String to show when the field is empty.
 * @param onTextChange    Action required for notifying about text changes.
 * @param imeAction       [ImeAction]
 * @param keyboardActions [KeyboardActions]
 * @param modifier        [Modifier]
 * @param textFieldModifier [Modifier]
 * @param text            Typed text.
 * @param errorText       Error to show if any.
 * @param keyboardType Specifies the type of keys available for the Keyboard (e.g. Text, Number)
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun GenericTextField(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textFieldModifier: Modifier = Modifier,
    placeholder: String = "",
    errorText: String? = null,
    singleLine: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    keyboardType: KeyboardType = KeyboardType.Text,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    colors: TextFieldColors = TextFieldDefaults.textFieldColors(
        textColor = MegaOriginalTheme.colors.text.primary,
        backgroundColor = MegaOriginalTheme.colors.background.surface1,
        cursorColor = MegaOriginalTheme.colors.border.strongSelected,
        errorCursorColor = MegaOriginalTheme.colors.text.error,
        errorIndicatorColor = MegaOriginalTheme.colors.text.error,
        focusedIndicatorColor = MegaOriginalTheme.colors.border.strongSelected,
        unfocusedIndicatorColor = MegaOriginalTheme.colors.border.disabled,
    ),
) = Column(modifier = modifier) {
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val isError = errorText != null
    val customTextSelectionColors = TextSelectionColors(
        handleColor = MegaOriginalTheme.colors.border.strongSelected,
        backgroundColor = MegaOriginalTheme.colors.border.strongSelected
    )

    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = textFieldModifier
                .testTag(GENERIC_TEXT_FIELD_TEXT_TAG)
                .background(Color.Transparent)
                .indicatorLine(true, isError, interactionSource, colors)
                .fillMaxWidth(),
            textStyle = MaterialTheme.typography.body1.copy(color = MegaOriginalTheme.colors.text.primary),
            cursorBrush = SolidColor(colors.cursorColor(isError).value),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            singleLine = singleLine,
        ) {
            GenericDecorationBox(
                text,
                it,
                singleLine,
                interactionSource,
                isError,
                placeholder,
                colors,
            )
        }
    }

    GenericError(errorText)
}

/**
 * Text field generic with [TextFieldValue] as input
 *
 * @param textFieldValue  Typed text with selection.
 * @param onTextChange    Action required for notifying about text changes.
 * @param placeholder     String to show when the field is empty.
 * @param imeAction       [ImeAction]
 * @param keyboardType Specifies the type of keys available for the Keyboard (e.g. Text, Number)
 * @param keyboardActions [KeyboardActions]
 * @param modifier        [Modifier]
 * @param errorText       Error to show if any.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun GenericTextField(
    textFieldValue: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    errorText: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    keyboardType: KeyboardType = KeyboardType.Text,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    colors: TextFieldColors = TextFieldDefaults.textFieldColors(
        textColor = MegaOriginalTheme.colors.text.primary,
        backgroundColor = MegaOriginalTheme.colors.background.surface1,
        cursorColor = MegaOriginalTheme.colors.border.strongSelected,
        errorCursorColor = MegaOriginalTheme.colors.text.error,
        errorIndicatorColor = MegaOriginalTheme.colors.text.error,
        focusedIndicatorColor = MegaOriginalTheme.colors.border.strongSelected,
        unfocusedIndicatorColor = MegaOriginalTheme.colors.border.disabled,
    ),
    singleLine: Boolean = true,
) = Column(modifier = modifier) {
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val isError = errorText != null
    val customTextSelectionColors = TextSelectionColors(
        handleColor = MegaOriginalTheme.colors.border.strongSelected,
        backgroundColor = MegaOriginalTheme.colors.border.strongSelected
    )

    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = onTextChange,
            modifier = Modifier
                .testTag(GENERIC_TEXT_FIELD_TEXT_TAG)
                .background(Color.Transparent)
                .indicatorLine(true, isError, interactionSource, colors)
                .fillMaxWidth(),
            textStyle = MaterialTheme.typography.body1.copy(color = MegaOriginalTheme.colors.text.primary),
            cursorBrush = SolidColor(colors.cursorColor(isError).value),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            singleLine = singleLine,
        ) {
            GenericDecorationBox(
                textFieldValue.text,
                it,
                singleLine,
                interactionSource,
                isError,
                placeholder,
                colors
            )
        }
    }

    GenericError(errorText)
}

@Composable
private fun GenericError(errorText: String?) {
    errorText?.let {
        ErrorTextTextField(
            modifier = Modifier.testTag(GENERIC_TEXT_FIELD_ERROR_TAG),
            errorText = it
        )
    }
}

@Composable
@OptIn(ExperimentalMaterialApi::class)
private fun GenericDecorationBox(
    text: String,
    innerTextField: @Composable () -> Unit,
    singleLine: Boolean,
    interactionSource: MutableInteractionSource,
    isError: Boolean,
    placeholder: String,
    colors: TextFieldColors,
) {
    TextFieldDefaults.TextFieldDecorationBox(
        value = text,
        innerTextField = innerTextField,
        enabled = true,
        singleLine = singleLine,
        interactionSource = interactionSource,
        visualTransformation = VisualTransformation.None,
        isError = isError,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.body1.copy(color = MegaOriginalTheme.colors.text.placeholder)
            )
        },
        colors = colors,
        contentPadding = TextFieldDefaults.textFieldWithoutLabelPadding(
            start = 0.dp,
            bottom = 7.dp
        )
    )
}

/**
 * GENERIC TEXT FIELD ERROR TAG
 */
const val GENERIC_TEXT_FIELD_ERROR_TAG = "generic_text_field:error_text"

internal const val GENERIC_TEXT_FIELD_TEXT_TAG = "generic_text_field:text_field"

@CombinedThemePreviews
@Composable
private fun PreviewGenericTextField(
    @PreviewParameter(TextFieldProvider::class) state: TextFieldState,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        var text by remember { mutableStateOf(state.text) }

        GenericTextField(
            placeholder = "Placeholder",
            onTextChange = { text = it },
            imeAction = ImeAction.Default,
            keyboardActions = KeyboardActions(),
            text = state.text,
            errorText = state.error,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewGenericTextFieldWithTextFieldValue(
    @PreviewParameter(TextFieldProvider::class) state: TextFieldState,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        var textFieldValue by remember {
            mutableStateOf(
                TextFieldValue(
                    text = state.text,
                    selection = TextRange(state.text.length)
                )
            )
        }

        GenericTextField(
            placeholder = "Placeholder",
            onTextChange = { textFieldValue = it },
            imeAction = ImeAction.Default,
            keyboardActions = KeyboardActions(),
            textFieldValue = textFieldValue,
            errorText = state.error,
        )
    }
}