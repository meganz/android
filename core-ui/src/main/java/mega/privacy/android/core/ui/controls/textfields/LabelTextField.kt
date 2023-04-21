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
import androidx.compose.ui.Modifier
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
import mega.privacy.android.core.ui.theme.extensions.grey012White038
import mega.privacy.android.core.ui.theme.extensions.grey_087_white_087
import mega.privacy.android.core.ui.theme.extensions.grey_white_alpha_038
import mega.privacy.android.core.ui.theme.extensions.red_900_red_400
import mega.privacy.android.core.ui.theme.extensions.teal_300_200

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LabelTextField(
    modifier: Modifier = Modifier,
    text: String = "",
    onTextChange: (String) -> Unit,
    label: String,
    errorText: String? = null,
    imeAction: ImeAction,
    keyboardActions: KeyboardActions,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) = Column(modifier = modifier) {
    val isError = errorText != null
    val colors = TextFieldDefaults.textFieldColors(
        textColor = MaterialTheme.colors.grey_087_white_087,
        backgroundColor = Color.Transparent,
        cursorColor = MaterialTheme.colors.teal_300_200,
        errorCursorColor = MaterialTheme.colors.red_900_red_400,
        errorIndicatorColor = MaterialTheme.colors.red_900_red_400,
        focusedIndicatorColor = MaterialTheme.colors.teal_300_200,
        unfocusedIndicatorColor = MaterialTheme.colors.grey012White038,
        focusedLabelColor = MaterialTheme.colors.grey_087_white_087,
        unfocusedLabelColor = MaterialTheme.colors.grey_white_alpha_038,
        errorLabelColor = MaterialTheme.colors.red_900_red_400,
    )
    var isFocused by remember { mutableStateOf(false) }
    val customTextSelectionColors = TextSelectionColors(
        handleColor = MaterialTheme.colors.teal_300_200,
        backgroundColor = MaterialTheme.colors.teal_300_200
    )

    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = modifier
                .background(Color.Transparent)
                .indicatorLine(true, isError, interactionSource, colors)
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused },
            textStyle = MaterialTheme.typography.subtitle2.copy(color = MaterialTheme.colors.grey_087_white_087),
            cursorBrush = SolidColor(colors.cursorColor(isError).value),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
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
                        modifier = modifier.padding(bottom = if (isFocused) 6.dp else 0.dp),
                        style =
                        if (isFocused) MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.teal_300_200)
                        else MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.grey_white_alpha_038),
                    )
                },
                colors = colors,
                contentPadding = TextFieldDefaults.textFieldWithoutLabelPadding(
                    start = 0.dp,
                    bottom = 7.dp
                )
            )
        }
    }

    errorText?.apply {
        Text(
            text = text,
            modifier = modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.teal_300_200),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewLabelTextField(
    @PreviewParameter(TextFieldProvider::class) state: TextFieldState,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        LabelTextField(
            text = state.text,
            onTextChange = {},
            label = "Label",
            errorText = state.error,
            imeAction = ImeAction.Default,
            keyboardActions = KeyboardActions()
        )
    }
}