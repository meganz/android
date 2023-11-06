//
// Generated automatically by KotlinTokensGenerator.
// Do not modify this file manually.
//
package mega.privacy.android.core.ui.theme.tokens

import androidx.compose.ui.graphics.Color

internal data class Focus(
    public val colorFocus: Color = Color.Magenta,
)

internal data class Indicator(
    public val magenta: Color = Color.Magenta,
    public val yellow: Color = Color.Magenta,
    public val orange: Color = Color.Magenta,
    public val indigo: Color = Color.Magenta,
    public val blue: Color = Color.Magenta,
    public val green: Color = Color.Magenta,
    public val pink: Color = Color.Magenta,
)

internal data class Support(
    public val error: Color = Color.Magenta,
    public val warning: Color = Color.Magenta,
    public val success: Color = Color.Magenta,
    public val info: Color = Color.Magenta,
)

internal data class Button(
    public val disabled: Color = Color.Magenta,
    public val errorPressed: Color = Color.Magenta,
    public val errorHover: Color = Color.Magenta,
    public val error: Color = Color.Magenta,
    public val outlineBackgroundHover: Color = Color.Magenta,
    public val outlineHover: Color = Color.Magenta,
    public val outline: Color = Color.Magenta,
    public val primaryHover: Color = Color.Magenta,
    public val secondaryPressed: Color = Color.Magenta,
    public val brandPressed: Color = Color.Magenta,
    public val secondaryHover: Color = Color.Magenta,
    public val secondary: Color = Color.Magenta,
    public val brandHover: Color = Color.Magenta,
    public val brand: Color = Color.Magenta,
    public val primaryPressed: Color = Color.Magenta,
    public val outlinePressed: Color = Color.Magenta,
    public val primary: Color = Color.Magenta,
)

internal data class Text(
    public val inverse: Color = Color.Magenta,
    public val disabled: Color = Color.Magenta,
    public val warning: Color = Color.Magenta,
    public val info: Color = Color.Magenta,
    public val success: Color = Color.Magenta,
    public val error: Color = Color.Magenta,
    public val onColorDisabled: Color = Color.Magenta,
    public val onColor: Color = Color.Magenta,
    public val placeholder: Color = Color.Magenta,
    public val accent: Color = Color.Magenta,
    public val secondary: Color = Color.Magenta,
    public val primary: Color = Color.Magenta,
    public val inverseAccent: Color = Color.Magenta,
) {
    public fun getTextColor(textColor: TextColor): Color = when (textColor) {
        TextColor.Inverse -> inverse
        TextColor.Disabled -> disabled
        TextColor.Warning -> warning
        TextColor.Info -> info
        TextColor.Success -> success
        TextColor.Error -> error
        TextColor.OnColorDisabled -> onColorDisabled
        TextColor.OnColor -> onColor
        TextColor.Placeholder -> placeholder
        TextColor.Accent -> accent
        TextColor.Secondary -> secondary
        TextColor.Primary -> primary
        TextColor.InverseAccent -> inverseAccent
    }
}

public enum class TextColor {
    Inverse,
    Disabled,
    Warning,
    Info,
    Success,
    Error,
    OnColorDisabled,
    OnColor,
    Placeholder,
    Accent,
    Secondary,
    Primary,
    InverseAccent,
}

internal data class Background(
    public val blur: Color = Color.Magenta,
    public val surface2: Color = Color.Magenta,
    public val surface3: Color = Color.Magenta,
    public val surface1: Color = Color.Magenta,
    public val inverse: Color = Color.Magenta,
    public val pageBackground: Color = Color.Magenta,
)

internal data class Icon(
    public val disabled: Color = Color.Magenta,
    public val inverse: Color = Color.Magenta,
    public val onColorDisabled: Color = Color.Magenta,
    public val onColor: Color = Color.Magenta,
    public val inverseAccent: Color = Color.Magenta,
    public val accent: Color = Color.Magenta,
    public val secondary: Color = Color.Magenta,
    public val primary: Color = Color.Magenta,
)

internal data class Components(
    public val toastBackground: Color = Color.Magenta,
    public val interactive: Color = Color.Magenta,
    public val selectionControl: Color = Color.Magenta,
)

internal data class Link(
    public val visited: Color = Color.Magenta,
    public val inverse: Color = Color.Magenta,
    public val primary: Color = Color.Magenta,
)

internal data class Notifications(
    public val notificationInfo: Color = Color.Magenta,
    public val notificationError: Color = Color.Magenta,
    public val notificationWarning: Color = Color.Magenta,
    public val notificationSuccess: Color = Color.Magenta,
)

internal data class Border(
    public val disabled: Color = Color.Magenta,
    public val strong: Color = Color.Magenta,
    public val interactive: Color = Color.Magenta,
    public val subtleSelected: Color = Color.Magenta,
    public val subtle: Color = Color.Magenta,
    public val strongSelected: Color = Color.Magenta,
)

internal interface SemanticTokens {
    public val focus: Focus

    public val indicator: Indicator

    public val support: Support

    public val button: Button

    public val text: Text

    public val background: Background

    public val icon: Icon

    public val components: Components

    public val link: Link

    public val notifications: Notifications

    public val border: Border
}
