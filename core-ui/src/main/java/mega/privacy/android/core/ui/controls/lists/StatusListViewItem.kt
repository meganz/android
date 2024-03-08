package mega.privacy.android.core.ui.controls.lists

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.tokens.TextColor

/**
 * Two line list item with status info
 *
 * @param icon                          Item icon
 * @param name                          Item name
 * @param statusText                    Status text
 * @param modifier                      Modifier
 * @param applySecondaryColorIconTint   If true, applies the secondary color to the icon
 * @param nameColor                     Item name color
 * @param statusIcon                    Status icon
 * @param statusColor                   Status color
 * @param onMoreClicked                 Action when "info" icon is clicked
 * @param onInfoClicked                 Action when "3-dos" icon is clicked
 */
@Composable
fun StatusListViewItem(
    @DrawableRes icon: Int,
    name: String,
    statusText: String,
    modifier: Modifier = Modifier,
    applySecondaryColorIconTint: Boolean = false,
    nameColor: TextColor = TextColor.Primary,
    @DrawableRes statusIcon: Int? = null,
    statusColor: StatusColor? = null,
    onMoreClicked: (() -> Unit)? = null,
    onInfoClicked: (() -> Unit)? = null,
) {
    GenericTwoLineListItem(
        modifier = modifier.padding(end = 8.dp),
        icon = {
            Image(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .testTag(ICON_TAG),
                painter = painterResource(id = icon),
                contentDescription = "Item icon",
                colorFilter = if (applySecondaryColorIconTint) {
                    ColorFilter.tint(MegaTheme.colors.icon.secondary)
                } else {
                    null
                }
            )
        },
        title = {
            MegaText(
                text = name,
                textColor = nameColor,
                modifier = Modifier.testTag(TITLE_TAG),
            )
        },
        subtitle = {
            DeviceCenterListViewItemStatus(
                statusText = statusText,
                statusIcon = statusIcon,
                statusColor = statusColor,
                modifier = Modifier.testTag(SUBTITLE_TAG),
            )
        },
        trailingIcons = {
            if (onInfoClicked != null) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_info),
                    contentDescription = "Info",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onInfoClicked() }
                        .testTag(MORE_ICON_TAG)
                )
            }
            if (onMoreClicked != null) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_more),
                    contentDescription = "More",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onMoreClicked() }
                        .testTag(MORE_ICON_TAG)
                )
            }
        }
    )
}

/**
 * Item status
 *
 * @param statusText    Status text
 * @param modifier      Modifier
 * @param statusIcon    Status icon
 * @param statusColor   Color to be applied for [statusText] and [statusIcon]
 */
@Composable
private fun DeviceCenterListViewItemStatus(
    statusText: String,
    modifier: Modifier = Modifier,
    @DrawableRes statusIcon: Int? = null,
    statusColor: StatusColor? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (statusIcon != null) {
            Icon(painter = painterResource(id = statusIcon),
                contentDescription = "Status Icon",
                tint = statusColor?.let {
                    getStatusColor(statusColor = statusColor)
                } ?: MegaTheme.colors.icon.secondary,
                modifier = Modifier.size(16.dp))
        }
        Text(
            text = statusText,
            color = statusColor?.let {
                getStatusColor(statusColor = statusColor)
            } ?: MegaTheme.colors.text.secondary,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.subtitle2,
        )
    }
}

/**
 * Gets the final [Color] for an [StatusColor]
 *
 * @param statusColor The status color
 * @return The corresponding color
 */
@Composable
private fun getStatusColor(statusColor: StatusColor): Color =
    when (statusColor) {
        StatusColor.Success -> MegaTheme.colors.support.success
        StatusColor.Info -> MegaTheme.colors.support.info
        StatusColor.Warning -> MegaTheme.colors.support.warning
        StatusColor.Error -> MegaTheme.colors.support.error
    }

/**
 * Status color
 */
enum class StatusColor {
    /**
     * Success: everything OK or up to date
     */
    Success,

    /**
     * Info: there is some info or something in progress
     */
    Info,

    /**
     * Warning: there is some alert
     */
    Warning,

    /**
     * Error: there is some final error
     */
    Error
}
