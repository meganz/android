import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.checkbox.Checkbox
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.modifiers.conditional
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.core.nodecomponents.list.NodeThumbnailView
import mega.privacy.android.core.nodecomponents.list.THUMBNAIL_FILE_TEST_TAG
import mega.privacy.android.core.nodecomponents.list.ThumbnailLayoutType
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailData
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR

/**
 * Composable to show media grid view item
 *
 * @param thumbnailData Thumbnail data
 * @param defaultImage Default image resource
 * @param modifier Modifier to be applied to the item
 * @param duration Duration string to be shown at bottom right corner (for video/audio)
 * @param isSelected Whether the item is selected
 * @param onClick Click action
 * @param onLongClick Long click action
 * @param isSensitive Whether the content is sensitive
 * @param showBlurEffect Whether to show blur effect for sensitive content
 * @param showFavourite Whether to show favourite icon at top right corner
 */
@Composable
fun MediaGridViewItem(
    thumbnailData: ThumbnailData?,
    @DrawableRes defaultImage: Int,
    modifier: Modifier = Modifier,
    duration: String? = null,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    isSensitive: Boolean = false,
    showBlurEffect: Boolean = false,
    showFavourite: Boolean = false,
) {
    Box(
        modifier = modifier
            .alpha(if (isSensitive) 0.5f else 1f)
            .aspectRatio(1f)
            .conditional(isSelected) {
                Modifier
                    .border(
                        width = 2.dp,
                        color = DSTokens.colors.border.strongSelected,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clip(RoundedCornerShape(4.dp))
            }
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { onLongClick() },
            ),
    ) {
        NodeThumbnailView(
            modifier = Modifier
                .align(Alignment.Center)
                .testTag(THUMBNAIL_FILE_TEST_TAG),
            layoutType = ThumbnailLayoutType.MediaGrid,
            data = thumbnailData,
            defaultImage = defaultImage,
            contentDescription = "",
            contentScale = ContentScale.Crop,
            blurImage = showBlurEffect && isSensitive
        )

        if (showFavourite) {
            MegaIcon(
                modifier = Modifier
                    .padding(top = 4.dp, end = 4.dp)
                    .background(
                        color = DSTokens.colors.background.surfaceTransparent,
                        shape = RoundedCornerShape(2.dp)
                    )
                    .padding(2.dp)
                    .align(Alignment.TopEnd)
                    .testTag(MEDIA_GRID_VIEW_FAVOURITE_ICON_TEST_TAG),
                painter = rememberVectorPainter(IconPack.Small.Thin.Solid.Heart),
                contentDescription = null,
                tint = IconColor.OnColor,
            )
        }

        if (isSelected) {
            Checkbox(
                modifier = Modifier
                    .padding(top = 1.dp, start = 1.dp)
                    .testTag(MEDIA_GRID_VIEW_CHECKBOX_TEST_TAG),
                checked = true,
                onCheckStateChanged = {},
                tapTargetArea = false,
                clickable = false,
            )
        }

        if (duration != null && duration.isNotEmpty()) {
            MegaText(
                modifier = Modifier
                    .padding(bottom = 4.dp, end = 4.dp)
                    .background(
                        color = DSTokens.colors.background.surfaceTransparent,
                        shape = RoundedCornerShape(2.dp)
                    )
                    .padding(
                        vertical = 2.dp,
                        horizontal = 4.dp
                    )
                    .align(Alignment.BottomEnd)
                    .testTag(MEDIA_GRID_VIEW_DURATION_TEST_TAG),
                text = duration,
                style = MaterialTheme.typography.labelSmall,
                textColor = TextColor.OnColor
            )
        }
    }
}

@CombinedThemePreviews
@Composable
fun MediaGridViewItemPreview() {
    AndroidThemeForPreviews {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(1.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            item {
                MediaGridViewItem(
                    defaultImage = iconPackR.drawable.ic_ic_choose_photo_medium_regular_solid,
                    thumbnailData = null,
                    duration = null,
                    isSelected = false,
                    showFavourite = true,
                    isSensitive = false,
                    showBlurEffect = false,
                    onClick = {},
                    onLongClick = {}
                )
            }
            item {
                MediaGridViewItem(
                    defaultImage = android.R.drawable.ic_menu_gallery,
                    thumbnailData = null,
                    duration = "03:45",
                    isSelected = false,
                    showFavourite = true,
                    isSensitive = false,
                    showBlurEffect = false,
                    onClick = {},
                    onLongClick = {}
                )
            }
            item {
                MediaGridViewItem(
                    defaultImage = iconPackR.drawable.ic_video_medium_solid,
                    thumbnailData = null,
                    duration = null,
                    isSelected = false,
                    showFavourite = true,
                    isSensitive = false,
                    showBlurEffect = false,
                    onClick = {},
                    onLongClick = {}
                )
            }
            item {
                MediaGridViewItem(
                    defaultImage = iconPackR.drawable.ic_video_medium_solid,
                    thumbnailData = ThumbnailRequest(
                        id = NodeId(-1L),
                        isPublicNode = false
                    ),
                    duration = null,
                    isSelected = false,
                    showFavourite = true,
                    isSensitive = false,
                    showBlurEffect = false,
                    onClick = {},
                    onLongClick = {}
                )
            }
            item {
                MediaGridViewItem(
                    defaultImage = iconPackR.drawable.ic_video_medium_solid,
                    thumbnailData = ThumbnailRequest(
                        id = NodeId(-1L),
                        isPublicNode = false
                    ),
                    duration = null,
                    isSelected = false,
                    showFavourite = false,
                    isSensitive = true,
                    showBlurEffect = false,
                    onClick = {},
                    onLongClick = {}
                )
            }
        }
    }
}

const val MEDIA_GRID_VIEW_FAVOURITE_ICON_TEST_TAG = "media_grid_view_item:favourite_icon"
const val MEDIA_GRID_VIEW_CHECKBOX_TEST_TAG = "media_grid_view_item:checkbox"
const val MEDIA_GRID_VIEW_DURATION_TEST_TAG = "media_grid_view_item:duration"