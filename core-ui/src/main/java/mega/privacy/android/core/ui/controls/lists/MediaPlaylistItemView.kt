package mega.privacy.android.core.ui.controls.lists

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.tokens.Colors
import mega.privacy.android.core.ui.theme.tokens.TextColor

/**
 * The media playlist item view
 *
 * @param icon thumbnail default icon
 * @param name the name of the media item
 * @param currentPlaylistPosition the current playing position
 * @param duration the duration of the media item
 * @param thumbnailData the thumbnail data of the media item
 * @param onClick the click callback
 * @param modifier Modifier
 * @param isPaused whether the media item is paused
 * @param isItemPlaying whether the media item is  current playing item
 * @param isReorderEnabled whether the media item can be reordered
 * @param isSelected whether the media item is selected
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaPlaylistItemView(
    @DrawableRes icon: Int,
    name: String,
    currentPlaylistPosition: String,
    duration: String,
    thumbnailData: Any?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPaused: Boolean = false,
    isItemPlaying: Boolean = false,
    isReorderEnabled: Boolean = true,
    isSelected: Boolean = false,
) {
    Row(
        modifier = modifier
            .combinedClickable(
                onClick = onClick
            )
            .fillMaxWidth()
            .height(72.dp)
            .testTag(MEDIA_PLAYLIST_ITEM_VIEW_TEST_TAG),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val thumbnailModifier = Modifier
            .padding(start = 12.dp)
            .size(48.dp)
            .clip(RoundedCornerShape(5.dp))
        PlaylistThumbnailView(
            modifier = thumbnailModifier,
            icon = icon,
            isSelected = isSelected,
            isPaused = isPaused,
            isItemPlaying = isItemPlaying,
            thumbnailData = thumbnailData
        )

        PlaylistInfoView(
            modifier = Modifier
                .padding(start = 24.dp)
                .fillMaxWidth()
                .weight(1f),
            name = name,
            currentPlaylistPosition = currentPlaylistPosition,
            isItemPlaying = isItemPlaying,
            duration = duration
        )

        if (!isItemPlaying && isReorderEnabled) {
            Image(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .testTag(MEDIA_PLAYLIST_ITEM_REORDER_ICON_TEST_TAG),
                painter = painterResource(R.drawable.ic_reorder),
                contentDescription = "Reorder",
                colorFilter = ColorFilter.tint(MegaTheme.colors.icon.secondary)
            )
        }
    }
}

@Composable
private fun PlaylistThumbnailView(
    modifier: Modifier,
    @DrawableRes icon: Int,
    isSelected: Boolean,
    isPaused: Boolean,
    isItemPlaying: Boolean,
    thumbnailData: Any?,
) {
    if (isSelected) {
        Image(
            modifier = modifier.testTag(MEDIA_PLAYLIST_ITEM_SELECT_ICON_TEST_TAG),
            painter = painterResource(R.drawable.ic_select_folder),
            contentDescription = "Selected",
        )
    } else {
        Box(
            modifier = modifier.testTag(MEDIA_PLAYLIST_ITEM_THUMBNAIL_LAYOUT_TEST_TAG),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                modifier = Modifier.testTag(MEDIA_PLAYLIST_ITEM_THUMBNAIL_ICON_TEST_TAG),
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
                        .background(Colors.Neutral.n800)
                        .testTag(MEDIA_PLAYLIST_ITEM_PAUSED_BACKGROUND_TEST_TAG),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .testTag(MEDIA_PLAYLIST_ITEM_PAUSED_ICON_TEST_TAG),
                        painter = painterResource(R.drawable.ic_pause),
                        contentDescription = "Paused",
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                }

            }
        }
    }
}

@Composable
private fun PlaylistInfoView(
    name: String,
    currentPlaylistPosition: String,
    isItemPlaying: Boolean,
    duration: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
    ) {
        MegaText(
            modifier = Modifier.testTag(MEDIA_PLAYLIST_ITEM_NAME_TEST_TAG),
            text = name,
            textColor = TextColor.Primary
        )

        MegaText(
            modifier = Modifier.testTag(MEDIA_PLAYLIST_ITEM_DURATION_TEST_TAG),
            text = if (isItemPlaying) {
                "$currentPlaylistPosition / $duration"
            } else {
                duration
            },
            textColor = TextColor.Secondary,
            style = MaterialTheme.typography.caption
        )
    }
}

@CombinedThemePreviews
@Composable
private fun MediaPlaylistItemInfoViewWithPausedPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MediaPlaylistItemView(
            icon = iconPackR.drawable.ic_audio_medium_solid,
            onClick = {},
            thumbnailData = null,
            modifier = Modifier,
            isPaused = true,
            isItemPlaying = true,
            name = "Video name",
            currentPlaylistPosition = "00:00",
            duration = "14:00"
        )
    }
}

@CombinedThemePreviews
@Composable
private fun MediaPlaylistItemInfoViewWithSelectedPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MediaPlaylistItemView(
            icon = iconPackR.drawable.ic_video_medium_solid,
            onClick = {},
            thumbnailData = null,
            modifier = Modifier,
            isSelected = true,
            name = "Video name",
            currentPlaylistPosition = "",
            duration = "14:00"
        )
    }
}

@CombinedThemePreviews
@Composable
private fun MediaPlaylistItemInfoViewPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MediaPlaylistItemView(
            icon = iconPackR.drawable.ic_audio_medium_solid,
            onClick = {},
            thumbnailData = null,
            modifier = Modifier,
            name = "Video name",
            currentPlaylistPosition = "",
            duration = "14:00",
        )
    }
}

/**
 * Test tag for the media playlist item view
 */
const val MEDIA_PLAYLIST_ITEM_VIEW_TEST_TAG = "media_playlist_item:row_view"

/**
 * Test tag for the selected icon of the media playlist item
 */
const val MEDIA_PLAYLIST_ITEM_SELECT_ICON_TEST_TAG = "media_playlist_item:image_selected"

/**
 * Test tag for the thumbnail layout of the media playlist item
 */
const val MEDIA_PLAYLIST_ITEM_THUMBNAIL_LAYOUT_TEST_TAG = "media_playlist_item:box_layout"

/**
 * Test tag for the thumbnail image of the media playlist item
 */
const val MEDIA_PLAYLIST_ITEM_THUMBNAIL_ICON_TEST_TAG = "media_playlist_item:image_thumbnail"

/**
 * Test tag for the paused background of the media playlist item
 */
const val MEDIA_PLAYLIST_ITEM_PAUSED_BACKGROUND_TEST_TAG =
    "media_playlist_item:box_paused_background"

/**
 * Test tag for the paused image of the media playlist item
 */
const val MEDIA_PLAYLIST_ITEM_PAUSED_ICON_TEST_TAG = "media_playlist_item:image_paused"

/**
 * Test tag for the name of the media playlist item
 */
const val MEDIA_PLAYLIST_ITEM_NAME_TEST_TAG = "media_playlist_item:text_name"

/**
 * Test tag for the duration of the media playlist item
 */
const val MEDIA_PLAYLIST_ITEM_DURATION_TEST_TAG = "media_playlist_item:text_duration"

/**
 * Test tag for the reorder icon of the media playlist item
 */
const val MEDIA_PLAYLIST_ITEM_REORDER_ICON_TEST_TAG = "media_playlist_item:image_reorder"