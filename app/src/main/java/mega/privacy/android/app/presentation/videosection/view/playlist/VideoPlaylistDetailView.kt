package mega.privacy.android.app.presentation.videosection.view.playlist

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistUIEntity
import mega.privacy.android.app.presentation.videosection.model.VideoUIEntity
import mega.privacy.android.app.presentation.videosection.view.allvideos.VideoItemView
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.ui.controls.dividers.DividerSpacing
import mega.privacy.android.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.grey_050_grey_800
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyView
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Test tag for empty view
 */
const val VIDEO_PLAYLIST_DETAIL_EMPTY_VIEW_TEST_TAG = "video_playlist_detail_empty_view_test_tag"

/**
 * Test tag for playlist title
 */
const val PLAYLIST_TITLE_TEST_TAG = "playlist_title_test_tag"

/**
 * Test tag for playlist total duration
 */
const val PLAYLIST_TOTAL_DURATION_TEST_TAG = "playlist_total_duration_test_tag"

/**
 * Test tag for playlist number of videos
 */
const val PLAYLIST_NUMBER_OF_VIDEOS_TEST_TAG = "playlist_number_of_videos_test_tag"


internal const val videoPlaylistDetailRoute = "videoSection/video_playlist/detail"

/**
 * Video playlist detail view
 */
@Composable
fun VideoPlaylistDetailView(
    playlist: VideoPlaylistUIEntity?,
    modifier: Modifier = Modifier,
    onClick: (item: VideoUIEntity, index: Int) -> Unit = { _, _ -> },
    onMenuClick: (VideoUIEntity) -> Unit = { _ -> },
    onLongClick: ((item: VideoUIEntity, index: Int) -> Unit) = { _, _ -> },
) {
    val items = playlist?.videos ?: emptyList()
    val lazyListState = rememberLazyListState()

    Column {
        VideoPlaylistHeaderView(
            thumbnailList = playlist?.thumbnailList,
            title = playlist?.title,
            totalDuration = playlist?.totalDuration,
            numberOfVideos = playlist?.numberOfVideos,
            modifier = modifier.padding(16.dp)
        )
        MegaDivider(
            dividerSpacing = DividerSpacing.Center,
            modifier = modifier.padding(bottom = 16.dp)
        )
        when {
            items.isEmpty() -> LegacyMegaEmptyView(
                modifier = modifier.testTag(VIDEO_PLAYLIST_DETAIL_EMPTY_VIEW_TEST_TAG),
                text = stringResource(id = R.string.homepage_empty_hint_video),
                imagePainter = painterResource(id = R.drawable.ic_homepage_empty_video)
            )

            else -> {
                LazyColumn(state = lazyListState, modifier = modifier) {
                    items(count = items.size, key = { items[it].id.longValue }) {
                        val videoItem = items[it]
                        VideoItemView(
                            icon = iconPackR.drawable.ic_video_list,
                            name = videoItem.name,
                            fileSize = formatFileSize(videoItem.size, LocalContext.current),
                            duration = videoItem.duration,
                            isFavourite = videoItem.isFavourite,
                            isSelected = videoItem.isSelected,
                            thumbnailData = if (videoItem.thumbnail?.exists() == true) {
                                videoItem.thumbnail
                            } else {
                                ThumbnailRequest(videoItem.id)
                            },
                            nodeAvailableOffline = videoItem.nodeAvailableOffline,
                            onClick = { onClick(videoItem, it) },
                            onMenuClick = { onMenuClick(videoItem) },
                            onLongClick = { onLongClick(videoItem, it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun VideoPlaylistHeaderView(
    thumbnailList: List<Any?>?,
    title: String?,
    totalDuration: String?,
    numberOfVideos: Int?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            val thumbnailModifier = Modifier
                .width(126.dp)
                .aspectRatio(1.6f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colors.grey_050_grey_800)

            ThumbnailListView(
                icon = R.drawable.ic_playlist_item_empty,
                modifier = thumbnailModifier,
                thumbnailList = thumbnailList
            )

            VideoPlaylistInfoView(
                title = title ?: "",
                totalDuration = totalDuration ?: "00:00:00",
                numberOfVideos = numberOfVideos ?: 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )
        }
        PlayAllButtonView(
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 16.dp)
        )
    }
}

@Composable
internal fun VideoPlaylistInfoView(
    title: String,
    totalDuration: String,
    numberOfVideos: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(start = 16.dp)
    ) {
        MegaText(
            modifier = modifier
                .fillMaxSize()
                .weight(1.5f)
                .testTag(PLAYLIST_TITLE_TEST_TAG),
            text = title,
            textColor = TextColor.Primary,
            style = MaterialTheme.typography.subtitle1,
            overflow = LongTextBehaviour.Clip(2)
        )

        MegaText(
            modifier = modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag(PLAYLIST_TOTAL_DURATION_TEST_TAG),
            text = totalDuration,
            textColor = TextColor.Secondary,
            style = MaterialTheme.typography.caption
        )

        MegaText(
            modifier = modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag(PLAYLIST_NUMBER_OF_VIDEOS_TEST_TAG),
            text = if (numberOfVideos != 0) {
                if (numberOfVideos == 1) {
                    "1 Video"
                } else {
                    "$numberOfVideos Videos"
                }
            } else {
                "no videos"
            },
            textColor = TextColor.Secondary,
            style = MaterialTheme.typography.caption
        )
    }
}

@Composable
internal fun PlayAllButtonView(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colors.secondary,
                shape = RoundedCornerShape(5.dp)
            )
    ) {
        Image(
            painter = painterResource(id = iconPackR.drawable.ic_playlist_play_all),
            contentDescription = "play all",
            modifier = Modifier
                .padding(start = 20.dp, end = 5.dp)
                .size(12.dp)
                .align(Alignment.CenterVertically)
        )

        MegaText(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(end = 20.dp),
            text = "Play all",
            textColor = TextColor.Accent,
            style = MaterialTheme.typography.caption
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideoPlaylistDetailViewPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        VideoPlaylistDetailView(null)
    }
}

@CombinedThemePreviews
@Composable
private fun VideoPlaylistHeaderViewPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        VideoPlaylistHeaderView(
            modifier = Modifier,
            thumbnailList = listOf(null),
            title = "New Playlist",
            totalDuration = "00:00:00",
            numberOfVideos = 0
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PlayAllButtonViewPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        PlayAllButtonView()
    }
}