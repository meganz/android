package mega.privacy.android.feature.photos.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailData
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.nodes.components.NodeThumbnailView
import mega.privacy.android.shared.nodes.components.ThumbnailLayoutType

@Composable
fun SelectVideoGridItem(
    name: String,
    @DrawableRes icon: Int,
    onItemClicked: () -> Unit,
    modifier: Modifier = Modifier,
    duration: String? = null,
    thumbnailData: ThumbnailData? = null,
    isSelected: Boolean = false,
    isSensitive: Boolean = false,
    isTakenDown: Boolean = false,
    isFolder: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isSensitive) 0.5f else 1f)
            .clip(DSTokens.shapes.extraSmall)
            .background(
                when {
                    isSelected -> DSTokens.colors.background.surface1
                    else -> DSTokens.colors.background.pageBackground
                }
            )
            .combinedClickable(
                onClick = { onItemClicked() }
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
                    .testTag(SELECT_VIDEO_GRID_ITEM_THUMBNAIL_FILE_TAG),
                layoutType = ThumbnailLayoutType.Grid,
                data = thumbnailData,
                defaultImage = icon,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                blurImage = isSensitive
            )

            if (!isFolder) {
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
                            .testTag(SELECT_VIDEO_GRID_ITEM_VIDEO_PLAY_ICON_TAG),
                        contentDescription = "Play Icon",
                        tint = IconColor.OnColor,
                    )
                }
            }

            if (isTakenDown) {
                MegaIcon(
                    imageVector = IconPack.Medium.Thin.Outline.AlertTriangle,
                    contentDescription = "Taken Down",
                    tint = IconColor.Brand,
                    modifier = Modifier
                        .badgeStyle()
                        .testTag(SELECT_VIDEO_GRID_ITEM_GRID_VIEW_TAKEN_TAG),
                )
            }

            if (duration != null && duration.isNotEmpty()) {
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
                            .testTag(SELECT_VIDEO_GRID_ITEM_VIDEO_DURATION_TAG),
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

            MegaText(
                text = name,
                overflow = TextOverflow.MiddleEllipsis,
                maxLines = 2,
                textColor = if (isTakenDown) TextColor.Error else TextColor.Primary,
                style = AppTheme.typography.bodySmall,
                modifier = Modifier
                    .weight(1f)
                    .testTag(SELECT_VIDEO_GRID_ITEM_NODE_TITLE_TAG),
            )

            if (isSelected) {
                MegaIcon(
                    imageVector = IconPack.Medium.Thin.Solid.CheckCircle,
                    tint = IconColor.Primary,
                    contentDescription = null,
                    modifier = Modifier.testTag(SELECT_VIDEO_GRID_ITEM_SELECT_ICON_TAG),
                )
                Spacer(modifier = Modifier.width(DSTokens.spacings.s1))
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
private fun NodeGridViewItemPreview() {
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
            listOf(false, true).forEach { isSelected ->
                item {
                    SelectVideoGridItem(
                        isSelected = isSelected,
                        name = "Video Node Name.mp4",
                        icon = iconPackR.drawable.ic_video_medium_solid,
                        thumbnailData = null,
                        isTakenDown = false,
                        duration = "12:30",
                        onItemClicked = {}
                    )
                }
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun NodeGridViewItemTakenDownPreview() {
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
            listOf(false, true).forEach { isSelected ->
                item {
                    SelectVideoGridItem(
                        isSelected = isSelected,
                        name = "Video Node Name.mp4",
                        icon = iconPackR.drawable.ic_video_medium_solid,
                        thumbnailData = null,
                        isTakenDown = true,
                        duration = "12:30",
                        onItemClicked = {}
                    )
                }
            }
        }
    }
}

const val SELECT_VIDEO_GRID_ITEM_NODE_TITLE_TAG = "select_video_grid_item:node_title"
const val SELECT_VIDEO_GRID_ITEM_THUMBNAIL_FILE_TAG = "select_video_grid_item:thumbnail_file"
const val SELECT_VIDEO_GRID_ITEM_GRID_VIEW_TAKEN_TAG = "select_video_grid_item:grid_view_icon_taken"
const val SELECT_VIDEO_GRID_ITEM_VIDEO_PLAY_ICON_TAG = "select_video_grid_item:video_play_icon"
const val SELECT_VIDEO_GRID_ITEM_VIDEO_DURATION_TAG = "select_video_grid_item:video_duration"
const val SELECT_VIDEO_GRID_ITEM_SELECT_ICON_TAG = "select_video_grid_item:icon_selected"