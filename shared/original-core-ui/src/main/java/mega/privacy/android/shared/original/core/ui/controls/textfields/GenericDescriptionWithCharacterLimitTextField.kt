package mega.privacy.android.shared.original.core.ui.controls.textfields

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.body3
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * TextField Generic Description with Character Limit
 */
@Composable
fun GenericDescriptionWithCharacterLimitTextField(
    maxCharacterLimit: Int,
    modifier: Modifier = Modifier,
    showClearIcon: Boolean = true,
    initiallyFocused: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
    value: String = "",
    onValueChange: (String) -> Unit,
    onClearText: () -> Unit = {},
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    var isFocused by rememberSaveable { mutableStateOf(initiallyFocused) }
    val focusRequester = remember { FocusRequester() }

    val keyboardOption = KeyboardOptions(
        keyboardType = KeyboardType.Text,
        imeAction = imeAction,
        capitalization = KeyboardCapitalization.Sentences
    )
    Column(modifier = modifier) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = {
                textFieldValue = it
                onValueChange(it.text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged {
                    if (isFocused != it.isFocused) {
                        isFocused = it.isFocused
                    }
                }
                .height(120.dp)
                .testTag(TEXT_FIELD_WITH_CHARACTER_LIMIT_VIEW_TEXT_FIELD),
            textStyle = MaterialTheme.typography.body3,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = MegaOriginalTheme.colors.text.primary,
                cursorColor = MegaOriginalTheme.colors.text.primary,
                focusedBorderColor = MegaOriginalTheme.colors.border.strongSelected,
                unfocusedBorderColor = MegaOriginalTheme.colors.border.strong,
                errorBorderColor = MegaOriginalTheme.colors.support.error
            ),
            isError = textFieldValue.text.length > maxCharacterLimit,
            keyboardOptions = keyboardOption,
            trailingIcon = {
                if (showClearIcon && textFieldValue.text.isNotEmpty()) {
                    Box(
                        contentAlignment = Alignment.TopEnd,
                        modifier = Modifier
                            .fillMaxHeight()
                    ) {
                        IconButton(
                            onClick = {
                                textFieldValue = TextFieldValue("")
                                onClearText()
                            },
                        ) {
                            Icon(
                                modifier = Modifier
                                    .size(16.dp),
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear text",
                                tint = MegaOriginalTheme.colors.icon.primary
                            )
                        }
                    }
                }
            }
        )

        MegaText(
            text = "${textFieldValue.text.length}/${maxCharacterLimit}",
            textColor = if (textFieldValue.text.length <= maxCharacterLimit) TextColor.Secondary else TextColor.Error,
            style = MaterialTheme.typography.body3,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 4.dp)
                .testTag(TEXT_FIELD_LIMIT_TEXT_COUNTER_TEST_TAG)
        )
    }

    if (initiallyFocused) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

@Composable
@CombinedThemePreviews
private fun TextFieldWithCharacterLimitEmptyPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        GenericDescriptionWithCharacterLimitTextField(
            maxCharacterLimit = 120,
            onValueChange = {}
        )
    }
}

@Composable
@CombinedThemePreviews
private fun TextFieldWithCharacterLimitDefaultPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        GenericDescriptionWithCharacterLimitTextField(
            maxCharacterLimit = 120,
            value = "This is a description with a character limit",
            initiallyFocused = true,
            onValueChange = {}
        )
    }
}

@Composable
@CombinedThemePreviews
private fun TextFieldWithCharacterLimitErrorPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        GenericDescriptionWithCharacterLimitTextField(
            maxCharacterLimit = 120,
            value = "This is a description with a character limit, but it's too long and takes times to fully read it because the number of characters of it exceeds the allowed limit",
            onValueChange = {}
        )
    }
}

internal const val TEXT_FIELD_LIMIT_TEXT_COUNTER_TEST_TAG =
    "text_field_with_character_limit:limit_text_counter"
internal const val TEXT_FIELD_WITH_CHARACTER_LIMIT_VIEW_TEXT_FIELD =
    "cancel_subscription_survey_view:text_field_with_character_limit_text_field"