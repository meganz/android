package mega.privacy.android.feature.sync.ui.views

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.SupportColor
import mega.android.core.ui.theme.values.TextColor

/**
 * Status line of a sync or issue card: an optional icon followed by the status text, both
 * tinted by [statusColor]. core-ui has no status-indicator component, so this composes the
 * same look from core-ui primitives.
 *
 * Status tints live in [SupportColor] while the neutral tint lives in [IconColor], and
 * [MegaIcon] takes them through separate overloads, hence the branch.
 *
 * @param statusText  Status text
 * @param modifier    Modifier
 * @param statusIcon  Status icon
 * @param statusColor Colour applied to [statusText] and [statusIcon]; secondary when null
 */
@Composable
internal fun SyncStatusIndicator(
    statusText: String,
    modifier: Modifier = Modifier,
    @DrawableRes statusIcon: Int? = null,
    statusColor: SupportColor? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        statusIcon?.let {
            val iconModifier = Modifier.size(16.dp)
            val painter = painterResource(id = it)
            if (statusColor != null) {
                MegaIcon(
                    painter = painter,
                    supportTint = statusColor,
                    contentDescription = null,
                    modifier = iconModifier,
                )
            } else {
                MegaIcon(
                    painter = painter,
                    tint = IconColor.Secondary,
                    contentDescription = null,
                    modifier = iconModifier,
                )
            }
        }
        MegaText(
            text = statusText,
            textColor = statusColor.toTextColor(),
            overflow = TextOverflow.Ellipsis,
            style = AppTheme.typography.titleSmall,
        )
    }
}

private fun SupportColor?.toTextColor(): TextColor = when (this) {
    SupportColor.Error -> TextColor.Error
    SupportColor.Success -> TextColor.Success
    SupportColor.Info -> TextColor.Info
    SupportColor.Warning -> TextColor.Warning
    null -> TextColor.Secondary
}
