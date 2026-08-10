package mega.privacy.android.feature.photos.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.checkbox.Checkbox
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.GenericListItem
import mega.android.core.ui.modifiers.conditional
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.SupportColor
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailData
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R
import mega.privacy.android.shared.nodes.components.NodeThumbnailView
import mega.privacy.android.shared.nodes.components.ThumbnailLayoutType

@Composable
fun SelectVideoListItem(
    title: String,
    subtitle: String,
    @DrawableRes icon: Int,
    onItemClicked: () -> Unit,
    modifier: Modifier = Modifier,
    titleMaxLines: Int = 1,
    thumbnailData: ThumbnailData? = null,
    isSelected: Boolean = false,
    isAvailableSelected: Boolean = false,
    isSensitive: Boolean = false,
    isTakenDown: Boolean = false,
    titleTextStyle: TextStyle = AppTheme.typography.bodyLarge,
    titleColor: TextColor = TextColor.Primary,
    subtitleColor: TextColor = TextColor.Secondary,
    isEnabled: Boolean = true,
) {
    GenericListItem(
        modifier = modifier
            .alpha(if (isSensitive || !isEnabled) 0.5f else 1f)
            .conditional(isSelected) {
                background(DSTokens.colors.background.surface1)
            },
        enableClick = isEnabled,
        contentPadding = PaddingValues(
            horizontal = DSTokens.spacings.s4,
            vertical = DSTokens.spacings.s3
        ),
        leadingElement = {
            NodeThumbnailView(
                modifier = Modifier
                    .size(32.dp)
                    .testTag(SELECT_VIDEO_LIST_ITEM_ICON_TAG),
                layoutType = ThumbnailLayoutType.List,
                data = thumbnailData,
                defaultImage = icon,
                contentDescription = "Thumbnail",
                blurImage = isSensitive
            )
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DSTokens.spacings.s2),
                modifier = Modifier.padding(bottom = DSTokens.spacings.s1)
            ) {
                MegaText(
                    text = title,
                    overflow = TextOverflow.MiddleEllipsis,
                    maxLines = titleMaxLines,
                    textColor = if (isTakenDown) TextColor.Error else titleColor,
                    style = titleTextStyle,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding()
                        .testTag(SELECT_VIDEO_LIST_ITEM_TITLE_TAG),
                )

                if (isTakenDown) {
                    MegaIcon(
                        imageVector = IconPack.Medium.Thin.Outline.AlertTriangle,
                        contentDescription = "Dispute taken down",
                        modifier = Modifier
                            .size(16.dp)
                            .testTag(SELECT_VIDEO_LIST_ITEM_TAKEN_DOWN_ICON_TAG),
                        tint = SupportColor.Error
                    )
                }
            }

        },
        subtitle = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                MegaText(
                    text = subtitle,
                    textColor = subtitleColor,
                    overflow = TextOverflow.Ellipsis,
                    style = AppTheme.typography.bodySmall,
                    maxLines = 1,
                    modifier = Modifier.testTag(SELECT_VIDEO_LIST_ITEM_SUBTITLE_TAG),
                )
            }
        },
        trailingElement = {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isAvailableSelected) {
                    Checkbox(
                        checked = isSelected,
                        onCheckStateChanged = { },
                        tapTargetArea = false,
                        clickable = false,
                        modifier = Modifier.testTag(SELECT_VIDEO_LIST_ITEM_SELECTED_ICON_TAG),
                    )
                }
            }
        },
        onClickListener = onItemClicked,
    )
}

@CombinedThemePreviews
@Composable
private fun GenericSelectVideoListItemPreview() {
    AndroidThemeForPreviews {
        LazyColumn {
            listOf(false, true).forEach { isSelected ->
                item {
                    SelectVideoListItem(
                        title = "Long title Long title Long title Long title Long title Long title",
                        subtitle = "Simple sub title",
                        icon = R.drawable.ic_folder_outgoing_medium_solid,
                        isSelected = isSelected,
                        onItemClicked = { },
                        isAvailableSelected = isSelected,
                        isEnabled = !isSelected
                    )
                }
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun GenericSelectVideoListItemTakeDownPreview() {
    AndroidThemeForPreviews {
        LazyColumn {
            listOf(false, true).forEach { isSelected ->
                item {
                    SelectVideoListItem(
                        title = "Simple title",
                        subtitle = "Simple sub title",
                        icon = R.drawable.ic_folder_outgoing_medium_solid,
                        isSelected = isSelected,
                        isTakenDown = true,
                        onItemClicked = { },
                        isAvailableSelected = isSelected,
                        isEnabled = !isSelected
                    )
                }
            }
        }
    }
}

const val SELECT_VIDEO_LIST_ITEM_TITLE_TAG = "select_video_list_item:title"
const val SELECT_VIDEO_LIST_ITEM_SUBTITLE_TAG = "select_video_list_item:subtitle"
const val SELECT_VIDEO_LIST_ITEM_ICON_TAG = "select_video_list_item:icon"
const val SELECT_VIDEO_LIST_ITEM_SELECTED_ICON_TAG = "select_video_list_item:icon_selected"
const val SELECT_VIDEO_LIST_ITEM_TAKEN_DOWN_ICON_TAG = "select_video_list_item:taken_down_icon"