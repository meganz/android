package mega.privacy.android.core.ui.theme

import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import mega.privacy.android.core.ui.theme.extensions.red_600_red_300
import mega.privacy.android.core.ui.theme.tokens.SemanticTokens

/**
 * Theme colors to be used in all core-ui components.
 */
@Immutable
internal data class MegaColors(
    private val semanticTokens: SemanticTokens,
    val isLight: Boolean,
) : SemanticTokens by semanticTokens {

    val isDark = !isLight

    val raisedButtonColors
        @Composable
        get() = ButtonDefaults.buttonColors(
            backgroundColor = button.primary,
            contentColor = text.inverse,
            disabledBackgroundColor = button.disabled,
            disabledContentColor = text.disabled,
        )

    val raisedErrorButtonColors
        @Composable
        get() = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.red_600_red_300,
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