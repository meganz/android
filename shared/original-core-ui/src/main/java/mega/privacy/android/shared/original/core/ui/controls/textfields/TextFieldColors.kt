package mega.privacy.android.shared.original.core.ui.controls.textfields

import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme

@Composable
internal fun textFieldColors(showIndicatorLine: Boolean = true) = TextFieldDefaults.textFieldColors(
    textColor = MegaOriginalTheme.colors.text.primary,
    backgroundColor = Color.Transparent,
    cursorColor = MegaOriginalTheme.colors.text.accent,
    errorCursorColor = MegaOriginalTheme.colors.text.error,
    errorIndicatorColor = if (!showIndicatorLine) Color.Transparent else MegaOriginalTheme.colors.text.error,
    focusedIndicatorColor = if (!showIndicatorLine) Color.Transparent else MegaOriginalTheme.colors.border.strong,
    unfocusedIndicatorColor = if (!showIndicatorLine) Color.Transparent else MegaOriginalTheme.colors.border.strong,
    focusedLabelColor = MegaOriginalTheme.colors.text.secondary,
    unfocusedLabelColor = MegaOriginalTheme.colors.text.secondary,
    errorLabelColor = MegaOriginalTheme.colors.text.error,
    placeholderColor = MegaOriginalTheme.colors.text.placeholder,
)

@Composable
internal fun customTextSelectionColors() = TextSelectionColors(
    handleColor = MegaOriginalTheme.colors.border.strongSelected,
    backgroundColor = MegaOriginalTheme.colors.border.strongSelected.copy(alpha = 0.4f)
)