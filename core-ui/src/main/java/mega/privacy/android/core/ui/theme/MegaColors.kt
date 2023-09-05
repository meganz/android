package mega.privacy.android.core.ui.theme

import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Theme colors to be used in all core-ui components.
 */
@Immutable
internal data class MegaColors(
    val buttonPrimary: Color,
    val textPrimary: Color,
    val textInverse: Color,
    val textWarning: Color,
    val notificationWarning: Color,
    val isLight: Boolean,
) {

    val buttonsColors
        @Composable
        get() = ButtonDefaults.buttonColors(
            backgroundColor = Color.Transparent,
            contentColor = buttonPrimary,
            disabledBackgroundColor = Color.Transparent,
            disabledContentColor = buttonPrimary.copy(alpha = .38f),
        )
    val raisedButtonColors
        @Composable
        get() = ButtonDefaults.buttonColors(
            backgroundColor = buttonPrimary,
            contentColor = textInverse,
            disabledBackgroundColor = buttonPrimary.copy(alpha = .25f),
            disabledContentColor = textInverse,
        )
}