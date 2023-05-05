package mega.privacy.android.core.ui.controls.textfields

import androidx.annotation.StringRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.MaterialTheme
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TextFieldDefaults.indicatorLine
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.KeyboardActions
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.black_white
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_038_white_alpha_038

/**
 * TextField Generic Description
 *
 * @param value                 Text
 * @param onValueChange         When text changes
 * @param placeholderId         Placeholder string resource Id
 * @param charLimitErrorId      Char limit error string resource Id
 * @param charLimit             Char limit value
 */
@Composable
fun GenericDescriptionTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    @StringRes placeholderId: Int? = null,
    @StringRes charLimitErrorId: Int? = null,
    charLimit: Int,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    var isCharLimitError by remember { mutableStateOf(false) }

    fun validate(text: String) {
        isCharLimitError = text.length > charLimit
    }

    Column(modifier = modifier) {
        if (value.isNotEmpty()) {
            validate(value)
        }

        val textFieldColors = TextFieldDefaults.textFieldColors(
            textColor = MaterialTheme.colors.onPrimary,
            backgroundColor = Color.Transparent,
            cursorColor = MaterialTheme.colors.secondary,
            errorCursorColor = MaterialTheme.colors.error,
            errorIndicatorColor = MaterialTheme.colors.error,
            focusedLabelColor = MaterialTheme.colors.onPrimary,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            unfocusedLabelColor = MaterialTheme.colors.grey_alpha_038_white_alpha_038,
            errorLabelColor = MaterialTheme.colors.error,
        )

        val customTextSelectionColors = TextSelectionColors(
            handleColor = MaterialTheme.colors.secondary,
            backgroundColor = MaterialTheme.colors.secondary
        )

        val keyboardOption = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Default,
            capitalization = KeyboardCapitalization.Sentences
        )

        val isError = isCharLimitError

        CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
            @OptIn(ExperimentalMaterialApi::class)
            (BasicTextField(
                value = value,
                modifier = Modifier
                    .defaultMinSize(
                        minWidth = TextFieldDefaults.MinWidth,
                        minHeight = 48.dp
                    )
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        if (isFocused != it.isFocused) {
                            isFocused = it.isFocused
                        }
                    }
                    .indicatorLine(
                        true,
                        isError,
                        interactionSource,
                        textFieldColors
                    ),
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.subtitle2.copy(
                    color = MaterialTheme.colors.onPrimary,
                    textAlign = TextAlign.Start
                ),
                cursorBrush = SolidColor(textFieldColors.cursorColor(isError).value),
                keyboardOptions = keyboardOption,
                keyboardActions = KeyboardActions.Default,
                interactionSource = interactionSource,
                decorationBox = @Composable { innerTextField ->
                    TextFieldDefaults.TextFieldDecorationBox(
                        value = value,
                        visualTransformation = visualTransformation,
                        innerTextField = innerTextField,
                        placeholder = {
                            placeholderId?.let { id ->
                                Text(
                                    text = stringResource(id = id),
                                    style = MaterialTheme.typography.body1.copy(
                                        color = if (isFocused) MaterialTheme.colors.grey_alpha_038_white_alpha_038 else MaterialTheme.colors.black_white,
                                        textAlign = TextAlign.Start
                                    ),
                                )
                            }
                        },
                        singleLine = true,
                        enabled = true,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = textFieldColors,
                        contentPadding = PaddingValues(top = if (value.isEmpty()) 13.dp else 15.dp),
                    )
                }
            ))
        }

        charLimitErrorId?.let { id ->
            if (isCharLimitError) {
                ErrorTextTextField(errorText = stringResource(id = id))
            }
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewGenericDescriptionTextField() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        GenericDescriptionTextField(
            value = "Description text",
            onValueChange = { },
            charLimit = 4000
        )
    }
}
