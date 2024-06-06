//
// Generated automatically by KotlinTokensGenerator.
// Do not modify this file manually.
//
package mega.privacy.android.shared.original.core.ui.theme.values

import mega.android.core.ui.tokens.theme.tokens.Background
import mega.android.core.ui.tokens.theme.tokens.Border
import mega.android.core.ui.tokens.theme.tokens.Button
import mega.android.core.ui.tokens.theme.tokens.Components
import mega.android.core.ui.tokens.theme.tokens.Focus
import mega.android.core.ui.tokens.theme.tokens.Icon
import mega.android.core.ui.tokens.theme.tokens.Indicator
import mega.android.core.ui.tokens.theme.tokens.Link
import mega.android.core.ui.tokens.theme.tokens.Notifications
import mega.android.core.ui.tokens.theme.tokens.SemanticTokens
import mega.android.core.ui.tokens.theme.tokens.Support
import mega.android.core.ui.tokens.theme.tokens.Text

internal object TempSemanticTokensLight : SemanticTokens {
    override val background: Background = Background(
            pageBackground = Colors.Base.white,
            inverse = Colors.Neutral.n700,
            surface1 = Colors.Neutral.n025,
            surface3 = Colors.Neutral.n100,
            surface2 = Colors.Neutral.n050,
            blur = Colors.BlackOpacity.n020,
            surfaceInverseAccent = Colors.Accent.n700,
            )

    override val button: Button = Button(
            primary = Colors.Accent.n900,
            primaryPressed = Colors.Accent.n600,
            brand = Colors.Primary.n600,
            brandHover = Colors.Primary.n700,
            brandPressed = Colors.Primary.n800,
            secondaryPressed = Colors.Neutral.n200,
            outline = Colors.Accent.n900,
            outlineHover = Colors.Accent.n700,
            outlineBackgroundHover = Colors.BlackOpacity.n005,
            outlinePressed = Colors.Accent.n600,
            error = Colors.Error.n600,
            errorHover = Colors.Error.n700,
            errorPressed = Colors.Error.n800,
            disabled = Colors.BlackOpacity.n010,
            secondary = Colors.Neutral.n050,
            primaryHover = Colors.Accent.n700,
            secondaryHover = Colors.Neutral.n100,
            )

    override val border: Border = Border(
            interactive = Colors.Primary.n600,
            strong = Colors.Neutral.n100,
            strongSelected = Colors.Accent.n900,
            subtle = Colors.Neutral.n050,
            subtleSelected = Colors.Accent.n900,
            disabled = Colors.Neutral.n100,
            )

    override val text: Text = Text(
            primary = Colors.Neutral.n800,
            secondary = Colors.Neutral.n600,
            accent = Colors.Accent.n900,
            placeholder = Colors.Neutral.n600,
            inverseAccent = Colors.Accent.n025,
            onColor = Colors.Neutral.n025,
            onColorDisabled = Colors.Neutral.n300,
            error = Colors.Error.n600,
            success = Colors.Success.n700,
            info = Colors.Secondary.Blue.n700,
            warning = Colors.Warning.n700,
            disabled = Colors.Neutral.n200,
            inverse = Colors.Neutral.n025,
            )

    override val icon: Icon = Icon(
            primary = Colors.Neutral.n800,
            secondary = Colors.Neutral.n600,
            accent = Colors.Accent.n900,
            inverseAccent = Colors.Accent.n025,
            onColor = Colors.Neutral.n025,
            onColorDisabled = Colors.Neutral.n300,
            inverse = Colors.Neutral.n025,
            disabled = Colors.Neutral.n200,
            )

    override val support: Support = Support(
            success = Colors.Success.n600,
            warning = Colors.Warning.n500,
            error = Colors.Error.n600,
            info = Colors.Secondary.Blue.n500,
            )

    override val components: Components = Components(
            selectionControl = Colors.Accent.n900,
            interactive = Colors.Primary.n600,
            toastBackground = Colors.Neutral.n700,
            )

    override val notifications: Notifications = Notifications(
            notificationSuccess = Colors.Success.n100,
            notificationWarning = Colors.Warning.n100,
            notificationError = Colors.Error.n100,
            notificationInfo = Colors.Secondary.Blue.n100,
            )

    override val indicator: Indicator = Indicator(
            pink = Colors.Error.n500,
            yellow = Colors.Warning.n500,
            green = Colors.Success.n500,
            blue = Colors.Secondary.Blue.n500,
            indigo = Colors.Secondary.Indigo.n500,
            magenta = Colors.Secondary.Magenta.n500,
            orange = Colors.Secondary.Orange.n500,
            )

    override val link: Link = Link(
            primary = Colors.Secondary.Indigo.n600,
            inverse = Colors.Secondary.Indigo.n400,
            visited = Colors.Secondary.Indigo.n900,
            )

    override val focus: Focus = Focus(
            colorFocus = Colors.Secondary.Indigo.n200,
            )
}
