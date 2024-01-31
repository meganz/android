package mega.privacy.android.core.ui.controls.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

internal const val CHAT_TEXT_FIELD_TEXT_TAG = "chat_text_field"
internal const val CHAT_TEXT_FIELD_EMOJI_ICON = "chat_text_field:emoji_icon"

/**
 * Chat text field
 *
 * @param textFieldValue text to show
 * @param onTextChange when text changes
 * @param modifier modifier for the text field
 * @param placeholder placeholder text
 * @param singleLine single line or not
 * @param imeAction ime action
 * @param keyboardActions keyboard actions
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChatTextField(
    textFieldValue: TextFieldValue,
    isExpanded: Boolean,
    onTextChange: (TextFieldValue) -> Unit,
    onEmojiClick: () -> Unit,
    isEmojiPickerShown: Boolean,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = false,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    maxLines: Int = if (isExpanded) Int.MAX_VALUE else 5,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) = Box(modifier = modifier) {
    val colors = TextFieldDefaults.textFieldColors(
        textColor = MegaTheme.colors.text.placeholder,
        backgroundColor = MegaTheme.colors.background.surface1,
        cursorColor = MegaTheme.colors.border.strongSelected,
        errorCursorColor = MegaTheme.colors.text.error,
        errorIndicatorColor = MegaTheme.colors.text.error,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
    )
    val customTextSelectionColors = TextSelectionColors(
        handleColor = MegaTheme.colors.border.strongSelected,
        backgroundColor = MegaTheme.colors.border.strongSelected
    )

    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = onTextChange,
            modifier = Modifier
                .testTag(CHAT_TEXT_FIELD_TEXT_TAG)
                .indicatorLine(
                    enabled = true,
                    isError = false,
                    interactionSource = interactionSource,
                    colors = colors
                )
                .fillMaxWidth(),
            textStyle = MaterialTheme.typography.body1.copy(color = MegaTheme.colors.text.primary),
            cursorBrush = SolidColor(colors.cursorColor(false).value),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = imeAction
            ),
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            singleLine = singleLine,
            maxLines = maxLines,
            onTextLayout = onTextLayout,
        ) {
            TextFieldDefaults.TextFieldDecorationBox(
                value = textFieldValue.text,
                innerTextField = it,
                enabled = true,
                singleLine = singleLine,
                interactionSource = interactionSource,
                visualTransformation = VisualTransformation.None,
                isError = false,
                placeholder = {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.body1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = colors,
                contentPadding = TextFieldDefaults.textFieldWithoutLabelPadding(
                    start = 12.dp,
                    bottom = 10.dp,
                    top = 10.dp,
                    end = if (isExpanded) 12.dp else 40.dp
                ),
            )
        }
    }

    Icon(
        modifier = Modifier
            .testTag(CHAT_TEXT_FIELD_EMOJI_ICON)
            .align(Alignment.BottomEnd)
            .padding(end = 8.dp, bottom = 8.dp)
            .clickable { onEmojiClick() },
        painter = painterResource(
            id =
            if (isEmojiPickerShown) R.drawable.ic_emoji_smile_medium_regular_solid
            else R.drawable.ic_emoji_smile_medium_regular
        ),
        contentDescription = "Emoji Icon",
        tint = MegaTheme.colors.icon.secondary
    )
}

@CombinedThemePreviews
@Composable
private fun ChatTextFieldPreview(
    @PreviewParameter(BooleanProvider::class) isEmojiPickerShown: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ChatTextField(
            textFieldValue = TextFieldValue("how it looks like when the text is too long"),
            placeholder = "Message",
            onTextChange = {},
            onEmojiClick = {},
            isEmojiPickerShown = isEmojiPickerShown,
            isExpanded = false
        )
    }
}