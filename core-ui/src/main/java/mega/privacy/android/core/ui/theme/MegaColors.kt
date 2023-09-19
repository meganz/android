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
    val buttonOutline: Color,
    val buttonDisabled: Color,
    val borderDisabled: Color,
    val textPrimary: Color,
    val textInverse: Color,
    val textWarning: Color,
    val textAccent: Color,
    val textDisabled: Color,
    val iconPrimary: Color,
    val notificationWarning: Color,
    val isLight: Boolean,
) {

    val buttonsColors
        @Composable
        get() = ButtonDefaults.buttonColors(
            backgroundColor = Color.Transparent,
            contentColor = textAccent,
            disabledBackgroundColor = Color.Transparent,
            disabledContentColor = textDisabled,
        )
    val raisedButtonColors
        @Composable
        get() = ButtonDefaults.buttonColors(
            backgroundColor = buttonPrimary,
            contentColor = textInverse,
            disabledBackgroundColor = buttonDisabled,
            disabledContentColor = textDisabled,
        )
}