package mega.privacy.android.feature.photos.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoPlaylistItemView(
    @DrawableRes emptyPlaylistIcon: Int,
    @DrawableRes noThumbnailIcon: Int,
    title: String,
    numberOfVideos: Int,
    totalDuration: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    thumbnailList: List<Any?>?,
    modifier: Modifier = Modifier,
    isSystemVideoPlaylist: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onMenuClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .fillMaxWidth()
            .height(87.dp)
            .testTag(VIDEO_PLAYLIST_ITEM_VIEW_TEST_TAG),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VideoPlaylistThumbnailView(
            emptyPlaylistIcon = emptyPlaylistIcon,
            noThumbnailIcon = noThumbnailIcon,
            modifier = Modifier,
            thumbnailList = thumbnailList
        )

        VideoPlaylistInfoView(
            title = title,
            numberOfVideos = numberOfVideos,
            totalDuration = totalDuration,
            modifier = Modifier.weight(1f),
        )

        if (!isSystemVideoPlaylist) {
            MegaIcon(
                imageVector = if (isSelected)
                    IconPack.Medium.Thin.Solid.CheckCircle
                else
                    IconPack.Medium.Thin.Outline.MoreVertical,
                tint = IconColor.Primary,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .clickable(enabled = !isSelected, onClick = onMenuClick)
                    .testTag(VIDEO_PLAYLIST_ITEM_TAILING_ICON_TEST_TAG)
            )
        }
    }
}

@Composable
internal fun VideoPlaylistThumbnailView(
    @DrawableRes emptyPlaylistIcon: Int,
    @DrawableRes noThumbnailIcon: Int,
    modifier: Modifier,
    thumbnailList: List<Any?>?,
) {
    Box(
        modifier = modifier.testTag(VIDEO_PLAYLIST_ITEM_THUMBNAIL_VIEW_TEST_TAG),
        contentAlignment = Alignment.TopStart
    ) {
        val thumbnailModifier = Modifier
            .width(126.dp)
            .aspectRatio(1.77f)
            .clip(RoundedCornerShape(10.dp))

        ThumbnailListView(
            emptyPlaylistIcon = emptyPlaylistIcon,
            noThumbnailIcon = noThumbnailIcon,
            modifier = thumbnailModifier,
            thumbnailList = thumbnailList,
        )

        Icon(
            imageVector = IconPack.Medium.Thin.Solid.RectangleVideoStack,
            tint = Color.White,
            contentDescription = "video stack",
            modifier = Modifier
                .padding(top = 5.dp, end = 5.dp)
                .size(24.dp)
                .align(Alignment.TopEnd)
                .testTag(VIDEO_PLAYLIST_ITEM_STACK_ICON_TEST_TAG)
        )
    }
}

@Composable
internal fun VideoPlaylistInfoView(
    title: String,
    numberOfVideos: Int,
    totalDuration: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(vertical = 5.dp, horizontal = 10.dp)
            .fillMaxHeight()
    ) {
        MegaText(
            text = title,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(VIDEO_PLAYLIST_ITEM_TITLE_TEST_TAG),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Normal
            ),
            textColor = TextColor.Primary,
            textAlign = TextAlign.Start,
            maxLines = 2
        )

        MegaText(
            modifier = Modifier
                .padding(vertical = 5.dp)
                .testTag(VIDEO_PLAYLIST_ITEM_INFO_TEST_TAG),
            text = if (numberOfVideos != 0) {
                val numberOfVideosText = pluralStringResource(
                    sharedR.plurals.video_section_playlists_video_count,
                    numberOfVideos,
                    numberOfVideos
                )
                "$numberOfVideosText • $totalDuration"
            } else {
                stringResource(sharedR.string.video_section_playlists_video_empty)
            },
            maxLines = 1,
            style = MaterialTheme.typography.bodySmall,
            textColor = TextColor.Secondary,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideoPlaylistItemViewWithoutVideosPreview() {
    AndroidThemeForPreviews {
        VideoPlaylistItemView(
            emptyPlaylistIcon = iconPackR.drawable.ic_video_playlist_default_thumbnail,
            noThumbnailIcon = iconPackR.drawable.ic_video_playlist_no_thumbnail,
            title = "New Playlist",
            thumbnailList = null,
            modifier = Modifier,
            numberOfVideos = 0,
            totalDuration = null,
            onClick = {},
            isSelected = false
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideoPlaylistItemViewWith1VideoPreview() {
    AndroidThemeForPreviews {
        VideoPlaylistItemView(
            emptyPlaylistIcon = iconPackR.drawable.ic_video_playlist_default_thumbnail,
            noThumbnailIcon = iconPackR.drawable.ic_video_playlist_no_thumbnail,
            title = "1 Video Playlist",
            thumbnailList = null,
            modifier = Modifier,
            numberOfVideos = 1,
            totalDuration = "00:05:55",
            onClick = {},
            isSelected = false
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideoPlaylistItemViewMultipleVideosPreview() {
    AndroidThemeForPreviews {
        VideoPlaylistItemView(
            emptyPlaylistIcon = iconPackR.drawable.ic_video_playlist_default_thumbnail,
            noThumbnailIcon = iconPackR.drawable.ic_video_playlist_no_thumbnail,
            title = "Multiple Video Playlist",
            thumbnailList = null,
            modifier = Modifier,
            numberOfVideos = 3,
            totalDuration = "1:00:55",
            onClick = {},
            isSelected = true
        )
    }
}

/**
 * Test tag for VideoPlaylistItemView
 */
const val VIDEO_PLAYLIST_ITEM_VIEW_TEST_TAG = "video_playlist_item_view_test_tag"

/**
 * Test tag for video playlist item title
 */
const val VIDEO_PLAYLIST_ITEM_TITLE_TEST_TAG = "video_playlist_item_title_test_tag"

/**
 * Test tag for video playlist item info
 */
const val VIDEO_PLAYLIST_ITEM_INFO_TEST_TAG = "video_playlist_item_info_test_tag"

/**
 * Test tag for thumbnail view
 */
const val VIDEO_PLAYLIST_ITEM_THUMBNAIL_VIEW_TEST_TAG =
    "video_playlist_item_thumbnail_icon_test_tag"

/**
 * Test tag for video playlist item stack icon
 */
const val VIDEO_PLAYLIST_ITEM_STACK_ICON_TEST_TAG = "video_playlist_item_stack_icon_test_tag"

/**
 * Test tag for tailing icon
 */
const val VIDEO_PLAYLIST_ITEM_TAILING_ICON_TEST_TAG = "video_playlist_item_tailing_icon_test_tag"