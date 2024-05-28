//
// Generated automatically by KotlinTokensGenerator.
// Do not modify this file manually.
//
package mega.privacy.android.shared.original.core.ui.theme.values

import androidx.compose.ui.graphics.Color
import mega.android.core.ui.tokens.theme.tokens.Text

public enum class TextColor {
    Primary,
    Secondary,
    Accent,
    Placeholder,
    InverseAccent,
    OnColor,
    OnColorDisabled,
    Error,
    Success,
    Info,
    Warning,
    Disabled,
    Inverse,
    ;

    internal fun getTextColor(text: Text): Color = when (this) {
        Primary -> text.primary
        Secondary -> text.secondary
        Accent -> text.accent
        Placeholder -> text.placeholder
        InverseAccent -> text.inverseAccent
        OnColor -> text.onColor
        OnColorDisabled -> text.onColorDisabled
        Error -> text.error
        Success -> text.success
        Info -> text.info
        Warning -> text.warning
        Disabled -> text.disabled
        Inverse -> text.inverse
    }
}
