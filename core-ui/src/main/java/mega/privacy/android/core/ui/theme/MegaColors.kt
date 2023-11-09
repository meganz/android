package mega.privacy.android.core.ui.theme

import androidx.compose.material.ButtonDefaults
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import mega.privacy.android.core.ui.theme.tokens.SemanticTokens

/**
 * Theme colors to be used in all core-ui components.
 */
@Immutable
internal data class MegaColors(
    private val semanticTokens: SemanticTokens,
    val isLight: Boolean,
) : SemanticTokens by semanticTokens {

    val buttonsColors
        @Composable
        get() = ButtonDefaults.buttonColors(
            backgroundColor = Color.Transparent,
            contentColor = text.accent,
            disabledBackgroundColor = Color.Transparent,
            disabledContentColor = text.disabled,
        )
    val snackBarButtonColors
        @Composable
        get() = ButtonDefaults.buttonColors(
            backgroundColor = Color.Transparent,
            contentColor = text.inverse,
            disabledBackgroundColor = Color.Transparent,
            disabledContentColor = text.disabled,
        )
    val raisedButtonColors
        @Composable
        get() = ButtonDefaults.buttonColors(
            backgroundColor = button.primary,
            contentColor = text.inverse,
            disabledBackgroundColor = button.disabled,
            disabledContentColor = text.disabled,
        )

    val radioColors
        @Composable
        get() = RadioButtonDefaults.colors(
            selectedColor = MegaTheme.colors.border.strongSelected,
            unselectedColor = MegaTheme.colors.icon.secondary,
            disabledColor = MegaTheme.colors.border.disabled,
        )
}