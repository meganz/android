package mega.privacy.mobile.home.presentation.home.widget.recents.view

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.SpannedText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.nodecomponents.list.NodeLabelCircle
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.mobile.home.presentation.home.widget.recents.model.RecentActionUiItem
import mega.privacy.mobile.home.presentation.home.widget.recents.model.text


/**
 * Composable for the recent actions list item
 *
 * @param item The RecentActionUiItem to display
 * @param isExpanded Whether the media bucket is expanded in the list
 * @param onMenuClicked Callback when the menu button is clicked
 * @param onItemClicked Callback when the item is clicked
 */
@Composable
fun RecentActionListItemView(
    item: RecentActionUiItem,
    isExpanded: Boolean = false,
    onMenuClicked: () -> Unit,
    onItemClicked: () -> Unit,
) {
    val titleText = item.title.text()
    val parentFolderNameText = item.parentFolderName.text
    val timeText = item.timestampText.formatTime()

    RecentActionListItemView(
        title = titleText,
        parentFolderName = parentFolderNameText,
        time = timeText,
        icon = item.icon,
        shareIcon = item.shareIcon,
        updatedByText = item.updatedByText?.text,
        label = item.nodeLabel,
        showFavourite = item.isFavourite,
        showVersion = item.isUpdate,
        isMediaBucket = item.isMediaBucket,
        isExpanded = isExpanded,
        onMenuClicked = onMenuClicked,
        onItemClicked = onItemClicked,
    )
}

/**
 * Composable for the recent actions list item (backward compatibility)
 *
 * @param title The title of the item
 * @param icon The icon resource ID of the item
 * @param shareIcon The share icon resource ID of the item
 * @param parentFolderName The name of the parent folder
 * @param time The time of the action
 * @param updatedByText The text indicating who updated/added the item
 * @param label The label of the item
 * @param showFavourite Whether to show the favourite icon
 * @param showVersion Whether to show the version icon
 * @param isSensitive Whether the item is sensitive
 * @param isMediaBucket Whether the item is a media bucket (e.g. multi images)
 * @param isExpanded Whether the media bucket is expanded in the list
 * @param onMenuClicked Callback when the menu button is clicked
 * @param onItemClicked Callback when the item is clicked
 */
@Composable
fun RecentActionListItemView(
    title: String,
    parentFolderName: String,
    time: String,
    @DrawableRes icon: Int = IconPackR.drawable.ic_generic_medium_solid,
    @DrawableRes shareIcon: Int? = null,
    updatedByText: String? = null,
    label: NodeLabel? = null,
    showFavourite: Boolean = false,
    showVersion: Boolean = false,
    isSensitive: Boolean = false,
    isMediaBucket: Boolean = false,
    isExpanded: Boolean = false,
    onMenuClicked: () -> Unit,
    onItemClicked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onItemClicked()
            }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .alpha(1f.takeIf { !isSensitive } ?: 0.5f),
    ) {
        Image(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .align(Alignment.CenterVertically)
                .testTag(ICON_TEST_TAG),
            painter = painterResource(icon),
            contentDescription = "Thumbnail"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                label?.let {
                    NodeLabelCircle(
                        label = it,
                        modifier = Modifier
                            .testTag(LABEL_TEST_TAG),
                    )
                }

                MegaText(
                    text = title,
                    textColor = TextColor.Primary,
                    modifier = Modifier.testTag(FIRST_LINE_TEST_TAG),
                    style = AppTheme.typography.bodyMedium,
                )

                if (showFavourite) {
                    MegaIcon(
                        imageVector = IconPack.Small.Thin.Solid.Heart,
                        tint = IconColor.Secondary,
                        contentDescription = "Favourite",
                        modifier = Modifier
                            .size(16.dp)
                            .testTag(FAVORITE_TEST_TAG)
                    )
                }
            }

            updatedByText?.let { text ->
                SpannedText(
                    value = text,
                    baseTextColor = TextColor.Secondary,
                    baseStyle = MaterialTheme.typography.bodySmall,
                    spanStyles = mapOf(
                        SpanIndicator('A') to MegaSpanStyle.TextColorStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Normal,
                            ),
                            textColor = TextColor.Secondary
                        ),
                        SpanIndicator('B') to MegaSpanStyle.TextColorStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Normal,
                            ),
                            textColor = TextColor.Primary
                        )
                    ),
                    overflow = TextOverflow.MiddleEllipsis,
                    maxLines = 1,
                    modifier = Modifier
                        .testTag(SECOND_LINE_TEST_TAG)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                shareIcon?.let { icon ->
                    Image(
                        modifier = Modifier
                            .size(24.dp)
                            .testTag(SHARES_ICON_TEST_TAG),
                        painter = painterResource(id = icon),
                        contentDescription = "Shares"
                    )
                }

                MegaText(
                    text = parentFolderName,
                    textColor = TextColor.Secondary,
                    style = AppTheme.typography.bodySmall,
                    overflow = TextOverflow.MiddleEllipsis,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f, false)
                        .testTag(FOLDER_NAME_TEST_TAG),
                )

                MegaIcon(
                    imageVector = if (showVersion) {
                        IconPack.Small.Thin.Outline.ClockRotate
                    } else {
                        IconPack.Small.Thin.Outline.ArrowUp
                    },
                    tint = IconColor.Secondary,
                    contentDescription = "Version",
                    modifier = Modifier
                        .size(16.dp)
                        .testTag(ACTION_ICON_TEST_TAG)
                )

                MegaText(
                    text = time,
                    textColor = TextColor.Secondary,
                    style = AppTheme.typography.bodySmall,
                    modifier = Modifier
                        .testTag(TIME_TEST_TAG),
                )
            }
        }

        if (!isMediaBucket) {
            MegaIcon(
                imageVector = IconPack.Medium.Thin.Outline.MoreVertical,
                contentDescription = "3 dots",
                tint = IconColor.Secondary,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(24.dp)
                    .clickable {
                        onMenuClicked()
                    }
                    .testTag(MENU_TEST_TAG)
            )
        } else {
            MegaIcon(
                imageVector = if (isExpanded) {
                    IconPack.Small.Thin.Outline.ChevronUp
                } else {
                    IconPack.Small.Thin.Outline.ChevronDown
                },
                contentDescription = "Dropdown",
                tint = IconColor.Secondary,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(24.dp)
                    .clickable {
                        onMenuClicked()
                    }
                    .testTag(MEDIA_BUCKET_MENU_TEST_TAG)
            )
        }
    }
}

internal const val ICON_TEST_TAG = "recent_action_list_item_view:icon"
internal const val FIRST_LINE_TEST_TAG = "recent_action_list_item_view:first_line"
internal const val SECOND_LINE_TEST_TAG = "recent_action_list_item_view:second_line"
internal const val LABEL_TEST_TAG = "recent_action_list_item_view:label"
internal const val FAVORITE_TEST_TAG = "recent_action_list_item_view:favorite"
internal const val FOLDER_NAME_TEST_TAG = "recent_action_list_item_view:folder_name"
internal const val SHARES_ICON_TEST_TAG = "recent_action_list_item_view:shares_icon"
internal const val ACTION_ICON_TEST_TAG = "recent_action_list_item_view:action_icon"
internal const val TIME_TEST_TAG = "recent_action_list_item_view:time"
internal const val MENU_TEST_TAG = "recent_action_list_item_view:menu"
internal const val MEDIA_BUCKET_MENU_TEST_TAG = "recent_action_list_item_view:media_bucket_menu"

@CombinedThemePreviews
@Composable
private fun RecentActionListItemViewPreview() {
    AndroidThemeForPreviews {
        RecentActionListItemView(
            title = "Invoice_October_reviewed.xlsx",
            icon = IconPackR.drawable.ic_spreadsheet_medium_solid,
            time = "12:00 PM",
            parentFolderName = "Cloud drive",
            onItemClicked = {},
            onMenuClicked = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun RecentActionListItemView2Preview() {
    AndroidThemeForPreviews {
        RecentActionListItemView(
            title = "Screen recording.mp4",
            updatedByText = "[A]added by[/A] [B]John Doe[/B]",
            icon = IconPackR.drawable.ic_video_medium_solid,
            shareIcon = IconPackR.drawable.ic_folder_incoming_medium_solid,
            time = "08:00",
            parentFolderName = "Tech Share",
            onItemClicked = {},
            onMenuClicked = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun RecentActionListItemViewUpdatePreview() {
    AndroidThemeForPreviews {
        RecentActionListItemView(
            title = "First line text",
            updatedByText = "[A]updated by[/A] [B]John Doe[/B]",
            showFavourite = true,
            showVersion = true,
            shareIcon = IconPackR.drawable.ic_folder_incoming_medium_solid,
            time = "08:00",
            parentFolderName = "Very Long Folder Name, Very Long Folder Name",
            onItemClicked = {},
            onMenuClicked = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun RecentActionListItemViewMultiFilePreview() {
    AndroidThemeForPreviews {
        RecentActionListItemView(
            title = "14 images",
            updatedByText = "[A]added by[/A] [B]John Doe[/B]",
            icon = IconPackR.drawable.ic_image_stack_medium_solid,
            time = "08:00",
            isMediaBucket = true,
            parentFolderName = "Very Long Folder Name, Very Long Folder Name",
            onItemClicked = {},
            onMenuClicked = {}
        )
    }
}