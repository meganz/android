package mega.privacy.android.feature.photos.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR

@Composable
fun VideoPlaylistDetailHeaderView(
    thumbnailList: List<Any?>?,
    title: String?,
    totalDuration: String?,
    numberOfVideos: Int?,
    onPlayAllClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            val thumbnailModifier = Modifier
                .width(126.dp)
                .aspectRatio(1.77f)
                .clip(RoundedCornerShape(10.dp))

            VideoPlaylistThumbnailView(
                emptyPlaylistIcon = iconPackR.drawable.ic_video_playlist_default_thumbnail,
                noThumbnailIcon = iconPackR.drawable.ic_video_playlist_no_thumbnail,
                modifier = thumbnailModifier,
                thumbnailList = thumbnailList
            )

            VideoPlaylistInfoView(
                title = title ?: "",
                totalDuration = if (totalDuration.isNullOrEmpty()) {
                    "00:00"
                } else {
                    totalDuration
                },
                numberOfVideos = numberOfVideos ?: 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .testTag(VIDEO_PLAYLIST_DETAIL_HEADER_INFO_TEST_TAG)
            )
        }
        if (numberOfVideos != null && numberOfVideos != 0) {
            PlayAllButtonView(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 16.dp, bottom = 10.dp),
                onPlayAllClicked = onPlayAllClicked
            )
        }
    }
}

@Composable
internal fun PlayAllButtonView(
    modifier: Modifier = Modifier,
    onPlayAllClicked: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .border(
                width = 1.dp,
                color = DSTokens.colors.button.outline,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onPlayAllClicked() }
            .testTag(VIDEO_PLAYLIST_DETAIL_HEADER_PLAY_ALL_TEST_TAG)
    ) {
        Icon(
            imageVector = IconPack.Medium.Thin.Outline.Play,
            contentDescription = "play all",
            modifier = Modifier
                .padding(start = 20.dp, end = 5.dp)
                .size(24.dp)
                .align(Alignment.CenterVertically)
        )

        MegaText(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(end = 20.dp),
            text = stringResource(id = sharedR.string.video_section_playlist_detail_play_all_button),
            textColor = TextColor.Accent,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideoPlaylistDetailHeaderViewDefaultThumbnailPreview() {
    AndroidThemeForPreviews {
        VideoPlaylistDetailHeaderView(
            thumbnailList = null,
            title = "Video Playlist",
            totalDuration = null,
            numberOfVideos = 0,
            onPlayAllClicked = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideoPlaylistDetailHeaderViewNoThumbnailPreview() {
    AndroidThemeForPreviews {
        VideoPlaylistDetailHeaderView(
            thumbnailList = emptyList(),
            title = "Video Playlist",
            totalDuration = "1:30:45",
            numberOfVideos = 10,
            onPlayAllClicked = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideoPlaylistDetailHeaderViewPreview() {
    AndroidThemeForPreviews {
        VideoPlaylistDetailHeaderView(
            thumbnailList = listOf(Any(), Any(), Any(), Any()),
            title = "Video Playlist",
            totalDuration = "1:30:45",
            numberOfVideos = 10,
            onPlayAllClicked = {},
        )
    }
}

/**
 * Test tag for video playlist detail header info
 */
const val VIDEO_PLAYLIST_DETAIL_HEADER_INFO_TEST_TAG = "video_playlist_detail_header:item_info"

/**
 * Test tag for video playlist detail header play all button
 */
const val VIDEO_PLAYLIST_DETAIL_HEADER_PLAY_ALL_TEST_TAG =
    "video_playlist_detail_header:button_play_all"