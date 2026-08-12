package mega.privacy.android.feature.sync.ui.views

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.SupportColor
import mega.android.core.ui.theme.values.TextColor

/**
 * A sync card footer action: icon stacked above its label.
 *
 * core-ui's [mega.android.core.ui.components.button.AccessoryBarButton] lays the icon beside
 * the label, adds dividers and a surface background, and its multi-button overload is private,
 * so it cannot express this row of three stacked actions.
 *
 * @param icon      Button icon
 * @param text      Button label
 * @param onClick   Action to perform on tap
 * @param modifier  Modifier
 * @param iconColor Status tint for the icon; the primary icon colour when null
 * @param textColor Label colour
 * @param enabled   Whether the action is enabled
 */
@Composable
internal fun SyncCardActionButton(
    @DrawableRes icon: Int,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconColor: SupportColor? = null,
    textColor: TextColor = TextColor.Primary,
    enabled: Boolean = true,
) {
    Column(
        modifier = modifier.clickable(enabled = enabled) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val iconModifier = Modifier
            .padding(bottom = 8.dp)
            .size(16.dp)
        val painter = painterResource(icon)
        when {
            !enabled -> MegaIcon(
                painter = painter,
                tint = IconColor.Disabled,
                contentDescription = null,
                modifier = iconModifier,
            )

            iconColor != null -> MegaIcon(
                painter = painter,
                supportTint = iconColor,
                contentDescription = null,
                modifier = iconModifier,
            )

            else -> MegaIcon(
                painter = painter,
                tint = IconColor.Primary,
                contentDescription = null,
                modifier = iconModifier,
            )
        }
        MegaText(
            text = text,
            textColor = if (enabled) textColor else TextColor.Disabled,
            style = AppTheme.typography.bodySmall,
        )
    }
}
