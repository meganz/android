//
// Generated automatically by KotlinTokensGenerator.
// Do not modify this file manually.
//
package mega.privacy.android.shared.theme.tokens

import mega.privacy.android.core.ui.theme.tokens.Background
import mega.privacy.android.core.ui.theme.tokens.Border
import mega.privacy.android.core.ui.theme.tokens.Button
import mega.privacy.android.core.ui.theme.tokens.Components
import mega.privacy.android.core.ui.theme.tokens.Focus
import mega.privacy.android.core.ui.theme.tokens.Icon
import mega.privacy.android.core.ui.theme.tokens.Indicator
import mega.privacy.android.core.ui.theme.tokens.Link
import mega.privacy.android.core.ui.theme.tokens.Notifications
import mega.privacy.android.core.ui.theme.tokens.SemanticTokens
import mega.privacy.android.core.ui.theme.tokens.Support
import mega.privacy.android.core.ui.theme.tokens.Text

internal object MegaAppSemanticTokensLight : SemanticTokens {
    override val focus: Focus = Focus(
            colorFocus = Colors.Secondary.Indigo.n200,
            )

    override val indicator: Indicator = Indicator(
            magenta = Colors.Secondary.Magenta.n500,
            yellow = Colors.Warning.n500,
            orange = Colors.Secondary.Orange.n500,
            indigo = Colors.Secondary.Indigo.n500,
            blue = Colors.Secondary.Blue.n500,
            green = Colors.Success.n500,
            pink = Colors.Error.n500,
            )

    override val support: Support = Support(
            error = Colors.Error.n600,
            warning = Colors.Warning.n500,
            success = Colors.Success.n600,
            info = Colors.Secondary.Blue.n500,
            )

    override val button: Button = Button(
            disabled = Colors.BlackOpacity.n010,
            errorPressed = Colors.Error.n800,
            errorHover = Colors.Error.n700,
            error = Colors.Error.n600,
            outlineBackgroundHover = Colors.BlackOpacity.n005,
            outlineHover = Colors.Accent.n700,
            outline = Colors.Accent.n900,
            primaryHover = Colors.Accent.n700,
            secondaryPressed = Colors.Neutral.n200,
            brandPressed = Colors.Primary.n800,
            secondaryHover = Colors.Neutral.n100,
            secondary = Colors.Neutral.n050,
            brandHover = Colors.Primary.n700,
            brand = Colors.Primary.n600,
            primaryPressed = Colors.Accent.n600,
            outlinePressed = Colors.Accent.n600,
            primary = Colors.Accent.n900,
            )

    override val text: Text = Text(
            inverse = Colors.Neutral.n025,
            disabled = Colors.Neutral.n200,
            warning = Colors.Warning.n700,
            info = Colors.Secondary.Blue.n700,
            success = Colors.Success.n700,
            error = Colors.Error.n600,
            onColorDisabled = Colors.Neutral.n300,
            onColor = Colors.Neutral.n025,
            placeholder = Colors.Neutral.n600,
            accent = Colors.Accent.n900,
            secondary = Colors.Neutral.n600,
            primary = Colors.Neutral.n800,
            inverseAccent = Colors.Accent.n025,
            )

    override val background: Background = Background(
            blur = Colors.BlackOpacity.n020,
            surface2 = Colors.Neutral.n050,
            surface3 = Colors.Neutral.n100,
            surface1 = Colors.Neutral.n025,
            inverse = Colors.Neutral.n700,
            pageBackground = Colors.Base.white,
            )

    override val icon: Icon = Icon(
            disabled = Colors.Neutral.n200,
            inverse = Colors.Neutral.n025,
            onColorDisabled = Colors.Neutral.n300,
            onColor = Colors.Neutral.n025,
            inverseAccent = Colors.Accent.n025,
            accent = Colors.Accent.n900,
            secondary = Colors.Neutral.n600,
            primary = Colors.Neutral.n800,
            )

    override val components: Components = Components(
            toastBackground = Colors.Neutral.n700,
            interactive = Colors.Primary.n600,
            selectionControl = Colors.Accent.n900,
            )

    override val link: Link = Link(
            visited = Colors.Secondary.Indigo.n900,
            inverse = Colors.Secondary.Indigo.n400,
            primary = Colors.Secondary.Indigo.n600,
            )

    override val notifications: Notifications = Notifications(
            notificationInfo = Colors.Secondary.Blue.n100,
            notificationError = Colors.Error.n100,
            notificationWarning = Colors.Warning.n100,
            notificationSuccess = Colors.Success.n100,
            )

    override val border: Border = Border(
            disabled = Colors.Neutral.n100,
            strong = Colors.Neutral.n100,
            interactive = Colors.Primary.n600,
            subtleSelected = Colors.Accent.n900,
            subtle = Colors.Neutral.n050,
            strongSelected = Colors.Accent.n900,
            )
}
