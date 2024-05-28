package mega.privacy.android.shared.original.core.ui.theme

import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import mega.android.core.ui.tokens.theme.tokens.SemanticTokens
import mega.privacy.android.shared.original.core.ui.theme.extensions.red_600_red_300

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
            selectedColor = MegaOriginalTheme.colors.border.strongSelected,
            unselectedColor = MegaOriginalTheme.colors.icon.secondary,
            disabledColor = MegaOriginalTheme.colors.border.disabled,
        )
}