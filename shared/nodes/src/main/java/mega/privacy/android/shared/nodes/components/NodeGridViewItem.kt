package mega.privacy.android.shared.nodes.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.checkbox.Checkbox
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.model.HighlightedText
import mega.android.core.ui.modifiers.itemContainerStyle
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailData
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as IconPackR

/**
 * Node grid view item component for displaying files and folders in a grid layout.
 *
 * @param name The display name of the node
 * @param iconRes The drawable resource ID for the default icon (used when thumbnail is not available)
 * @param thumbnailData The thumbnail data for the node (can be URL, file path, or any supported thumbnail data)
 * @param isTakenDown Whether the node has been taken down (affects text color and shows warning icon)
 * @param modifier Optional [Modifier] to be applied to the component
 * @param duration Optional duration string for video files (displayed as a badge)
 * @param isSelected Whether the node is currently selected
 * @param isInSelectionMode Whether the grid is in selection mode (shows checkbox instead of more menu)
 * @param isFolderNode Whether this node represents a folder (affects layout and styling)
 * @param isVideoNode Whether this node represents a video file (shows play icon overlay)
 * @param onClick Callback invoked when the item is clicked
 * @param onLongClick Callback invoked when the item is long-pressed
 * @param onMenuClick Callback invoked when the more options menu is clicked
 * @param isSensitive Whether the item contains sensitive content (reduces opacity)
 * @param isSensitive Whether the item contains sensitive content (reduces opacity)
 * @param showBlurEffect Whether to apply blur effect to thumbnails for sensitive content
 * @param isHighlighted Whether the item should be highlighted (different background color)
 * @param showLink Whether to show a link icon in the top-right corner (indicates shared link)
 * @param showFavourite Whether to show a heart icon in the top-right corner (indicates favourite status)
 * @param label Optional label indicator in the footer area
 */
@Composable
fun NodeGridViewItem(
    name: String,
    @DrawableRes iconRes: Int,
    thumbnailData: ThumbnailData?,
    isTakenDown: Boolean,
    modifier: Modifier = Modifier,
    duration: String? = null,
    isSelected: Boolean = false,
    isInSelectionMode: Boolean = false,
    isFolderNode: Boolean = false,
    isVideoNode: Boolean = false,
    highlightText: String = "",
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    isSensitive: Boolean = false,
    showBlurEffect: Boolean = false,
    isHighlighted: Boolean = false,
    showLink: Boolean = false,
    showFavourite: Boolean = false,
    label: NodeLabel? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .itemContainerStyle(
                enabled = enabled && !isSensitive,
                selected = isSelected,
                highlighted = isHighlighted
            )
            .clip(DSTokens.shapes.extraSmall)
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = onLongClick,
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(5f / 4f)
                .clip(DSTokens.shapes.extraSmall)
                .background(DSTokens.colors.background.surface2)
        ) {
            NodeThumbnailView(
                modifier = Modifier
                    .align(Alignment.Center)
                    .testTag(THUMBNAIL_FILE_TEST_TAG),
                layoutType = ThumbnailLayoutType.Grid,
                data = thumbnailData,
                defaultImage = iconRes,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                blurImage = showBlurEffect && isSensitive
            )

            if (isVideoNode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DSTokens.colors.background.blur)
                ) {
                    MegaIcon(
                        imageVector = IconPack.Medium.Thin.Solid.PlayCircle,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(32.dp)
                            .testTag(VIDEO_PLAY_ICON_TEST_TAG),
                        contentDescription = "Play Icon",
                        tint = IconColor.OnColor,
                    )
                }
            }

            // Top right badges
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(DSTokens.spacings.s2),
                horizontalArrangement = Arrangement.spacedBy(DSTokens.spacings.s2)
            ) {
                if (showFavourite) {
                    MegaIcon(
                        imageVector = IconPack.Small.Thin.Solid.Heart,
                        tint = IconColor.OnColor,
                        contentDescription = "Favourite",
                        modifier = Modifier
                            .testTag(GRID_VIEW_FAVOURITE_ICON_TEST_TAG)
                            .badgeStyle()
                    )
                }
                if (showLink) {
                    MegaIcon(
                        imageVector = IconPack.Medium.Thin.Outline.Link01,
                        tint = IconColor.OnColor,
                        contentDescription = "Link",
                        modifier = Modifier
                            .testTag(GRID_VIEW_LINK_ICON_TEST_TAG)
                            .badgeStyle(),
                    )
                }

                if (isTakenDown) {
                    MegaIcon(
                        imageVector = IconPack.Medium.Thin.Outline.AlertTriangle,
                        contentDescription = "Taken Down",
                        tint = IconColor.Brand,
                        modifier = Modifier
                            .badgeStyle()
                            .testTag(GRID_VIEW_TAKEN_TEST_TAG),
                    )
                }
            }

            if (duration != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(shape = RoundedCornerShape(size = 3.dp))
                        .background(
                            color = DSTokens.colors.background.surfaceTransparent,
                        )
                        .padding(
                            horizontal = DSTokens.spacings.s2,
                            vertical = DSTokens.spacings.s1
                        ),
                ) {
                    MegaText(
                        text = duration,
                        style = AppTheme.typography.bodySmall,
                        textColor = TextColor.OnColor,
                        modifier = Modifier
                            .testTag(VIDEO_DURATION_TEST_TAG),
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = folderMinHeight),
            horizontalArrangement = Arrangement.spacedBy(DSTokens.spacings.s2, Alignment.Start),
        ) {
            Spacer(modifier = Modifier.width(DSTokens.spacings.s2))

            if (label != null) {
                NodeLabelCircle(
                    label = label,
                    modifier = Modifier
                        .testTag(GRID_VIEW_LABEL_TEST_TAG),
                )
            }

            if (highlightText.isNotBlank()) {
                MegaText(
                    text = HighlightedText(
                        full = name,
                        highlighted = highlightText,
                    ),
                    textColor = if (isTakenDown) TextColor.Error else TextColor.Primary,
                    style = AppTheme.typography.bodySmall,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(NODE_TITLE_TEXT_TEST_TAG),
                )
            } else {
                MegaText(
                    text = name,
                    overflow = TextOverflow.MiddleEllipsis,
                    maxLines = 2,
                    textColor = if (isTakenDown) TextColor.Error else TextColor.Primary,
                    style = AppTheme.typography.bodySmall,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(NODE_TITLE_TEXT_TEST_TAG),
                )
            }

            if (isInSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckStateChanged = {},
                    tapTargetArea = false,
                    clickable = false,
                    modifier = Modifier.testTag(GRID_VIEW_CHECKBOX_TAG)
                )
                Spacer(modifier = Modifier.width(DSTokens.spacings.s1))
            } else if (onMenuClick != null) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .wrapContentSize(unbounded = true, align = Alignment.Center)
                        .size(48.dp)
                        .clickable { onMenuClick() }
                        .testTag(GRID_VIEW_MORE_ICON_TEST_TAG),
                    contentAlignment = Alignment.Center
                ) {
                    MegaIcon(
                        imageVector = IconPack.Medium.Thin.Outline.MoreVertical,
                        contentDescription = "More",
                        tint = IconColor.Secondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

private val folderMinHeight = 44.dp

@Composable
private fun Modifier.badgeStyle(): Modifier = this
    .size(24.dp)
    .background(
        color = DSTokens.colors.background.surfaceTransparent,
        shape = DSTokens.shapes.extraSmall
    )
    .padding(4.dp)

@CombinedThemePreviews
@Composable
private fun NodeGridViewItemPreview(
    @PreviewParameter(NodeGridViewItemDataProvider::class) data: NodeGridViewItemData,
) {
    AndroidThemeForPreviews {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(DSTokens.spacings.s3),
            horizontalArrangement = Arrangement.spacedBy(DSTokens.spacings.s3),
            contentPadding = PaddingValues(
                horizontal = DSTokens.spacings.s3,
                vertical = DSTokens.spacings.s3,
            ),
            modifier = Modifier.background(DSTokens.colors.background.pageBackground),
        ) {
            listOf(false, true).forEach { selectionMode ->
                item {
                    var isSelected by remember {
                        mutableStateOf(false)
                    }
                    NodeGridViewItem(
                        isSelected = isSelected,
                        isInSelectionMode = selectionMode,
                        name = data.name,
                        iconRes = data.iconRes,
                        thumbnailData = data.thumbnailData,
                        isTakenDown = data.isTakenDown,
                        onClick = {
                            isSelected = !isSelected
                        },
                        isFolderNode = data.isFolderNode,
                        duration = data.duration,
                        isVideoNode = data.isVideoNode,
                        showLink = data.showLink,
                        showFavourite = data.showFavourite,
                        label = data.label,
                    )
                }
            }
        }
    }
}

private data class NodeGridViewItemData(
    val name: String,
    val thumbnailData: ThumbnailData? = null,
    val duration: String?,
    val isTakenDown: Boolean,
    val iconRes: Int,
    val isFolderNode: Boolean,
    val isVideoNode: Boolean = false,
    val showLink: Boolean = false,
    val showFavourite: Boolean = false,
    val label: NodeLabel? = null,
)

private class NodeGridViewItemDataProvider : PreviewParameterProvider<NodeGridViewItemData> {
    override val values = sequenceOf(
        NodeGridViewItemData(
            name = "My folder",
            thumbnailData = null,
            duration = null,
            isTakenDown = false,
            iconRes = IconPackR.drawable.ic_folder_backup_medium_solid,
            isFolderNode = true,
            label = NodeLabel.GREEN
        ),
        NodeGridViewItemData(
            name = "Longest name I could thing of example of three lines.png",
            thumbnailData = null,
            duration = null,
            isTakenDown = false,
            iconRes = IconPackR.drawable.ic_folder_backup_medium_solid,
            isFolderNode = true,
            label = NodeLabel.GREEN
        ),
        NodeGridViewItemData(
            name = "Getting started with MEGA.pdf",
            thumbnailData = null,
            duration = null,
            isTakenDown = false,
            iconRes = IconPackR.drawable.ic_photoshop_medium_solid,
            isFolderNode = false
        ),
        NodeGridViewItemData(
            name = "Getting started with MEGA.pdf",
            duration = null,
            isTakenDown = false,
            iconRes = IconPackR.drawable.ic_generic_medium_solid,
            isFolderNode = false
        ),
        NodeGridViewItemData(
            name = "NodeGridViewItem3",
            thumbnailData = null,
            duration = "12:3",
            isTakenDown = false,
            iconRes = IconPackR.drawable.ic_video_medium_solid,
            isFolderNode = false,
            isVideoNode = true
        ),
        NodeGridViewItemData(
            name = "Getting started with MEGA.pdf",
            duration = "12:3",
            isTakenDown = true,
            iconRes = IconPackR.drawable.ic_audio_medium_solid,
            isFolderNode = false
        ),
        NodeGridViewItemData(
            name = "Getting started with MEGA",
            thumbnailData = null,
            duration = null,
            isTakenDown = false,
            iconRes = IconPackR.drawable.ic_pdf_medium_solid,
            isFolderNode = false,
            showLink = true,
            showFavourite = true,
            label = NodeLabel.BLUE
        )
    )
}


const val NODE_TITLE_TEXT_TEST_TAG = "node_grid_view_item:node_title"
const val THUMBNAIL_FILE_TEST_TAG = "node_grid_view_item:thumbnail_file"
const val GRID_VIEW_TAKEN_TEST_TAG = "node_grid_view_item:grid_view_icon_taken"
const val GRID_VIEW_MORE_ICON_TEST_TAG = "node_grid_view_item:grid_view_more_icon"
const val VIDEO_PLAY_ICON_TEST_TAG = "node_grid_view_item:video_play_icon"
const val VIDEO_DURATION_TEST_TAG = "node_grid_view_item:video_duration"
const val FOLDER_VIEW_ICON_TEST_TAG = "node_grid_view_item:folder_view_icon"
const val GRID_VIEW_FAVOURITE_ICON_TEST_TAG = "node_grid_view_item:favourite_icon"
const val GRID_VIEW_LINK_ICON_TEST_TAG = "node_grid_view_item:link_icon"
const val GRID_VIEW_LABEL_TEST_TAG = "node_grid_view_item:label"
const val GRID_VIEW_CHECKBOX_TAG = "node_grid_view_item:checkbox"


