package mega.privacy.android.core.ui.controls.lists

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.status.MegaStatusIndicator
import mega.privacy.android.core.ui.controls.status.StatusColor
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
            MegaStatusIndicator(
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


