//
// Generated automatically by KotlinTokensGenerator.
// Do not modify this file manually.
//
package mega.privacy.android.shared.original.core.ui.theme.values

import androidx.compose.ui.graphics.Color
import mega.android.core.ui.tokens.theme.tokens.Icon

public enum class IconColor {
    Primary,
    Secondary,
    Accent,
    InverseAccent,
    OnColor,
    OnColorDisabled,
    Inverse,
    Disabled,
    ;

    internal fun getIconColor(icon: Icon): Color = when (this) {
        Primary -> icon.primary
        Secondary -> icon.secondary
        Accent -> icon.accent
        InverseAccent -> icon.inverseAccent
        OnColor -> icon.onColor
        OnColorDisabled -> icon.onColorDisabled
        Inverse -> icon.inverse
        Disabled -> icon.disabled
    }
}
