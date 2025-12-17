package mega.privacy.android.feature.photos.presentation.playlists

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollLazyColumn
import mega.privacy.android.core.nodecomponents.list.NodesViewSkeleton
import mega.privacy.android.core.sharedcomponents.empty.MegaEmptyView
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.feature.photos.components.VideoPlaylistItemView
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR

@Composable
fun VideoPlaylistsTabRoute(
    modifier: Modifier = Modifier,
    viewModel: VideoPlaylistsTabViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    VideoPlaylistsTabScreen(
        uiState = uiState,
        modifier = modifier
    )
}

@Composable
internal fun VideoPlaylistsTabScreen(
    uiState: VideoPlaylistsTabUiState,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()

    when (uiState) {
        is VideoPlaylistsTabUiState.Loading -> NodesViewSkeleton(
            modifier = modifier.testTag(VIDEO_PLAYLISTS_TAB_LOADING_VIEW_TEST_TAG),
            isListView = true,
            contentPadding = PaddingValues()
        )

        is VideoPlaylistsTabUiState.Data -> {
            if (uiState.videoPlaylistEntities.isEmpty()) {
                MegaEmptyView(
                    modifier = modifier.testTag(VIDEO_PLAYLISTS_TAB_EMPTY_VIEW_TEST_TAG),
                    text = stringResource(id = sharedR.string.video_section_playlists_empty_hint_playlist),
                    imagePainter = painterResource(id = iconPackR.drawable.ic_playlist_glass)
                )
            } else {
                val items = uiState.videoPlaylistEntities
                FastScrollLazyColumn(
                    state = lazyListState,
                    totalItems = items.size,
                    modifier = modifier.testTag(VIDEO_PLAYLISTS_TAB_ALL_PLAYLISTS_VIEW_TEST_TAG)
                ) {
                    items(count = items.size, key = { items[it].id.longValue }) {
                        val videoPlaylistItem = items[it]
                        VideoPlaylistItemView(
                            emptyPlaylistIcon = iconPackR.drawable.ic_video_playlist_default_thumbnail,
                            noThumbnailIcon = iconPackR.drawable.ic_video_playlist_no_thumbnail,
                            title = videoPlaylistItem.title,
                            numberOfVideos = videoPlaylistItem.numberOfVideos,
                            thumbnailList = videoPlaylistItem.thumbnailList?.map { id ->
                                ThumbnailRequest(id)
                            },
                            totalDuration = videoPlaylistItem.totalDuration,
                            isSelected = videoPlaylistItem.isSelected,
                            isSystemVideoPlaylist = videoPlaylistItem.isSystemVideoPlayer,
                            onClick = {},
                            onMenuClick = {},
                            onLongClick = {}
                        )
                    }
                }
            }
        }
    }
}

/**
 * Test tag for loading view.
 */
const val VIDEO_PLAYLISTS_TAB_LOADING_VIEW_TEST_TAG = "video_playlists_tab:loading_view"

/**
 * Test tag for empty view
 */
const val VIDEO_PLAYLISTS_TAB_EMPTY_VIEW_TEST_TAG = "video_playlists_tab:empty_view"

/**
 * Test tag for the video playlist tab all video playlists
 */
const val VIDEO_PLAYLISTS_TAB_ALL_PLAYLISTS_VIEW_TEST_TAG = "video_playlists_tab:view_all_playlists"