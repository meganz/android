package mega.privacy.android.shared.original.core.ui.controls.lists

import mega.privacy.android.icon.pack.R as IconPackR
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.controls.images.ThumbnailView
import mega.privacy.android.shared.original.core.ui.controls.text.HighlightedText
import mega.privacy.android.shared.original.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * Generic two line list item
 *
 * Node list item
 *
 * @param title Title
 * @param subtitle Subtitle
 * @param description Description
 * @param icon Icon
 * @param modifier Modifier
 * @param thumbnailData Thumbnail data
 * @param accessPermissionIcon Access permission icon
 * @param titleOverflow Title overflow
 * @param subTitleOverflow Subtitle overflow
 * @param showOffline Show offline
 * @param showVersion Show version
 * @param showChecked Show checked
 * @param isSelected Is selected
 * @param showIsVerified Show is verified
 * @param isTakenDown Is taken down
 * @param labelColor Label color
 * @param showLink Show link
 * @param showFavourite Show favourite
 * @param onMoreClicked On more clicked
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NodeListViewItem(
    title: String,
    subtitle: String,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    description: String? = null,
    thumbnailData: Any? = null,
    titleColor: TextColor = TextColor.Primary,
    subtitleColor: TextColor = TextColor.Secondary,
    @DrawableRes accessPermissionIcon: Int? = null,
    titleOverflow: LongTextBehaviour = LongTextBehaviour.MiddleEllipsis,
    subTitleOverflow: LongTextBehaviour = LongTextBehaviour.Clip(),
    highlightText: String = "",
    showOffline: Boolean = false,
    showVersion: Boolean = false,
    showChecked: Boolean = false,
    isSelected: Boolean = false,
    showIsVerified: Boolean = false,
    isTakenDown: Boolean = false,
    labelColor: Color? = null,
    showLink: Boolean = false,
    showFavourite: Boolean = false,
    isSensitive: Boolean = false,
    onMoreClicked: (() -> Unit)? = null,
    onInfoClicked: (() -> Unit)? = null,
    onItemClicked: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    GenericThreeLineListItem(
        modifier = modifier
            .alpha(1f.takeIf { !isSensitive } ?: 0.5f)
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = {
                    onItemClicked?.invoke()
                }
            ),
        fillSubTitleText = showIsVerified.not(),
        icon = {
            AnimatedContent(targetState = isSelected, label = "node thumbnail") {
                if (it) {
                    Image(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .testTag(SELECTED_TEST_TAG),
                        painter = painterResource(R.drawable.ic_select_folder),
                        contentDescription = "Selected",
                    )
                } else {
                    ThumbnailView(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .blur(16.dp.takeIf { isSensitive } ?: 0.dp)
                            .testTag(ICON_TAG),
                        data = thumbnailData,
                        defaultImage = icon,
                        contentDescription = "Thumbnail",
                    )
                }
            }
        },
        title = {
            if (highlightText.isNotBlank()) {
                HighlightedText(
                    text = title,
                    highlightText = highlightText,
                    textColor = if (isTakenDown) TextColor.Error else titleColor,
                    modifier = Modifier.testTag(TITLE_TAG),
                )
            } else {
                MegaText(
                    text = title,
                    overflow = titleOverflow,
                    textColor = if (isTakenDown) TextColor.Error else titleColor,
                    modifier = Modifier.testTag(TITLE_TAG),
                )
            }
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
                    tint = MegaOriginalTheme.colors.icon.accent,
                    modifier = Modifier
                        .size(16.dp)
                        .testTag(VERSION_ICON_TAG)
                )
            }
        },
        subtitle = {
            MegaText(
                text = subtitle,
                textColor = subtitleColor,
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
            if (showIsVerified) {
                Image(
                    modifier = Modifier
                        .size(16.dp)
                        .testTag(OFFLINE_ICON_TAG),
                    painter = painterResource(id = IconPackR.drawable.ic_contact_verified),
                    contentDescription = "Verified",
                )
            }
        },
        description = {
            description?.let {
                if (highlightText.isNotBlank()) {
                    HighlightedText(
                        text = description,
                        highlightText = highlightText,
                        highlightBold = true,
                        textColor = subtitleColor,
                    )
                }
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
                    painter = painterResource(id = IconPackR.drawable.ic_more_vertical_medium_regular_outline),
                    contentDescription = "More",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onMoreClicked() }
                        .testTag(MORE_ICON_TAG)
                )
            } else {
                Spacer(modifier = Modifier.size(24.dp))
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
private fun GenericNodeListViewItemSimplePreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        NodeListViewItem(
            title = "Simple title",
            subtitle = "Simple sub title",
            icon = IconPackR.drawable.ic_folder_sync_medium_solid
        )
    }
}

@CombinedThemePreviews
@Composable
private fun GenericNodeListViewItemHighlightPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        NodeListViewItem(
            title = "Simple title highlight",
            highlightText = "TITLE",
            subtitle = "Simple sub title",
            icon = IconPackR.drawable.ic_folder_sync_medium_solid
        )
    }
}

@CombinedThemePreviews
@Composable
private fun GenericNodeListItemWithLongTitlePreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        NodeListViewItem(
            title = "Title very big for testing the middle ellipsis",
            subtitle = "Subtitle very big for testing the middle ellipsis",
            icon = IconPackR.drawable.ic_folder_incoming_medium_solid,
            onMoreClicked = { },
            showOffline = true,
            showVersion = true,
            showFavourite = true,
            showLink = true,
            labelColor = MegaOriginalTheme.colors.indicator.pink,
            thumbnailData = "https://www.mega.com/resources/images/mega-logo.svg"
        )
    }
}

@CombinedThemePreviews
@Composable
private fun GenericNodeListItemPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        NodeListViewItem(
            title = "Title",
            subtitle = "Subtitle",
            icon = IconPackR.drawable.ic_folder_outgoing_medium_solid,
            onMoreClicked = { },
            accessPermissionIcon = R.drawable.ic_sync,
            showOffline = true,
            showVersion = true,
            showFavourite = true,
            showLink = true,
            labelColor = MegaOriginalTheme.colors.indicator.pink,
            thumbnailData = "https://www.mega.com/resources/images/mega-logo.svg"
        )
    }
}

@CombinedThemePreviews
@Composable
private fun GenericNodeListItemWithoutMoreOptionPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        NodeListViewItem(
            title = "Title",
            subtitle = "Subtitle",
            icon = IconPackR.drawable.ic_folder_outgoing_medium_solid,
            showFavourite = true,
            showLink = true,
            showChecked = true,
            onInfoClicked = { },
            onMoreClicked = { },
            accessPermissionIcon = R.drawable.ic_sync,
            labelColor = MegaOriginalTheme.colors.indicator.pink,
            showIsVerified = true,
            isTakenDown = true,
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
internal const val SELECTED_TEST_TAG = "node_list_view_item:image_selected"

