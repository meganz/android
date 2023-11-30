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

internal object MegaAppSemanticTokensDark : SemanticTokens {
    override val focus: Focus = Focus(
            colorFocus = Colors.Secondary.Indigo.n700,
            )

    override val indicator: Indicator = Indicator(
            magenta = Colors.Secondary.Magenta.n300,
            yellow = Colors.Warning.n400,
            orange = Colors.Secondary.Orange.n300,
            indigo = Colors.Secondary.Indigo.n300,
            blue = Colors.Secondary.Blue.n400,
            green = Colors.Success.n400,
            pink = Colors.Error.n400,
            )

    override val support: Support = Support(
            error = Colors.Error.n400,
            warning = Colors.Warning.n500,
            success = Colors.Success.n500,
            info = Colors.Secondary.Blue.n600,
            )

    override val button: Button = Button(
            disabled = Colors.WhiteOpacity.n010,
            errorPressed = Colors.Error.n300,
            errorHover = Colors.Error.n400,
            error = Colors.Error.n500,
            outlineBackgroundHover = Colors.WhiteOpacity.n005,
            outlineHover = Colors.Accent.n300,
            outline = Colors.Accent.n050,
            primaryHover = Colors.Accent.n300,
            secondaryPressed = Colors.Neutral.n500,
            brandPressed = Colors.Primary.n300,
            secondaryHover = Colors.Neutral.n600,
            secondary = Colors.Neutral.n700,
            brandHover = Colors.Primary.n400,
            brand = Colors.Primary.n500,
            primaryPressed = Colors.Accent.n200,
            outlinePressed = Colors.Accent.n200,
            primary = Colors.Accent.n050,
            )

    override val text: Text = Text(
            inverse = Colors.Neutral.n800,
            disabled = Colors.Neutral.n500,
            warning = Colors.Warning.n500,
            info = Colors.Secondary.Blue.n500,
            success = Colors.Success.n500,
            error = Colors.Error.n400,
            onColorDisabled = Colors.Neutral.n400,
            onColor = Colors.Neutral.n025,
            placeholder = Colors.Neutral.n200,
            accent = Colors.Accent.n025,
            secondary = Colors.Neutral.n300,
            primary = Colors.Neutral.n050,
            inverseAccent = Colors.Accent.n900,
            )

    override val background: Background = Background(
            blur = Colors.BlackOpacity.n020,
            surface2 = Colors.Neutral.n700,
            surface3 = Colors.Neutral.n600,
            surface1 = Colors.Neutral.n800,
            inverse = Colors.Neutral.n050,
            pageBackground = Colors.Neutral.n900,
            )

    override val icon: Icon = Icon(
            disabled = Colors.Neutral.n500,
            inverse = Colors.Neutral.n800,
            onColorDisabled = Colors.Neutral.n400,
            onColor = Colors.Neutral.n025,
            inverseAccent = Colors.Accent.n900,
            accent = Colors.Accent.n025,
            secondary = Colors.Neutral.n300,
            primary = Colors.Neutral.n050,
            )

    override val components: Components = Components(
            toastBackground = Colors.Neutral.n200,
            interactive = Colors.Primary.n400,
            selectionControl = Colors.Accent.n050,
            )

    override val link: Link = Link(
            visited = Colors.Secondary.Indigo.n100,
            inverse = Colors.Secondary.Indigo.n600,
            primary = Colors.Secondary.Indigo.n400,
            )

    override val notifications: Notifications = Notifications(
            notificationInfo = Colors.Secondary.Blue.n900,
            notificationError = Colors.Error.n900,
            notificationWarning = Colors.Warning.n800,
            notificationSuccess = Colors.Success.n900,
            )

    override val border: Border = Border(
            disabled = Colors.Neutral.n700,
            strong = Colors.Neutral.n600,
            interactive = Colors.Primary.n400,
            subtleSelected = Colors.Accent.n050,
            subtle = Colors.Neutral.n800,
            strongSelected = Colors.Accent.n050,
            )
}
