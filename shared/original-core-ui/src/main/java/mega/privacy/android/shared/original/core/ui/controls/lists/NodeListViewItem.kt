package mega.privacy.android.shared.original.core.ui.controls.lists

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.core.R
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.original.core.ui.controls.chip.HighlightChip
import mega.privacy.android.shared.original.core.ui.controls.images.ThumbnailView
import mega.privacy.android.shared.original.core.ui.controls.text.HighlightedText
import mega.privacy.android.shared.original.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemeComponentPreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.utils.normalize

/**
 * Generic two line list item
 *
 * Node list item
 *
 * @param title Title
 * @param subtitle Subtitle
 * @param description Description
 * @param tags Tags
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
 * @param isHighlighted if true, the background will be highlighted with a different color to make the item stand out above the others
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
    tags: List<String>? = null,
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
    showBlurEffect: Boolean = false,
    isHighlighted: Boolean = false,
    onMoreClicked: (() -> Unit)? = null,
    onInfoClicked: (() -> Unit)? = null,
    onItemClicked: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    GenericMultilineListItem(
        modifier = modifier
            .alpha(1f.takeIf { !isSensitive } ?: 0.5f)
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = {
                    onItemClicked?.invoke()
                }
            )
            .then(
                if (isHighlighted) Modifier.background(DSTokens.colors.background.surface2)
                else Modifier
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
                            .testTag(ICON_TAG),
                        data = thumbnailData,
                        defaultImage = icon,
                        contentDescription = "Thumbnail",
                        onSuccess = { modifier ->
                            if (!showBlurEffect) {
                                modifier.size(48.dp)
                            } else {
                                modifier
                                    .size(48.dp)
                                    .blur(16.dp.takeIf { isSensitive } ?: 0.dp)
                            }
                        }
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
            if (isTakenDown) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_alert_triangle),
                    contentDescription = "Dispute taken down",
                    modifier = Modifier.testTag(TAKEN_DOWN_ICON_TAG),
                    tint = DSTokens.colors.support.error
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
                    tint = DSTokens.colors.icon.accent,
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
            if (description != null && highlightText.isNotBlank()) {
                val normalizedHighlight = remember(highlightText) { highlightText.normalize() }
                val normalizedDescription = remember(description) { description.normalize() }
                if (normalizedDescription.contains(normalizedHighlight, ignoreCase = true)) {
                    HighlightedText(
                        modifier = Modifier.testTag(DESCRIPTION_TAG),
                        text = description,
                        highlightText = highlightText,
                        highlightFontWeight = FontWeight.Bold,
                        textColor = subtitleColor,
                    )
                }
            }
        },
        customRow = {
            if (highlightText.isNotBlank() && tags != null) {
                TagsRow(
                    tags = tags,
                    highlightText = highlightText,
                    modifier = Modifier.testTag(TAGS_TAG),
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
                    painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.MoreVertical),
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

/**
 * Tags row with highlight
 */
@Composable
fun TagsRow(
    tags: List<String>,
    highlightText: String,
    addSpacing: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val tagHighlightText = remember(highlightText) {
        highlightText.removePrefix("#").normalize()
    }

    val matchingTags = remember(tags, tagHighlightText) {
        if (tagHighlightText.isNotBlank()) {
            tags.filter {
                it.normalize().contains(tagHighlightText, ignoreCase = true)
            }
        } else {
            emptyList()
        }
    }
    if (matchingTags.isNotEmpty()) {
        Row(
            modifier = modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (addSpacing) {
                Spacer(modifier = Modifier.width(2.dp))
            }
            matchingTags.forEach { tag ->
                HighlightChip(
                    text = "#$tag",
                    highlightText = tagHighlightText,
                )
            }
            if (addSpacing) {
                Spacer(modifier = Modifier.width(2.dp))
            }
        }
    }
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

@CombinedThemeComponentPreviews
@Composable
private fun GenericNodeListViewItemSimplePreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        NodeListViewItem(
            title = "Simple title",
            subtitle = "Simple sub title",
            icon = IconPackR.drawable.ic_folder_sync_medium_solid
        )
    }
}

@CombinedThemeComponentPreviews
@Composable
private fun GenericNodeListViewItemHighlightPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        NodeListViewItem(
            title = "Simple title highlight",
            highlightText = "TITLE",
            subtitle = "Simple sub title",
            icon = IconPackR.drawable.ic_folder_sync_medium_solid
        )
    }
}

@CombinedThemeComponentPreviews
@Composable
private fun GenericNodeListItemWithLongTitlePreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        NodeListViewItem(
            title = "Title very big for testing the middle ellipsis",
            subtitle = "Subtitle very big for testing the middle ellipsis",
            icon = IconPackR.drawable.ic_folder_incoming_medium_solid,
            onMoreClicked = { },
            showOffline = true,
            showVersion = true,
            showFavourite = true,
            showLink = true,
            labelColor = DSTokens.colors.indicator.pink,
            thumbnailData = "https://www.mega.com/resources/images/mega-logo.svg"
        )
    }
}

@CombinedThemeComponentPreviews
@Composable
private fun GenericNodeListItemPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
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
            labelColor = DSTokens.colors.indicator.pink,
            thumbnailData = "https://www.mega.com/resources/images/mega-logo.svg"
        )
    }
}

@CombinedThemeComponentPreviews
@Composable
private fun GenericNodeListItemWithoutMoreOptionPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
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
            labelColor = DSTokens.colors.indicator.pink,
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
internal const val TAKEN_DOWN_ICON_TAG = "node_list_view_item:taken_down_icon"
internal const val OFFLINE_ICON_TAG = "node_list_view_item:offline_icon"
internal const val VERSION_ICON_TAG = "node_list_view_item:version_icon"
internal const val PERMISSION_ICON_TAG = "node_list_view_item:permission_icon"
internal const val LABEL_TAG = "node_list_view_item:label"
internal const val MORE_ICON_TAG = "node_list_view_item:more_icon"
internal const val SELECTED_TEST_TAG = "node_list_view_item:image_selected"
internal const val TAGS_TAG = "node_list_view_item:tags"
internal const val DESCRIPTION_TAG = "node_list_view_item:description"


