package mega.privacy.android.app.presentation.videosection.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.presentation.videosection.VideoSectionViewModel
import mega.privacy.android.app.presentation.videosection.model.VideoUIEntity
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistUIEntity
import mega.privacy.android.app.presentation.videosection.model.VideoSectionTab
import mega.privacy.android.app.presentation.videosection.view.allvideos.AllVideosView
import mega.privacy.android.app.presentation.videosection.view.playlist.VideoPlaylistsView

internal const val videoSectionRoute = "videoSection/video_section"

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun VideoSectionComposeView(
    videoSectionViewModel: VideoSectionViewModel,
    onClick: (item: VideoUIEntity, index: Int) -> Unit,
    onSortOrderClick: () -> Unit = {},
    onMenuClick: (VideoUIEntity) -> Unit = {},
    onLongClick: (item: VideoUIEntity, index: Int) -> Unit = { _, _ -> },
    onPlaylistItemClick: (item: VideoPlaylistUIEntity, index: Int) -> Unit = { _, _ -> },
    onPlaylistItemMenuClick: (VideoPlaylistUIEntity) -> Unit = { _ -> },
    onPlaylistItemLongClick: (VideoPlaylistUIEntity, index: Int) -> Unit = { _, _ -> },
) {
    val uiState by videoSectionViewModel.state.collectAsStateWithLifecycle()
    val tabState by videoSectionViewModel.tabState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    val allLazyListState = rememberLazyListState()
    val playlistsLazyListState = rememberLazyListState()

    val pagerState = rememberPagerState(
        initialPage = tabState.selectedTab.ordinal,
        initialPageOffsetFraction = 0f
    ) {
        tabState.tabs.size
    }

    LaunchedEffect(Unit) {
        videoSectionViewModel.updateCurrentVideoPlaylist(null)
    }

    LaunchedEffect(pagerState.currentPage) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            videoSectionViewModel.onTabSelected(selectTab = tabState.tabs[page])
            val tab = VideoSectionTab.entries[page]
            pagerState.scrollToPage(tab.ordinal)
        }
    }

    VideoSectionBodyView(
        pagerState = pagerState,
        tabs = tabState.tabs,
        allVideoView = {
            AllVideosView(
                items = uiState.allVideos,
                progressBarShowing = uiState.progressBarShowing,
                searchMode = uiState.searchMode,
                scrollToTop = uiState.scrollToTop,
                lazyListState = allLazyListState,
                sortOrder = stringResource(
                    id = SortByHeaderViewModel.orderNameMap[uiState.sortOrder]
                        ?: R.string.sortby_name
                ),
                modifier = Modifier,
                onSortOrderClick = onSortOrderClick,
                onClick = onClick,
                onLongClick = onLongClick,
                onMenuClick = onMenuClick
            )
        },
        playlistsView = {
            VideoPlaylistsView(
                items = uiState.videoPlaylists,
                progressBarShowing = uiState.isPlaylistProgressBarShown,
                searchMode = uiState.searchMode,
                scrollToTop = uiState.scrollToTop,
                lazyListState = playlistsLazyListState,
                sortOrder = stringResource(
                    id = SortByHeaderViewModel.orderNameMap[uiState.sortOrder]
                        ?: R.string.sortby_name
                ),
                errorMessage = uiState.createDialogErrorMessage,
                modifier = Modifier,
                onSortOrderClick = onSortOrderClick,
                onClick = onPlaylistItemClick,
                onLongClick = onPlaylistItemLongClick,
                onMenuClick = onPlaylistItemMenuClick,
                setDialogInputPlaceholder = videoSectionViewModel::setPlaceholderTitle,
                isInputTitleValid = uiState.isInputTitleValid,
                showCreateVideoPlaylistDialog = uiState.shouldCreateVideoPlaylistDialog,
                inputPlaceHolderText = uiState.createVideoPlaylistPlaceholderTitle,
                setShowCreateVideoPlaylistDialog = videoSectionViewModel::setShowCreateVideoPlaylistDialog,
                setInputValidity = videoSectionViewModel::setNewPlaylistTitleValidity,
                onDialogPositiveButtonClicked = videoSectionViewModel::createNewPlaylist
            )
        },
        selectedTab = tabState.selectedTab,
        allLazyListState = allLazyListState,
        playlistsLazyListState = playlistsLazyListState,
        onTabSelected = { tab ->
            videoSectionViewModel.onTabSelected(selectTab = tab)
            coroutineScope.launch {
                pagerState.scrollToPage(tab.ordinal)
            }
        }
    )
}