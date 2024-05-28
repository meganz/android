package mega.privacy.android.legacy.core.ui.controls.lists

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.controls.lists.GenericTwoLineListItem
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * The media queue item view
 *
 * @param icon thumbnail default icon
 * @param name the name of the media item
 * @param currentPlayingPosition the current playing position
 * @param duration the duration of the media item
 * @param thumbnailData the thumbnail data of the media item
 * @param onClick the click callback
 * @param modifier Modifier
 * @param isPaused whether the media item is paused
 * @param isItemPlaying whether the media item is  current playing item
 * @param isReorderEnabled whether the media item can be reordered
 * @param isSelected whether the media item is selected
 */
@Composable
fun MediaQueueItemView(
    @DrawableRes icon: Int,
    name: String,
    currentPlayingPosition: String,
    duration: String,
    thumbnailData: Any?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPaused: Boolean = false,
    isItemPlaying: Boolean = false,
    isReorderEnabled: Boolean = true,
    isSelected: Boolean = false,
) {
    GenericTwoLineListItem(
        title = name,
        modifier = modifier.testTag(MEDIA_QUEUE_ITEM_VIEW_TEST_TAG),
        subtitle = if (isItemPlaying) {
            "$currentPlayingPosition / $duration"
        } else {
            duration
        },
        icon = {
            QueueThumbnailView(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(48.dp)
                    .clip(RoundedCornerShape(5.dp)),
                icon = icon,
                isSelected = isSelected,
                isPaused = isPaused,
                isItemPlaying = isItemPlaying,
                thumbnailData = thumbnailData
            )
        },
        trailingIcons = {
            if (!isItemPlaying && isReorderEnabled) {
                Image(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .testTag(MEDIA_QUEUE_ITEM_REORDER_ICON_TEST_TAG),
                    painter = painterResource(R.drawable.ic_reorder),
                    contentDescription = "Reorder",
                    colorFilter = ColorFilter.tint(MaterialTheme.colors.onPrimary)
                )
            }
        },
        onItemClicked = onClick
    )
}

@Composable
private fun QueueThumbnailView(
    modifier: Modifier,
    @DrawableRes icon: Int,
    isSelected: Boolean,
    isPaused: Boolean,
    isItemPlaying: Boolean,
    thumbnailData: Any?,
) {
    if (isSelected) {
        Image(
            modifier = modifier.testTag(MEDIA_QUEUE_ITEM_SELECT_ICON_TEST_TAG),
            painter = painterResource(R.drawable.ic_select_folder),
            contentDescription = "Selected",
        )
    } else {
        Box(
            modifier = modifier.testTag(MEDIA_QUEUE_ITEM_THUMBNAIL_LAYOUT_TEST_TAG),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                modifier = Modifier.testTag(MEDIA_QUEUE_ITEM_THUMBNAIL_ICON_TEST_TAG),
                model = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnailData)
                    .crossfade(true)
                    .build(),
                contentDescription = "Thumbnail",
                placeholder = painterResource(id = icon),
                error = painterResource(id = icon),
                contentScale = ContentScale.FillBounds
            )

            if (isItemPlaying && isPaused) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.DarkGray)
                        .testTag(MEDIA_QUEUE_ITEM_PAUSED_BACKGROUND_TEST_TAG),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .testTag(MEDIA_QUEUE_ITEM_PAUSED_ICON_TEST_TAG),
                        painter = painterResource(R.drawable.ic_pause),
                        contentDescription = "Paused",
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                }

            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun MediaQueueItemInfoViewWithPausedPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        MediaQueueItemView(
            icon = iconPackR.drawable.ic_audio_medium_solid,
            onClick = {},
            thumbnailData = null,
            modifier = Modifier,
            isPaused = true,
            isItemPlaying = true,
            name = "Video name",
            currentPlayingPosition = "00:00",
            duration = "14:00"
        )
    }
}

@CombinedThemePreviews
@Composable
private fun MediaQueueItemInfoViewWithSelectedPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        MediaQueueItemView(
            icon = iconPackR.drawable.ic_video_medium_solid,
            onClick = {},
            thumbnailData = null,
            modifier = Modifier,
            isSelected = true,
            name = "Video name",
            currentPlayingPosition = "",
            duration = "14:00"
        )
    }
}

@CombinedThemePreviews
@Composable
private fun MediaQueueItemInfoViewPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        MediaQueueItemView(
            icon = iconPackR.drawable.ic_audio_medium_solid,
            onClick = {},
            thumbnailData = null,
            modifier = Modifier,
            name = "Video name",
            currentPlayingPosition = "",
            duration = "14:00",
        )
    }
}

/**
 * Test tag for the media queue item view
 */
const val MEDIA_QUEUE_ITEM_VIEW_TEST_TAG = "media_queue_item:row_view"

/**
 * Test tag for the selected icon of the media queue item
 */
const val MEDIA_QUEUE_ITEM_SELECT_ICON_TEST_TAG = "media_queue_item:image_selected"

/**
 * Test tag for the thumbnail layout of the media queue item
 */
const val MEDIA_QUEUE_ITEM_THUMBNAIL_LAYOUT_TEST_TAG = "media_queue_item:box_layout"

/**
 * Test tag for the thumbnail image of the media queue item
 */
const val MEDIA_QUEUE_ITEM_THUMBNAIL_ICON_TEST_TAG = "media_queue_item:image_thumbnail"

/**
 * Test tag for the paused background of the media queue item
 */
const val MEDIA_QUEUE_ITEM_PAUSED_BACKGROUND_TEST_TAG = "media_queue_item:box_paused_background"

/**
 * Test tag for the paused image of the media queue item
 */
const val MEDIA_QUEUE_ITEM_PAUSED_ICON_TEST_TAG = "media_queue_item:image_paused"

/**
 * Test tag for the reorder icon of the media queue item
 */
const val MEDIA_QUEUE_ITEM_REORDER_ICON_TEST_TAG = "media_queue_item:image_reorder"