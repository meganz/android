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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.preview.TextFieldProvider
import mega.privacy.android.shared.original.core.ui.preview.TextFieldState
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Text field generic.
 *
 * @param text            Typed text.
 * @param onTextChange    Action required for notifying about text changes.
 * @param modifier        [Modifier]
 * @param textFieldModifier [Modifier]
 * @param placeholder     String to show when the field is empty.
 * @param errorText       Error to show if any.
 * @param imeAction       [ImeAction]
 * @param keyboardType Specifies the type of keys available for the Keyboard (e.g. Text, Number)
 * @param keyboardActions [KeyboardActions]
 * @param singleLine when set to true, this text field becomes a single horizontally scrolling text field instead of wrapping onto multiple lines.
 * @param trailingIcon  the optional trailing icon to be displayed at the end of the text field container
 * @param showIndicatorLine when set to false the indicator line under the text won't be shown
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
    imeAction: ImeAction = ImeAction.Done,
    keyboardType: KeyboardType = KeyboardType.Text,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null,
    showIndicatorLine: Boolean = true,
) = GenericTextField(
    text = text,
    onTextChange = onTextChange,
    modifier = modifier,
    textFieldModifier = textFieldModifier,
    placeholder = placeholder,
    errorText = errorText,
    imeAction = imeAction,
    keyboardType = keyboardType,
    keyboardActions = keyboardActions,
    singleLine = singleLine,
    trailingIcon = trailingIcon,
    showIndicatorLine = showIndicatorLine,
    basicTextField = @Composable {
        with(it) {
            BasicTextField(
                value = this.value,
                onValueChange = this.onValueChange,
                modifier = this.textFieldModifier,
                textStyle = this.textStyle,
                cursorBrush = this.cursorBrush,
                singleLine = this.singleLine,
                keyboardOptions = this.keyboardOptions,
                keyboardActions = this.keyboardActions,
                interactionSource = this.interactionSource,
                decorationBox = this.decorationBox,
            )
        }
    }
)

/**
 * Text field generic with [TextFieldValue] as input
 *
 * @param textFieldValue  Typed text with selection.
 * @param onTextChange    Action required for notifying about text changes.
 * @param modifier        [Modifier]
 * @param textFieldModifier [Modifier]
 * @param placeholder     String to show when the field is empty.
 * @param errorText       Error to show if any.
 * @param imeAction       [ImeAction]
 * @param keyboardType Specifies the type of keys available for the Keyboard (e.g. Text, Number)
 * @param keyboardActions [KeyboardActions]
 * @param singleLine when set to true, this text field becomes a single horizontally scrolling text field instead of wrapping onto multiple lines.
 * @param trailingIcon  the optional trailing icon to be displayed at the end of the text field container
 * @param showIndicatorLine when set to false the indicator line under the text won't be shown
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun GenericTextField(
    textFieldValue: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    textFieldModifier: Modifier = Modifier,
    placeholder: String = "",
    errorText: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    keyboardType: KeyboardType = KeyboardType.Text,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null,
    showIndicatorLine: Boolean = true,
) = GenericTextField(
    text = textFieldValue,
    onTextChange = onTextChange,
    modifier = modifier,
    textFieldModifier = textFieldModifier,
    placeholder = placeholder,
    errorText = errorText,
    imeAction = imeAction,
    keyboardType = keyboardType,
    keyboardActions = keyboardActions,
    singleLine = singleLine,
    trailingIcon = trailingIcon,
    showIndicatorLine = showIndicatorLine,
    @Composable {
        with(it) {
            BasicTextField(
                value = this.value,
                onValueChange = this.onValueChange,
                modifier = this.textFieldModifier,
                textStyle = this.textStyle,
                cursorBrush = this.cursorBrush,
                singleLine = this.singleLine,
                keyboardOptions = this.keyboardOptions,
                keyboardActions = this.keyboardActions,
                interactionSource = this.interactionSource,
                decorationBox = this.decorationBox,
            )
        }
    }
)

private data class BasicTextFieldParams<T>(
    val value: T,
    val onValueChange: (T) -> Unit,
    val textFieldModifier: Modifier,
    val textStyle: TextStyle,
    val cursorBrush: Brush,
    val singleLine: Boolean,
    val keyboardOptions: KeyboardOptions,
    val keyboardActions: KeyboardActions,
    val interactionSource: MutableInteractionSource,
    val decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit,
)

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun <T> GenericTextField(
    text: T,
    onTextChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    textFieldModifier: Modifier,
    placeholder: String,
    errorText: String?,
    imeAction: ImeAction,
    keyboardType: KeyboardType,
    keyboardActions: KeyboardActions,
    singleLine: Boolean,
    trailingIcon: @Composable (() -> Unit)?,
    showIndicatorLine: Boolean,
    basicTextField: @Composable (BasicTextFieldParams<T>) -> Unit,
) = Column(modifier = modifier) {
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val isError = errorText != null

    val colors = textFieldColors(showIndicatorLine)
    val customTextSelectionColors = customTextSelectionColors()

    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        basicTextField(
            BasicTextFieldParams<T>(
                value = text,
                onValueChange = onTextChange,
                textFieldModifier = textFieldModifier
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
                    text = if (text is TextFieldValue) text.text else text.toString(),
                    innerTextField = it,
                    trailingIcon = trailingIcon,
                    singleLine = singleLine,
                    interactionSource = interactionSource,
                    isError = isError,
                    placeholder = placeholder,
                    colors = colors,
                )
            })

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
    trailingIcon: @Composable (() -> Unit)? = null,
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
                style = MaterialTheme.typography.body1
            )
        },
        colors = colors,
        contentPadding = TextFieldDefaults.textFieldWithoutLabelPadding(
            start = 0.dp,
            bottom = 7.dp
        ),
        trailingIcon = trailingIcon
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
    OriginalTheme(isDark = isSystemInDarkTheme()) {
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
    OriginalTheme(isDark = isSystemInDarkTheme()) {
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