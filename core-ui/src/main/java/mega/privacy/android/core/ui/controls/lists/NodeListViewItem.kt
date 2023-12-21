package mega.privacy.android.core.ui.controls.lists

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.images.ThumbnailView
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.tokens.TextColor

/**
 * Generic two line list item
 *
 * Node list item
 *
 * @param title Title
 * @param subtitle Subtitle
 * @param icon Icon
 * @param modifier Modifier
 * @param thumbnailData Thumbnail data
 * @param accessPermissionIcon Access permission icon
 * @param titleOverflow Title overflow
 * @param subTitleOverflow Subtitle overflow
 * @param showOffline Show offline
 * @param showVersion Show version
 * @param showChecked Show checked
 * @param labelColor Label color
 * @param showLink Show link
 * @param showFavourite Show favourite
 * @param onMoreClicked On more clicked
 */
@Composable
fun NodeListViewItem(
    title: String,
    subtitle: String,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    thumbnailData: Any? = null,
    @DrawableRes accessPermissionIcon: Int? = null,
    titleOverflow: LongTextBehaviour = LongTextBehaviour.Clip(),
    subTitleOverflow: LongTextBehaviour = LongTextBehaviour.Clip(),
    showOffline: Boolean = false,
    showVersion: Boolean = false,
    showChecked: Boolean = false,
    labelColor: Color? = null,
    showLink: Boolean = false,
    showFavourite: Boolean = false,
    onMoreClicked: (() -> Unit)? = null,
    onInfoClicked: (() -> Unit)? = null,
) {
    GenericTwoLineListItem(
        modifier = modifier,
        icon = {
            ThumbnailView(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .testTag(ICON_TAG),
                data = thumbnailData,
                defaultImage = icon,
                contentDescription = "Thumbnail",
            )
        },
        title = {
            MegaText(
                text = title,
                overflow = titleOverflow,
                textColor = TextColor.Primary,
                modifier = Modifier.testTag(TITLE_TAG),
            )
        },
        titleIcons = {
            if (labelColor != null) {
                Circle(color = labelColor, modifier = Modifier.testTag(LABEL_TAG))
            }
            if (showFavourite) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_favourite_small),
                    contentDescription = "Favourite",
                    modifier = Modifier.testTag(FAVOURITE_ICON_TAG)
                )
            }
            if (showLink) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_link_small),
                    contentDescription = "Link",
                    modifier = Modifier.testTag(LINK_ICON_TAG)
                )
            }
        },
        subTitlePrefixIcons = {
            if (showVersion) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_version_small),
                    contentDescription = "Version",
                    modifier = Modifier
                        .size(16.dp)
                        .testTag(VERSION_ICON_TAG)
                )
            }
            if (showChecked) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check_circle),
                    contentDescription = "Checked",
                    tint = MegaTheme.colors.icon.accent,
                    modifier = Modifier
                        .size(16.dp)
                        .testTag(VERSION_ICON_TAG)
                )
            }
        },
        subtitle = {
            MegaText(
                text = subtitle,
                textColor = TextColor.Secondary,
                overflow = subTitleOverflow,
                modifier = Modifier.testTag(SUBTITLE_TAG),
            )
        },
        subTitleSuffixIcons = {
            if (showOffline) {
                Icon(
                    modifier = Modifier
                        .size(16.dp)
                        .testTag(OFFLINE_ICON_TAG),
                    painter = painterResource(id = R.drawable.ic_offline_indicator),
                    contentDescription = "Offline",
                )
            }
        },
        trailingIcons = {
            if (accessPermissionIcon != null) {
                Icon(
                    modifier = Modifier
                        .size(24.dp)
                        .testTag(PERMISSION_ICON_TAG),
                    painter = painterResource(id = accessPermissionIcon),
                    contentDescription = "Access permission",
                )
            }
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

@Composable
private fun Circle(color: Color, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.size(8.dp),
        onDraw = {
            drawCircle(color = color)
        },
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewGenericTwoLineListViewItemSimple() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        NodeListViewItem(
            title = "Simple title",
            subtitle = "Simple sub title",
            icon = R.drawable.ic_folder_sync
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewGenericTwoLineListItemWithLongTitle() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        NodeListViewItem(
            title = "Title very big for testing the middle ellipsis",
            subtitle = "Subtitle very big for testing the middle ellipsis",
            icon = R.drawable.ic_folder_incoming,
            onMoreClicked = { },
            showOffline = true,
            showVersion = true,
            showFavourite = true,
            showLink = true,
            labelColor = MegaTheme.colors.indicator.pink,
            thumbnailData = "https://www.mega.com/resources/images/mega-logo.svg"
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewGenericTwoLineListItem() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        NodeListViewItem(
            title = "Title",
            subtitle = "Subtitle",
            icon = R.drawable.ic_folder_outgoing,
            onMoreClicked = { },
            accessPermissionIcon = R.drawable.ic_sync,
            showOffline = true,
            showVersion = true,
            showFavourite = true,
            showLink = true,
            labelColor = MegaTheme.colors.indicator.pink,
            thumbnailData = "https://www.mega.com/resources/images/mega-logo.svg"
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewGenericTwoLineListItemWithoutMoreOption() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        NodeListViewItem(
            title = "Title",
            subtitle = "Subtitle",
            icon = R.drawable.ic_folder_outgoing,
            showFavourite = true,
            showLink = true,
            showChecked = true,
            onInfoClicked = { },
            onMoreClicked = { },
            accessPermissionIcon = R.drawable.ic_sync,
            labelColor = MegaTheme.colors.indicator.pink,
            thumbnailData = "https://www.mega.com/resources/images/mega-logo.svg"
        )
    }
}

internal const val TITLE_TAG = "node_list_view_item:title"
internal const val SUBTITLE_TAG = "node_list_view_item:subtitle"
internal const val ICON_TAG = "node_list_view_item:icon"
internal const val FAVOURITE_ICON_TAG = "node_list_view_item:favourite_icon"
internal const val LINK_ICON_TAG = "node_list_view_item:link_icon"
internal const val OFFLINE_ICON_TAG = "node_list_view_item:offline_icon"
internal const val VERSION_ICON_TAG = "node_list_view_item:version_icon"
internal const val PERMISSION_ICON_TAG = "node_list_view_item:permission_icon"
internal const val LABEL_TAG = "node_list_view_item:label"
internal const val MORE_ICON_TAG = "node_list_view_item:more_icon"

