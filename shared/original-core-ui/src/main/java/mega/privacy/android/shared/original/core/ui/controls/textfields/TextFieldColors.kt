package mega.privacy.android.shared.original.core.ui.controls.textfields

import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import mega.android.core.ui.tokens.theme.DSTokens

@Composable
internal fun textFieldColors(showIndicatorLine: Boolean = true) = TextFieldDefaults.textFieldColors(
    textColor = DSTokens.colors.text.primary,
    backgroundColor = Color.Transparent,
    cursorColor = DSTokens.colors.text.accent,
    errorCursorColor = DSTokens.colors.text.error,
    errorIndicatorColor = if (!showIndicatorLine) Color.Transparent else DSTokens.colors.text.error,
    focusedIndicatorColor = if (!showIndicatorLine) Color.Transparent else DSTokens.colors.border.strong,
    unfocusedIndicatorColor = if (!showIndicatorLine) Color.Transparent else DSTokens.colors.border.strong,
    focusedLabelColor = DSTokens.colors.text.secondary,
    unfocusedLabelColor = DSTokens.colors.text.secondary,
    errorLabelColor = DSTokens.colors.text.error,
    placeholderColor = DSTokens.colors.text.placeholder,
)

@Composable
internal fun customTextSelectionColors() = TextSelectionColors(
    handleColor = DSTokens.colors.border.strongSelected,
    backgroundColor = DSTokens.colors.button.secondaryPressed
)