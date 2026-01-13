package mega.privacy.android.feature.photos.presentation.playlists.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollLazyColumn
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.core.nodecomponents.list.NodeLabelCircle
import mega.privacy.android.core.nodecomponents.list.NodesViewSkeleton
import mega.privacy.android.core.sharedcomponents.empty.MegaEmptyView
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.feature.photos.components.VideoItemView
import mega.privacy.android.feature.photos.components.VideoPlaylistDetailHeaderView
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.shared.resources.R as sharedR

@Composable
fun VideoPlaylistDetailRoute(
    navigationHandler: NavigationHandler,
    viewModel: VideoPlaylistDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    VideoPlaylistDetailScreen(
        uiState = uiState,
        onBack = navigationHandler::back
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlaylistDetailScreen(
    uiState: VideoPlaylistDetailUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        topBar = {
            MegaTopAppBar(
                modifier = Modifier
                    .testTag(VIDEO_PLAYLISTS_DETAIL_APP_BAR_VIEW_TEST_TAG),
                navigationType = AppBarNavigationType.Back(onBack),
                title = (uiState as? VideoPlaylistDetailUiState.Data)?.currentPlaylist?.title ?: "",
                actions = emptyList()
            )
        }
    ) { innerPadding ->
        when (uiState) {
            is VideoPlaylistDetailUiState.Loading -> NodesViewSkeleton(
                modifier = modifier
                    .padding(innerPadding)
                    .testTag(VIDEO_PLAYLIST_DETAIL_LOADING_VIEW_TEST_TAG),
                isListView = true,
                contentPadding = PaddingValues()
            )

            is VideoPlaylistDetailUiState.Data -> {
                if (uiState.currentPlaylist == null
                    || uiState.currentPlaylist.videos == null
                    || uiState.currentPlaylist.videos.isEmpty()
                ) {
                    VideoPlaylistDetailEmptyView(
                        title = uiState.currentPlaylist?.title,
                        totalDuration = uiState.currentPlaylist?.totalDuration,
                        numberOfVideos = uiState.currentPlaylist?.numberOfVideos,
                        modifier = modifier.padding(innerPadding)
                    )
                } else {
                    val items = uiState.currentPlaylist.videos
                    FastScrollLazyColumn(
                        state = lazyListState,
                        totalItems = items.size,
                        modifier = Modifier
                            .padding(innerPadding)
                            .testTag(VIDEO_PLAYLISTS_DETAIL_PLAYLIST_DETAIL_VIEW_TEST_TAG)
                    ) {
                        item(key = "header") {
                            VideoPlaylistDetailHeaderView(
                                thumbnailList =
                                    uiState.currentPlaylist.thumbnailList?.map { id ->
                                        ThumbnailRequest(id)
                                    },
                                title = uiState.currentPlaylist.title,
                                totalDuration = uiState.currentPlaylist.totalDuration,
                                numberOfVideos = uiState.currentPlaylist.numberOfVideos,
                                modifier = Modifier.padding(16.dp),
                                onPlayAllClicked = {}
                            )
                        }

                        items(items = items, key = { it.id.longValue }) { videoItem ->
                            VideoItemView(
                                icon = iconPackR.drawable.ic_video_section_video_default_thumbnail,
                                name = videoItem.name,
                                description = videoItem.description?.replace("\n", " "),
                                fileSize = formatFileSize(videoItem.size, LocalContext.current),
                                duration = videoItem.durationString,
                                isFavourite = videoItem.isFavourite,
                                isSelected = videoItem.isSelected,
                                isSharedWithPublicLink = videoItem.isSharedItems,
                                labelView = {
                                    videoItem.nodeLabel?.let { label ->
                                        NodeLabelCircle(
                                            modifier = Modifier.padding(start = 10.dp),
                                            label = label
                                        )
                                    }
                                },
                                thumbnailData = ThumbnailRequest(videoItem.id),
                                nodeAvailableOffline = videoItem.nodeAvailableOffline,
                                onClick = {},
                                onMenuClick = {},
                                onLongClick = {},
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPlaylistDetailEmptyView(
    title: String?,
    totalDuration: String?,
    numberOfVideos: Int?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.testTag(VIDEO_PLAYLIST_DETAIL_VIDEOS_EMPTY_VIEW_TEST_TAG)) {
        VideoPlaylistDetailHeaderView(
            thumbnailList = null,
            title = title,
            totalDuration = totalDuration,
            numberOfVideos = numberOfVideos,
            modifier = Modifier.padding(16.dp),
            onPlayAllClicked = {}
        )

        MegaEmptyView(
            text = stringResource(id = sharedR.string.videos_tab_empty_hint_video),
            imagePainter = painterResource(id = iconPackR.drawable.ic_video_glass)
        )
    }
}

/**
 * Test tag for the video playlist detail loading view
 */
const val VIDEO_PLAYLIST_DETAIL_LOADING_VIEW_TEST_TAG = "video_playlist_detail:view_loading"

/**
 * Test tag for the video playlist detail videos empty view
 */
const val VIDEO_PLAYLIST_DETAIL_VIDEOS_EMPTY_VIEW_TEST_TAG =
    "video_playlist_detail:view_videos_empty"

/**
 * Test tag for the video playlist detail view
 */
const val VIDEO_PLAYLISTS_DETAIL_PLAYLIST_DETAIL_VIEW_TEST_TAG =
    "video_playlists_detail:view_playlist_detail"

/**
 * Test tag for the video playlist detail app bar
 */
const val VIDEO_PLAYLISTS_DETAIL_APP_BAR_VIEW_TEST_TAG = "video_playlists_detail:view_app_bar"
