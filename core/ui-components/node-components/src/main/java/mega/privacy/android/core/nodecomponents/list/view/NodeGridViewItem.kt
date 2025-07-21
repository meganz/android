package mega.privacy.android.core.nodecomponents.list.view

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.checkbox.Checkbox
import mega.android.core.ui.components.image.GridThumbnailView
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.modifiers.conditional
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
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
 * @param isInvisible Whether the item should be invisible (renders as a spacer)
 * @param isSensitive Whether the item contains sensitive content (reduces opacity)
 * @param showBlurEffect Whether to apply blur effect to thumbnails for sensitive content
 * @param isHighlighted Whether the item should be highlighted (different background color)
 * @param showLink Whether to show a link icon in the top-right corner (indicates shared link)
 * @param showFavourite Whether to show a heart icon in the top-right corner (indicates favourite status)
 * @param labelColor Optional color for the label indicator in the footer area
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NodeGridViewItem(
    name: String,
    @DrawableRes iconRes: Int,
    thumbnailData: Any?,
    isTakenDown: Boolean,
    modifier: Modifier = Modifier,
    duration: String? = null,
    isSelected: Boolean = false,
    isInSelectionMode: Boolean = false,
    isFolderNode: Boolean = false,
    isVideoNode: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onMenuClick: (() -> Unit) = {},
    isInvisible: Boolean = false,
    isSensitive: Boolean = false,
    showBlurEffect: Boolean = false,
    isHighlighted: Boolean = false,
    showLink: Boolean = false,
    showFavourite: Boolean = false,
    labelColor: Color? = null,
) {
    if (isInvisible) {
        Spacer(
            modifier = Modifier
                .height(folderMinHeight)
                .fillMaxWidth(),
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .alpha(1f.takeIf { !isSensitive } ?: 0.5f)
                .conditional(isFolderNode) {
                    border(
                        width = 1.dp,
                        color = DSTokens.colors.border.subtle,
                        shape = DSTokens.shapes.extraSmall,
                    )
                }
                .clip(DSTokens.shapes.extraSmall)
                .background(
                    when {
                        isSelected -> DSTokens.colors.background.surface1
                        isHighlighted -> DSTokens.colors.background.surface2
                        else -> DSTokens.colors.background.pageBackground
                    }
                )
                .conditional(!isFolderNode) {
                    padding(DSTokens.spacings.s2)
                }
                .combinedClickable(
                    onClick = { onClick() },
                    onLongClick = { onLongClick() },
                )
        ) {
            if (!isFolderNode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(5f / 4f)
                        .clip(DSTokens.shapes.extraSmall)
                        .background(DSTokens.colors.background.surface2)
                ) {
                    GridThumbnailView(
                        data = thumbnailData,
                        defaultImage = iconRes,
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.Center)
                            .testTag(THUMBNAIL_FILE_TEST_TAG),
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        onSuccess = { modifier ->
                            if (!showBlurEffect) {
                                modifier
                            } else {
                                modifier
                                    .blur(16.dp.takeIf { isSensitive } ?: 0.dp)
                            }
                        }
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

                    // Top right icons
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(DSTokens.spacings.s2),
                        horizontalArrangement = Arrangement.spacedBy(DSTokens.spacings.s2)
                    ) {
                        if (showFavourite) {
                            MegaIcon(
                                imageVector = IconPack.Small.Thin.Solid.Heart,
                                tint = IconColor.Secondary,
                                contentDescription = "Favourite",
                                modifier = Modifier
                                    .testTag(GRID_VIEW_FAVOURITE_ICON_TEST_TAG)
                                    .badgeStyle()
                            )
                        }
                        if (showLink) {
                            MegaIcon(
                                imageVector = IconPack.Medium.Thin.Outline.Link01,
                                tint = IconColor.Secondary,
                                contentDescription = "Link",
                                modifier = Modifier
                                    .testTag(GRID_VIEW_LINK_ICON_TEST_TAG)
                                    .badgeStyle(),
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
                                    color = DSTokens.colors.background.surfaceInverseAccent,
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
            }

            Footer(
                name = name,
                isFolderNode = isFolderNode,
                iconRes = iconRes,
                isTakenDown = isTakenDown,
                isSensitive = isSensitive,
                onMenuClick = onMenuClick,
                isSelected = isSelected,
                isInSelectionMode = isInSelectionMode,
                labelColor = labelColor,
            )
        }
    }
}

private val folderMinHeight = 44.dp

@Composable
private fun Modifier.badgeStyle(): Modifier = this
    .size(24.dp)
    .background(
        color = DSTokens.colors.background.surface1,
        shape = DSTokens.shapes.extraSmall
    )
    .padding(4.dp)

@Composable
private fun Footer(
    isFolderNode: Boolean,
    iconRes: Int,
    name: String,
    isTakenDown: Boolean,
    isSensitive: Boolean,
    onMenuClick: (() -> Unit),
    isSelected: Boolean,
    isInSelectionMode: Boolean = false,
    modifier: Modifier = Modifier,
    labelColor: Color? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = folderMinHeight)
            .conditional(isFolderNode) {
                padding(horizontal = DSTokens.spacings.s2)
            },
        horizontalArrangement = Arrangement.spacedBy(DSTokens.spacings.s2, Alignment.Start),
    ) {
        if (isFolderNode) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = "Folder",
                modifier = Modifier
                    .padding(start = 4.dp)
                    .alpha(1f.takeIf { !isSensitive } ?: 0.5f)
                    .size(24.dp)
                    .testTag(FOLDER_VIEW_ICON_TEST_TAG),
            )
        }
        if (labelColor != null) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = labelColor,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
                    .testTag(GRID_VIEW_LABEL_TEST_TAG)
            )
        }
        MegaText(
            text = name,
            textColor = if (isTakenDown) TextColor.Error else TextColor.Primary,
            style = AppTheme.typography.bodySmall,
            overflow = TextOverflow.MiddleEllipsis,
            maxLines = if (isFolderNode) 1 else 2,
            modifier = Modifier
                .weight(1f)
                .testTag(NODE_TITLE_TEXT_TEST_TAG),
        )
        if (isTakenDown) {
            MegaIcon(
                imageVector = IconPack.Medium.Thin.Outline.AlertTriangle,
                contentDescription = "Taken Down",
                tint = IconColor.Brand,
                modifier = Modifier
                    .size(22.dp)
                    .testTag(GRID_VIEW_TAKEN_TEST_TAG),
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
        } else {
            MegaIcon(
                imageVector = IconPack.Medium.Thin.Outline.MoreVertical,
                tint = IconColor.Secondary,
                contentDescription = "More",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onMenuClick() }
                    .testTag(GRID_VIEW_MORE_ICON_TEST_TAG)
            )
        }
    }
}


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
                        labelColor = data.labelColor,
                    )
                }
            }
        }
    }
}

private data class NodeGridViewItemData(
    val name: String,
    val thumbnailData: Any?,
    val duration: String?,
    val isTakenDown: Boolean,
    val iconRes: Int,
    val isFolderNode: Boolean,
    val isVideoNode: Boolean = false,
    val showLink: Boolean = false,
    val showFavourite: Boolean = false,
    val labelColor: Color? = null,
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
            labelColor = Color.Green
        ),
        NodeGridViewItemData(
            name = "Longest name I could thing of example of three lines.png",
            thumbnailData = null,
            duration = null,
            isTakenDown = false,
            iconRes = IconPackR.drawable.ic_folder_backup_medium_solid,
            isFolderNode = true,
            labelColor = Color.Green
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
            thumbnailData = "https://mega.io/wp-content/themes/megapages/megalib/images/megaicon.svg",
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
            thumbnailData = "https://mega.io/wp-content/themes/megapages/megalib/images/megaicon.svg",
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
            labelColor = Color.Green
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


