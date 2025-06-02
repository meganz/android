package mega.privacy.android.shared.original.core.ui.theme

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.runtime.Composable
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.values.BackgroundColor
import mega.android.core.ui.theme.values.ComponentsColor
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.LinkColor
import mega.android.core.ui.theme.values.SupportColor
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.android.core.ui.tokens.theme.colors.DSColors
import mega.privacy.android.shared.original.core.ui.theme.extensions.red_600_red_300
import mega.privacy.android.shared.original.core.ui.theme.shape.Shapes

/**
 * Android original theme to be used only in Previews.
 *
 * @param content
 */
@SuppressLint("IsSystemInDarkTheme")
@Composable
internal fun OriginalThemeForPreviews(
    content: @Composable () -> Unit,
) = OriginalTheme(
    isSystemInDarkTheme(),
    content = content,
)

/**
 * Android theme to be used by components in the original design system
 *
 * @param isDark
 * @param content
 */
@Composable
fun OriginalTheme(
    isDark: Boolean,
    content: @Composable () -> Unit,
) {
    val legacyColors = if (isDark) {
        LegacyDarkColorPalette
    } else {
        LegacyLightColorPalette
    }
    AndroidTheme(
        isDark = isDark,
        useLegacyStatusBarColor = false,
    ) {
        //we need to keep `MaterialTheme` for now as not all the components are migrated to our Design System.
        MaterialTheme(
            colors = legacyColors,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

@Composable
internal fun DSTokens.textColor(textColor: TextColor) =
    textColor.getTextColor(colors.text)

@Composable
internal fun DSTokens.iconColor(iconColor: IconColor) =
    iconColor.getIconColor(colors.icon)

@Composable
internal fun DSTokens.supportColor(supportColor: SupportColor) =
    supportColor.getSupportColor(colors.support)

@Composable
internal fun DSTokens.linkColor(linkColor: LinkColor) =
    linkColor.getLinkColor(colors.link)

@Composable
internal fun DSTokens.componentsColor(componentsColor: ComponentsColor) =
    componentsColor.getComponentsColor(colors.components)

@Composable
internal fun DSTokens.backgroundColor(backgroundColor: BackgroundColor) =
    backgroundColor.getBackgroundColor(colors.background)


internal val DSColors.raisedButtonColors
    @Composable
    get() = ButtonDefaults.buttonColors(
        backgroundColor = button.primary,
        contentColor = text.inverse,
        disabledBackgroundColor = button.disabled,
        disabledContentColor = text.disabled,
    )

internal val DSColors.raisedErrorButtonColors
    @Composable
    get() = ButtonDefaults.buttonColors(
        backgroundColor = MaterialTheme.colors.red_600_red_300,
        contentColor = text.inverse,
        disabledBackgroundColor = button.disabled,
        disabledContentColor = text.disabled,
    )

internal val DSColors.radioColors
    @Composable
    get() = RadioButtonDefaults.colors(
        selectedColor = DSTokens.colors.border.strongSelected,
        unselectedColor = DSTokens.colors.icon.secondary,
        disabledColor = DSTokens.colors.border.disabled,
    )
