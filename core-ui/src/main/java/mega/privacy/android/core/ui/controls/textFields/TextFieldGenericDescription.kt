package mega.privacy.android.core.ui.controls.textFields

import android.content.res.Configuration
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import androidx.compose.material.MaterialTheme
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TextFieldDefaults.indicatorLine
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.grey_alpha_087
import mega.privacy.android.core.ui.theme.teal_200
import mega.privacy.android.core.ui.theme.teal_300
import mega.privacy.android.core.ui.theme.white_alpha_087

/**
 * TextField Generic Description
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TextFieldGenericDescription(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String? = null,
    errorText: String? = null,
    charLimit: Int? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    var isError by remember { mutableStateOf(false) }

    fun validate(text: String) {
        charLimit?.let {
            isError = text.length > it
        }
    }

    Column {

        if (value.isNotEmpty()) {
            validate(value)
        }

        val colors = TextFieldDefaults.textFieldColors(
            backgroundColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            textColor = if (MaterialTheme.colors.isLight) grey_alpha_087 else white_alpha_087,
            cursorColor = if (MaterialTheme.colors.isLight) teal_300 else teal_200,
            focusedLabelColor = if (MaterialTheme.colors.isLight) grey_alpha_087 else white_alpha_087,
            focusedIndicatorColor = Color.Transparent,
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier
                .defaultMinSize(
                    minWidth = TextFieldDefaults.MinWidth,
                    minHeight = 48.dp
                )
                .focusRequester(focusRequester)
                .onFocusChanged {
                    if (isFocused != it.isFocused) {
                        isFocused = it.isFocused
                    }
                }
                .indicatorLine(
                    enabled = true,
                    isError = isError,
                    interactionSource = remember { MutableInteractionSource() },
                    colors = colors
                ),
            cursorBrush = SolidColor(colors.cursorColor(isError).value),
            textStyle = MaterialTheme.typography.subtitle2,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Default,
                capitalization = KeyboardCapitalization.Sentences
            )
        ) { innerTextField ->
            TextFieldDefaults.TextFieldDecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = false,
                isError = isError,
                visualTransformation = VisualTransformation.None,
                interactionSource = remember { MutableInteractionSource() },
                placeholder = {
                    placeholderText?.let { text ->
                        Text(text = text)
                    }
                },
                colors = colors,
                contentPadding = PaddingValues(
                    start = 0.dp,
                    top = 13.dp,
                    end = 0.dp,
                    bottom = 0.dp
                ),
            )
        }

        errorText?.let {
            if (isError) {
                Text(
                    modifier = Modifier.padding(start = 0.dp),
                    text = it,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.error
                )
            }
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

@ShowkaseComposable("Text field - Generic Description", "Text fields")
@Composable
fun ShowkasePreviewTextField() = PreviewTextFieldGenericDescription()

@Preview
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PreviewTextFieldGenericDescription() {
    var content by remember {
        mutableStateOf("")
    }
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        TextFieldGenericDescription(
            placeholderText = "Add description",
            value = content,
            onValueChange = { content = it },
        )
    }
}
