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
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistUIEntity
import mega.privacy.android.app.presentation.videosection.model.VideoSectionTab
import mega.privacy.android.app.presentation.videosection.model.VideoUIEntity
import mega.privacy.android.app.presentation.videosection.view.allvideos.AllVideosView
import mega.privacy.android.app.presentation.videosection.view.playlist.VideoPlaylistsView
import mega.privacy.android.domain.entity.SortOrder

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun VideoSectionComposeView(
    videoSectionViewModel: VideoSectionViewModel,
    onClick: (item: VideoUIEntity, index: Int) -> Unit,
    onSortOrderClick: () -> Unit,
    onMenuClick: (VideoUIEntity) -> Unit,
    onLongClick: (item: VideoUIEntity, index: Int) -> Unit,
    onPlaylistItemClick: (item: VideoPlaylistUIEntity, index: Int) -> Unit,
    onPlaylistItemMenuClick: (VideoPlaylistUIEntity) -> Unit,
    onPlaylistItemLongClick: (VideoPlaylistUIEntity, index: Int) -> Unit,
    onDeleteDialogButtonClicked: () -> Unit,
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
                selectedDurationFilterOption = uiState.durationSelectedFilterOption,
                selectedLocationFilterOption = uiState.locationSelectedFilterOption,
                onLocationFilterItemClicked = videoSectionViewModel::setLocationSelectedFilterOption,
                onDurationFilterItemClicked = videoSectionViewModel::setDurationSelectedFilterOption,
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
                    id = SortByHeaderViewModel.orderNameMap[
                        when (uiState.sortOrder) {
                            SortOrder.ORDER_SIZE_DESC,
                            SortOrder.ORDER_SIZE_ASC,
                            SortOrder.ORDER_FAV_ASC,
                            SortOrder.ORDER_FAV_DESC,
                            SortOrder.ORDER_LABEL_ASC,
                            SortOrder.ORDER_LABEL_DESC,
                            -> SortOrder.ORDER_DEFAULT_ASC

                            else -> uiState.sortOrder
                        }
                    ]
                        ?: R.string.sortby_name
                ),
                errorMessage = uiState.createDialogErrorMessage,
                modifier = Modifier,
                onSortOrderClick = onSortOrderClick,
                onClick = onPlaylistItemClick,
                onLongClick = onPlaylistItemLongClick,
                setDialogInputPlaceholder = videoSectionViewModel::setPlaceholderTitle,
                isInputTitleValid = uiState.isInputTitleValid,
                inputPlaceHolderText = uiState.createVideoPlaylistPlaceholderTitle,
                setInputValidity = videoSectionViewModel::setNewPlaylistTitleValidity,
                shouldCreateVideoPlaylistDialog = uiState.shouldCreateVideoPlaylist,
                setShouldCreateVideoPlaylist = videoSectionViewModel::setShouldCreateVideoPlaylist,
                onCreateDialogPositiveButtonClicked = videoSectionViewModel::createNewPlaylist,
                shouldRenameVideoPlaylistDialog = uiState.shouldRenameVideoPlaylist,
                setShouldRenameVideoPlaylist = videoSectionViewModel::setShouldRenameVideoPlaylist,
                onRenameDialogPositiveButtonClicked = videoSectionViewModel::updateVideoPlaylistTitle,
                shouldDeleteVideoPlaylistDialog = uiState.shouldDeleteVideoPlaylist,
                setShouldDeleteVideoPlaylist = videoSectionViewModel::setShouldDeleteVideoPlaylist,
                onDeleteDialogPositiveButtonClicked = { playlist ->
                    videoSectionViewModel.setShouldDeleteSingleVideoPlaylist(false)
                    videoSectionViewModel.removeVideoPlaylists(listOf(playlist))
                },
                onDeletedMessageShown = videoSectionViewModel::clearDeletedVideoPlaylistTitles,
                deletedVideoPlaylistTitles = uiState.deletedVideoPlaylistTitles,
                onDeletePlaylistsDialogPositiveButtonClicked = {
                    videoSectionViewModel.setShouldDeleteVideoPlaylist(false)
                    val removedPlaylists =
                        uiState.selectedVideoPlaylistHandles.mapNotNull {
                            uiState.videoPlaylists.firstOrNull { playlist ->
                                playlist.id.longValue == it
                            }
                        }
                    videoSectionViewModel.removeVideoPlaylists(removedPlaylists)
                    onDeleteDialogButtonClicked()
                }
            )
        },
        selectedTab = tabState.selectedTab,
        allLazyListState = allLazyListState,
        playlistsLazyListState = playlistsLazyListState,
        onTabSelected = { tab ->
            if (!uiState.searchMode && !uiState.actionMode) {
                videoSectionViewModel.onTabSelected(selectTab = tab)
                coroutineScope.launch {
                    pagerState.scrollToPage(tab.ordinal)
                }
            }
        },
        swipeEnabled = !uiState.searchMode && !uiState.actionMode
    )
}

internal const val videoSectionRoute = "videoSection/video_section"