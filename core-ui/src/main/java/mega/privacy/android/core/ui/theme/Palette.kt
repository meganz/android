package mega.privacy.android.core.ui.theme

import androidx.compose.ui.graphics.Color
import mega.privacy.android.core.ui.theme.tokens.Dark
import mega.privacy.android.core.ui.theme.tokens.Light

/**
 * [MegaColors] palette to be used in Light Theme
 */
internal val lightColorPalette = MegaColors(
    buttonPrimary = Light.Button.colorButtonPrimary,
    textPrimary = Light.Text.colorTextPrimary,
    textInverse = Light.Text.ColorTextInverse,
    textWarning = Light.Text.colorTextWarning,
    notificationWarning = Light.Notifications.colorNotificationWarning,
    isLight = true
)

/**
 * [MegaColors] palette to be used in Dark Theme
 */
internal val darkColorPalette = MegaColors(
    buttonPrimary = Dark.Button.colorButtonPrimary,
    textPrimary = Dark.Text.colorTextPrimary,
    textInverse = Dark.Text.ColorTextInverse,
    textWarning = Dark.Text.colorTextWarning,
    notificationWarning = Dark.Notifications.colorNotificationWarning,
    isLight = false
)

/**
 * [MegaColors] default palette for testing purposes, all magenta to easily detect it.
 */
internal val testColorPalette = MegaColors(
    Color.Magenta,
    Color.Magenta,
    Color.Magenta,
    Color.Magenta,
    Color.Magenta,
    false,
)