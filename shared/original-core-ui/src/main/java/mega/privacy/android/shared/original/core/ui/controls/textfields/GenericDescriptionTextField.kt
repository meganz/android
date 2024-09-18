package mega.privacy.android.shared.original.core.ui.controls.textfields

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.controls.textfields.transformations.PrefixTransformation
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * TextField Generic Description
 *
 * @param value                 Text
 * @param onValueChange         When text changes
 * @param placeholder           Placeholder string
 */
@Composable
fun GenericDescriptionTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    initiallyFocused: Boolean = true,
    isEnabled: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    showUnderline: Boolean = false,
    placeholder: String? = null,
    supportingText: String? = null,
    title: String? = null,
    showError: Boolean = false,
    onSizeChange: () -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    var size by remember { mutableStateOf(IntSize(0, 0)) }



    Column(modifier = modifier.testTag(GENERIC_DESCRIPTION_TEXT_FIELD_COLUMN)) {
        val textFieldColors = TextFieldDefaults.textFieldColors(
            textColor = if (isFocused) MegaOriginalTheme.colors.text.primary else MegaOriginalTheme.colors.text.onColorDisabled,
            backgroundColor = MegaOriginalTheme.colors.background.pageBackground,
            cursorColor = MegaOriginalTheme.colors.border.strongSelected,
            errorCursorColor = MegaOriginalTheme.colors.text.error,
            errorIndicatorColor = MegaOriginalTheme.colors.support.error,
            focusedLabelColor = MegaOriginalTheme.colors.text.primary,
            focusedIndicatorColor = if (showUnderline) MegaOriginalTheme.colors.text.accent else Color.Transparent,
            unfocusedIndicatorColor = if (showUnderline) MegaOriginalTheme.colors.border.strong else Color.Transparent,
            unfocusedLabelColor = MegaOriginalTheme.colors.text.onColorDisabled,
            errorLabelColor = MegaOriginalTheme.colors.text.error,
        )

        val customTextSelectionColors = TextSelectionColors(
            handleColor = MegaOriginalTheme.colors.border.strongSelected,
            backgroundColor = MegaOriginalTheme.colors.border.strongSelected
        )

        val keyboardOption = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = imeAction,
            capitalization = KeyboardCapitalization.Sentences
        )

        if (!isFocused && value.isNotEmpty()) {
            title?.let { text ->
                Text(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .testTag(GENERIC_DESCRIPTION_TEXT_FIELD_TITLE_TEXT),
                    text = text,
                    style = MaterialTheme.typography.subtitle1.copy(
                        color = MegaOriginalTheme.colors.text.primary,
                        textAlign = TextAlign.Start
                    )
                )
            }
        }

        CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
            @OptIn(ExperimentalMaterialApi::class)
            BasicTextField(
                value = value,
                maxLines = maxLines,
                enabled = isEnabled,
                modifier = modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        if (isFocused != it.isFocused) {
                            isFocused = it.isFocused
                        }
                    }
                    .indicatorLine(
                        enabled = true,
                        isError = showError,
                        interactionSource = interactionSource,
                        colors = textFieldColors,
                        focusedIndicatorLineThickness = 1.dp,
                        unfocusedIndicatorLineThickness = 1.dp
                    )
                    .onSizeChanged { newSize ->
                        if (newSize != size) {
                            onSizeChange()
                        }
                        size = newSize
                    }
                    .testTag(GENERIC_DESCRIPTION_TEXT_FIELD_TEXT),
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.subtitle2.copy(
                    color = MegaOriginalTheme.colors.text.primary,
                    textAlign = TextAlign.Start
                ),
                visualTransformation = visualTransformation,
                cursorBrush = SolidColor(textFieldColors.cursorColor(showError).value),
                keyboardOptions = keyboardOption,
                keyboardActions = keyboardActions,
                interactionSource = interactionSource,
                decorationBox = @Composable { innerTextField ->
                    TextFieldDefaults.TextFieldDecorationBox(
                        value = value,
                        visualTransformation = visualTransformation,
                        innerTextField = innerTextField,
                        placeholder = {
                            placeholder?.let { text ->
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.body1.copy(
                                        color = if (isFocused) MegaOriginalTheme.colors.text.onColorDisabled else MegaOriginalTheme.colors.text.primary,
                                        textAlign = TextAlign.Start
                                    ),
                                )
                            }
                        },
                        singleLine = true,
                        enabled = true,
                        isError = showError,
                        interactionSource = interactionSource,
                        colors = textFieldColors,
                        contentPadding = PaddingValues(vertical = 8.dp)
                    )
                }
            )
        }

        supportingText?.let { message ->
            if (showError) {
                ErrorTextTextField(
                    errorText = message,
                    modifier = Modifier.testTag(GENERIC_DESCRIPTION_TEXT_FIELD_ERROR_TEXT)
                )
            } else {
                MegaText(
                    modifier = Modifier.testTag(GENERIC_DESCRIPTION_TEXT_FIELD_INFO_DESCRIPTION),
                    text = message,
                    textColor = TextColor.Secondary,
                    style = MaterialTheme.typography.body2,
                )
            }
        }

        if (initiallyFocused) {
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun GenericDescriptionTextFieldPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        var textFieldValue by remember { mutableStateOf("") }
        GenericDescriptionTextField(
            value = textFieldValue,
            onValueChange = { textFieldValue = it },
            placeholder = "Description",
            showUnderline = true,
            supportingText = "Error message",
            visualTransformation = PrefixTransformation("#"),
        )
    }
}

/**
 * Generic description text field parent column
 */
const val GENERIC_DESCRIPTION_TEXT_FIELD_COLUMN = "generic_description_text_field:parent_column"

/**
 * Generic description text field text
 */
const val GENERIC_DESCRIPTION_TEXT_FIELD_TEXT = "generic_description_text_field:text"

/**
 * Generic description text field error text
 */
const val GENERIC_DESCRIPTION_TEXT_FIELD_ERROR_TEXT = "generic_description_text_field:error_text"

/**
 * Generic description text field title text
 */
const val GENERIC_DESCRIPTION_TEXT_FIELD_INFO_DESCRIPTION =
    "generic_description_text_field:info_description"

/**
 * Generic description text field title text
 */
const val GENERIC_DESCRIPTION_TEXT_FIELD_TITLE_TEXT = "generic_description_text_field:title_text"