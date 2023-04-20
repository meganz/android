package mega.privacy.android.core.ui.controls.textfields

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.controls.MegaTextField
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.grey_087_white_087
import mega.privacy.android.core.ui.theme.extensions.grey_white_alpha_038
import mega.privacy.android.core.ui.theme.extensions.teal_300_200

/**
 * TextField Generic Title
 *
 * @param value                 Text
 * @param onValueChange         When text changes
 * @param placeholderId         Placeholder string resource Id
 * @param charLimitErrorId      Char limit error string resource Id
 * @param emptyValueErrorId     Empty value error string resource Id
 * @param isEmptyValueError     True if it's empty value error. False, if not.
 * @param charLimit             Char limit value
 */
@Composable
fun GenericTitleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes placeholderId: Int? = null,
    @StringRes charLimitErrorId: Int? = null,
    isEmptyValueError: Boolean = false,
    @StringRes emptyValueErrorId: Int? = null,
    charLimit: Int,
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    var isCharLimitError by remember { mutableStateOf(false) }

    fun validate(text: String) {
        isCharLimitError = text.length > charLimit
    }

    Column {
        if (value.isNotEmpty()) {
            validate(value)
        }

        val colors = TextFieldDefaults.textFieldColors(
            backgroundColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            textColor = MaterialTheme.colors.grey_087_white_087,
            cursorColor = MaterialTheme.colors.teal_300_200,
            focusedLabelColor = MaterialTheme.colors.grey_087_white_087,
            focusedIndicatorColor = Color.Transparent,
        )

        MegaTextField(
            modifier = Modifier
                .focusRequester(focusRequester)
                .onFocusChanged {
                    if (isFocused != it.isFocused) {
                        isFocused = it.isFocused
                    }
                },
            value = value,
            onValueChange = onValueChange,
            enabled = true,
            showBorder = true,
            readOnly = false,
            maxLines = 1,
            singleLine = true,
            textStyle = MaterialTheme.typography.h6,
            isError = isCharLimitError || isEmptyValueError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Default,
                capitalization = KeyboardCapitalization.Sentences
            ),
            placeholder = {
                placeholderId?.let { id ->
                    Text(
                        text = stringResource(id = id),
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.grey_white_alpha_038,
                        textAlign = TextAlign.Start
                    )
                }
            },
            colors = colors,
            contentPadding = PaddingValues(top = 12.dp),
            defaultMinHeight = 48.dp
        )

        charLimitErrorId?.let { id ->
            if (isCharLimitError) {
                ErrorTextTextField(errorTextId = id)
            }
        }

        emptyValueErrorId?.let { id ->
            if (isEmptyValueError) {
                ErrorTextTextField(errorTextId = id)
            }
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

@Preview
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PreviewGenericTitleTextField() {
    var content by remember {
        mutableStateOf("")
    }

    AndroidTheme(isDark = isSystemInDarkTheme()) {
        GenericTitleTextField(
            value = content,
            isEmptyValueError = false,
            onValueChange = { content = it },
            charLimit = 30
        )
    }
}