package mega.privacy.android.core.nodecomponents.list.view


import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.checkbox.Checkbox
import mega.android.core.ui.components.chip.HighlightChip
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.image.ThumbnailView
import mega.android.core.ui.components.list.GenericListItem
import mega.android.core.ui.components.text.HighlightedText
import mega.android.core.ui.components.util.normalize
import mega.android.core.ui.modifiers.conditional
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.SupportColor
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R

/**
 * Node list item
 *
 * @param title Title of the item
 * @param subtitle Subtitle of the item
 * @param icon Icon resource ID to display
 * @param modifier [Modifier] to be applied to the item
 * @param description Optional description text to display below the subtitle
 * @param tags Optional list of tags to display below the description
 * @param thumbnailData Data for the thumbnail, can be a URL or any other data type supported by ThumbnailView
 * @param titleColor Color for the title text
 * @param subtitleColor Color for the subtitle text
 * @param accessPermissionIcon Optional icon resource ID for access permission
 * @param highlightText Text to highlight in the title and description
 * @param showOffline if true, shows an offline icon
 * @param showVersion if true, shows a version icon
 * @param isSelected if true, the item is selected
 * @param isInSelectionMode if true, the item is in selection mode
 * @param showIsVerified if true, shows a verified icon
 * @param isTakenDown if true, shows a taken down icon
 * @param labelColor Optional color for a label circle next to the title
 * @param showLink if true, shows a link icon
 * @param showFavourite if true, shows a favourite icon
 * @param isSensitive if true, the item is considered sensitive and will be displayed with reduced opacity
 * @param showBlurEffect if true, applies a blur effect to the thumbnail when the item is sensitive
 * @param isHighlighted if true, the background will be highlighted with a different color to make the item stand out above the others
 * @param onMoreClicked Callback for when the more options icon is clicked
 * @param onInfoClicked Callback for when the info icon is clicked
 * @param onMoreClicked On more clicked
 */
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
    highlightText: String = "",
    showOffline: Boolean = false,
    showVersion: Boolean = false,
    isSelected: Boolean = false,
    isInSelectionMode: Boolean = false,
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
    onItemClicked: () -> Unit,
    onLongClicked: (() -> Unit)? = null,
) {
    GenericListItem(
        modifier = modifier
            .alpha(1f.takeIf { !isSensitive } ?: 0.5f)
            .conditional(isHighlighted) {
                background(DSTokens.colors.background.surface2)
            },
        contentPadding = PaddingValues(
            horizontal = DSTokens.spacings.s4,
            vertical = DSTokens.spacings.s3
        ),
        leadingElement = {
            ThumbnailView(
                modifier = Modifier
                    .size(32.dp)
                    .clip(DSTokens.shapes.medium)
                    .testTag(ICON_TAG),
                data = thumbnailData,
                defaultImage = icon,
                contentDescription = "Thumbnail",
                onSuccess = { modifier ->
                    if (!showBlurEffect) {
                        modifier.size(32.dp)
                    } else {
                        modifier
                            .size(32.dp)
                            .blur(16.dp.takeIf { isSensitive } ?: 0.dp)
                    }
                }
            )
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DSTokens.spacings.s2),
                modifier = Modifier.padding(bottom = DSTokens.spacings.s1)
            ) {
                if (labelColor != null) {
                    Circle(
                        color = labelColor,
                        modifier = Modifier.testTag(LABEL_TAG)
                    )
                }
                if (highlightText.isNotBlank()) {
                    HighlightedText(
                        text = title,
                        highlightText = highlightText,
                        textColor = if (isTakenDown) TextColor.Error else titleColor,
                        style = AppTheme.typography.bodyLarge,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .testTag(TITLE_TAG),
                    )
                } else {
                    MegaText(
                        text = title,
                        overflow = TextOverflow.MiddleEllipsis,
                        maxLines = 1,
                        textColor = if (isTakenDown) TextColor.Error else titleColor,
                        style = AppTheme.typography.bodyLarge,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .testTag(TITLE_TAG),
                    )
                }
                if (showFavourite) {
                    MegaIcon(
                        imageVector = IconPack.Small.Thin.Solid.Heart,
                        tint = IconColor.Secondary,
                        contentDescription = "Favourite",
                        modifier = Modifier
                            .size(16.dp)
                            .testTag(FAVOURITE_ICON_TAG)
                    )
                }
                if (showLink) {
                    MegaIcon(
                        imageVector = IconPack.Medium.Thin.Outline.Link01,
                        contentDescription = "Link",
                        modifier = Modifier
                            .size(16.dp)
                            .testTag(LINK_ICON_TAG),
                        tint = IconColor.Secondary
                    )
                }
                if (isTakenDown) {
                    MegaIcon(
                        imageVector = IconPack.Medium.Thin.Outline.AlertTriangle,
                        contentDescription = "Dispute taken down",
                        modifier = Modifier
                            .size(16.dp)
                            .testTag(TAKEN_DOWN_ICON_TAG),
                        tint = SupportColor.Error
                    )
                }
            }
        },
        subtitle = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DSTokens.spacings.s1),
            ) {
                if (showVersion) {
                    MegaIcon(
                        imageVector = IconPack.Medium.Thin.Outline.ClockRotate,
                        tint = IconColor.Secondary,
                        contentDescription = "Version",
                        modifier = Modifier
                            .size(16.dp)
                            .testTag(VERSION_ICON_TAG)
                    )
                }
                MegaText(
                    text = subtitle,
                    textColor = subtitleColor,
                    overflow = TextOverflow.Clip,
                    style = AppTheme.typography.bodySmall,
                    maxLines = 1,
                    modifier = Modifier.testTag(SUBTITLE_TAG),
                )
                if (showOffline) {
                    MegaIcon(
                        imageVector = IconPack.Medium.Thin.Outline.ArrowDownCircle,
                        contentDescription = "Offline",
                        modifier = Modifier
                            .size(16.dp)
                            .testTag(OFFLINE_ICON_TAG),
                        tint = IconColor.Secondary
                    )
                }
                if (showIsVerified) {
                    MegaIcon(
                        imageVector = IconPack.Medium.Thin.Outline.CheckCircle,
                        contentDescription = "Verified",
                        modifier = Modifier
                            .size(16.dp)
                            .testTag(VERIFIED_ICON_TAG),
                        tint = IconColor.Secondary
                    )
                }
            }
            if (description != null && highlightText.isNotBlank()) {
                val normalizedHighlight = remember(highlightText) { highlightText.normalize() }
                val normalizedDescription = remember(description) { description.normalize() }
                if (normalizedDescription.contains(normalizedHighlight, ignoreCase = true)) {
                    HighlightedText(
                        modifier = Modifier
                            .testTag(DESCRIPTION_TAG)
                            .padding(vertical = DSTokens.spacings.s2),
                        text = description,
                        highlightText = highlightText,
                        highlightFontWeight = FontWeight.Bold,
                        textColor = subtitleColor,
                        style = AppTheme.typography.bodySmall,
                    )
                }
            }
            if (highlightText.isNotBlank() && tags != null) {
                TagsRow(
                    tags = tags,
                    highlightText = highlightText,
                    modifier = Modifier.testTag(TAGS_TAG),
                )
            }
        },
        trailingElement = {
            if (accessPermissionIcon != null) {
                MegaIcon(
                    modifier = Modifier
                        .size(24.dp)
                        .testTag(PERMISSION_ICON_TAG),
                    painter = painterResource(id = accessPermissionIcon),
                    contentDescription = "Access permission",
                )
            }
            if (onInfoClicked != null) {
                MegaIcon(
                    imageVector = IconPack.Medium.Thin.Outline.Info,
                    contentDescription = "Info",
                    tint = IconColor.Secondary,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onInfoClicked() }
                        .testTag(INFO_ICON_TAG)
                )
            }
            AnimatedContent(targetState = isInSelectionMode, label = "node thumbnail") {
                if (it) {
                    Checkbox(
                        checked = isSelected,
                        onCheckStateChanged = { },
                        tapTargetArea = false,
                        clickable = false,
                        modifier = Modifier.testTag(CHECKBOX_TAG),
                    )
                } else {
                    if (onMoreClicked != null) {
                        MegaIcon(
                            imageVector = IconPack.Medium.Thin.Outline.MoreVertical,
                            contentDescription = "More",
                            tint = IconColor.Secondary,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { onMoreClicked() }
                                .testTag(MORE_ICON_TAG)
                        )
                    } else {
                        Spacer(modifier = Modifier.size(24.dp))
                    }
                }
            }
        },
        onClickListener = onItemClicked,
        onLongClickListener = onLongClicked
    )
}

@Composable
fun Circle(color: Color, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.size(7.dp),
        onDraw = {
            drawCircle(color = color)
        },
    )
}

/**
 * Tags row with highlight
 */
@Composable
fun TagsRow(
    tags: List<String>,
    highlightText: String,
    modifier: Modifier = Modifier,
    addSpacing: Boolean = false,
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
            horizontalArrangement = Arrangement.spacedBy(DSTokens.spacings.s3)
        ) {
            if (addSpacing) {
                Spacer(modifier = Modifier.width(DSTokens.spacings.s1))
            }
            matchingTags.forEach { tag ->
                HighlightChip(
                    text = "#$tag",
                    highlightText = tagHighlightText,
                )
            }
            if (addSpacing) {
                Spacer(modifier = Modifier.width(DSTokens.spacings.s1))
            }
        }
    }
}

internal const val TITLE_TAG = "node_list_view_item:title"
internal const val SUBTITLE_TAG = "node_list_view_item:subtitle"
internal const val ICON_TAG = "node_list_view_item:icon"
internal const val FAVOURITE_ICON_TAG = "node_list_view_item:favourite_icon"
internal const val LINK_ICON_TAG = "node_list_view_item:link_icon"
internal const val TAKEN_DOWN_ICON_TAG = "node_list_view_item:taken_down_icon"
internal const val OFFLINE_ICON_TAG = "node_list_view_item:offline_icon"
internal const val VERIFIED_ICON_TAG = "node_list_view_item:verified_icon"
internal const val VERSION_ICON_TAG = "node_list_view_item:version_icon"
internal const val PERMISSION_ICON_TAG = "node_list_view_item:permission_icon"
internal const val LABEL_TAG = "node_list_view_item:label"
internal const val MORE_ICON_TAG = "node_list_view_item:more_icon"
internal const val INFO_ICON_TAG = "node_list_view_item:info_icon"
internal const val CHECKBOX_TAG = "node_list_view_item:checkbox"
internal const val TAGS_TAG = "node_list_view_item:tags"
internal const val DESCRIPTION_TAG = "node_list_view_item:description"

@CombinedThemePreviews
@Composable
private fun GenericNodeListViewItemSimplePreview() {
    AndroidThemeForPreviews {
        NodeListViewItem(
            title = "Simple title",
            subtitle = "Simple sub title",
            icon = R.drawable.ic_folder_outgoing_medium_solid,
            onItemClicked = { },
            onMoreClicked = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun GenericNodeListViewItemSelectionModePreview() {
    AndroidThemeForPreviews {
        NodeListViewItem(
            title = "Simple title",
            subtitle = "8.1 MB • Feb 19, 2024",
            showLink = true,
            showOffline = true,
            showFavourite = true,
            showVersion = true,
            icon = R.drawable.ic_folder_outgoing_medium_solid,
            isInSelectionMode = true,
            onItemClicked = { },
            onMoreClicked = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun GenericNodeListViewItemSelectedModePreview() {
    AndroidThemeForPreviews {
        NodeListViewItem(
            title = "Simple title",
            subtitle = "Simple sub title",
            icon = R.drawable.ic_folder_outgoing_medium_solid,
            isInSelectionMode = true,
            isSelected = true,
            onItemClicked = { },
            onMoreClicked = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun GenericNodeListItemWithLongTitlePreview() {
    AndroidThemeForPreviews {
        NodeListViewItem(
            title = "Title very big for testing the middle ellipsis",
            subtitle = "Subtitle very big for testing the middle ellipsis",
            icon = R.drawable.ic_folder_incoming_medium_solid,
            onMoreClicked = { },
            showOffline = true,
            showVersion = true,
            showFavourite = true,
            showLink = true,
            labelColor = DSTokens.colors.indicator.pink,
            thumbnailData = "https://www.mega.com/resources/images/mega-logo.svg",
            onItemClicked = { }
        )
    }
}

@CombinedThemePreviews
@Composable
private fun GenericNodeListItemTakenDownPreview() {
    AndroidThemeForPreviews {
        NodeListViewItem(
            title = "Taken down file",
            subtitle = "8.1 MB • Feb 19, 2024",
            icon = R.drawable.ic_folder_incoming_medium_solid,
            onMoreClicked = { },
            showOffline = false,
            showVersion = false,
            showFavourite = false,
            showLink = false,
            isTakenDown = true,
            thumbnailData = "https://www.mega.com/resources/images/mega-logo.svg",
            onItemClicked = { }
        )
    }
}

@CombinedThemePreviews
@Composable
private fun NodeListItemWithAllFeaturesPreview() {
    AndroidThemeForPreviews {
        NodeListViewItem(
            title = "Complete Feature Demo",
            subtitle = "8.1 MB • Feb 19, 2024",
            description = "This item demonstrates all available features including description, tags, and various icons.",
            tags = listOf("demo", "complete", "features", "showcase"),
            icon = R.drawable.ic_folder_medium_solid,
            highlightText = "demo",
            showOffline = true,
            showVersion = true,
            showFavourite = true,
            showLink = true,
            showIsVerified = true,
            labelColor = DSTokens.colors.indicator.blue,
            thumbnailData = "https://www.mega.com/resources/images/mega-logo.svg",
            onMoreClicked = { },
            onInfoClicked = { },
            onItemClicked = { }
        )
    }
}

