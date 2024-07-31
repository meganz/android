package mega.privacy.android.app.presentation.videosection.view

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.presentation.videosection.VideoSectionViewModel
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistUIEntity
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction
import mega.privacy.android.app.presentation.videosection.model.VideoSectionTab
import mega.privacy.android.app.presentation.videosection.model.VideoUIEntity
import mega.privacy.android.app.presentation.videosection.view.allvideos.AllVideosView
import mega.privacy.android.app.presentation.videosection.view.playlist.VideoPlaylistsView
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
internal fun VideoSectionComposeView(
    videoSectionViewModel: VideoSectionViewModel,
    onClick: (item: VideoUIEntity, index: Int) -> Unit,
    onSortOrderClick: () -> Unit,
    onMenuClick: (VideoUIEntity) -> Unit,
    onLongClick: (item: VideoUIEntity, index: Int) -> Unit,
    onPlaylistItemClick: (item: VideoPlaylistUIEntity, index: Int) -> Unit,
    onPlaylistItemLongClick: (VideoPlaylistUIEntity, index: Int) -> Unit,
    onDeleteDialogButtonClicked: () -> Unit,
    onMenuAction: (VideoSectionMenuAction?) -> Unit,
) {
    val uiState by videoSectionViewModel.state.collectAsStateWithLifecycle()
    val tabState by videoSectionViewModel.tabState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    val allLazyListState = rememberLazyListState()
    val playlistsLazyListState = rememberLazyListState()

    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val pagerState = rememberPagerState(
        initialPage = tabState.selectedTab.ordinal,
        initialPageOffsetFraction = 0f
    ) {
        tabState.tabs.size
    }

    val accountType = uiState.accountDetail?.levelDetail?.accountType

    var showDeleteVideoPlaylist by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            videoSectionViewModel.onTabSelected(selectTab = tabState.tabs[page])
            val tab = VideoSectionTab.entries[page]
            pagerState.scrollToPage(tab.ordinal)
        }
    }

    val isBackHandlerEnabled =
        uiState.isInSelection || uiState.searchState == SearchWidgetState.EXPANDED

    BackHandler(isBackHandlerEnabled) {
        when {
            uiState.isInSelection -> {
                videoSectionViewModel.clearAllSelectedVideos()
                videoSectionViewModel.clearAllSelectedVideoPlaylists()
            }

            uiState.searchState == SearchWidgetState.EXPANDED ->
                videoSectionViewModel.exitSearch()

            else ->
                onBackPressedDispatcher?.onBackPressed()
        }
    }

    MegaScaffold(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        topBar = {
            VideoSectionTopBar(
                tab = tabState.selectedTab,
                title = stringResource(R.string.sortby_type_video_first),
                isActionMode = uiState.isInSelection,
                selectedSize = if (tabState.selectedTab == VideoSectionTab.All)
                    uiState.selectedVideoHandles.size
                else
                    uiState.selectedVideoPlaylistHandles.size,
                searchState = uiState.searchState,
                query = uiState.query,
                onMenuActionClicked = { action ->
                    when (action) {
                        is VideoSectionMenuAction.VideoSectionUnhideAction -> {
                            coroutineScope.launch {
                                videoSectionViewModel.hideOrUnhideNodes(
                                    nodeIds = videoSectionViewModel.getSelectedNodes()
                                        .map { it.id },
                                    hide = false,
                                )
                            }
                            videoSectionViewModel.clearAllSelectedVideos()
                        }

                        is VideoSectionMenuAction.VideoSectionSelectAllAction ->
                            if (tabState.selectedTab == VideoSectionTab.All) {
                                videoSectionViewModel.selectAllNodes()
                            } else {
                                videoSectionViewModel.selectAllVideoPlaylists()
                            }


                        is VideoSectionMenuAction.VideoSectionClearSelectionAction ->
                            if (tabState.selectedTab == VideoSectionTab.All) {
                                videoSectionViewModel.clearAllSelectedVideos()
                            } else {
                                videoSectionViewModel.clearAllSelectedVideoPlaylists()
                            }

                        is VideoSectionMenuAction.VideoSectionRemoveAction ->
                            showDeleteVideoPlaylist = true

                        else -> onMenuAction(action)
                    }
                },
                onSearchTextChanged = videoSectionViewModel::searchQuery,
                onCloseClicked = videoSectionViewModel::exitSearch,
                onSearchClicked = videoSectionViewModel::searchWidgetStateUpdate,
                onBackPressed = {
                    when {
                        uiState.isInSelection -> {
                            videoSectionViewModel.clearAllSelectedVideos()
                            videoSectionViewModel.clearAllSelectedVideoPlaylists()
                        }

                        uiState.searchState == SearchWidgetState.EXPANDED ->
                            videoSectionViewModel.exitSearch()

                        else ->
                            onBackPressedDispatcher?.onBackPressed()
                    }
                },
                isHideMenuActionVisible = uiState.isHideMenuActionVisible,
                isUnhideMenuActionVisible = uiState.isUnhideMenuActionVisible,
                isRemoveLinkMenuActionVisible = uiState.isRemoveLinkMenuActionVisible
            )
        }
    ) { paddingValues ->
        VideoSectionBodyView(
            modifier = Modifier.padding(paddingValues),
            pagerState = pagerState,
            tabs = tabState.tabs,
            allVideoView = {
                AllVideosView(
                    items = uiState.allVideos,
                    accountType = accountType,
                    progressBarShowing = uiState.progressBarShowing,
                    searchMode = uiState.searchState == SearchWidgetState.EXPANDED,
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
                    searchMode = uiState.searchState == SearchWidgetState.EXPANDED,
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
                    onCreateDialogPositiveButtonClicked = videoSectionViewModel::createNewPlaylist,
                    onRenameDialogPositiveButtonClicked = videoSectionViewModel::updateVideoPlaylistTitle,
                    showDeleteVideoPlaylistDialog = showDeleteVideoPlaylist,
                    updateShowDeleteVideoPlaylist = { showDeleteVideoPlaylist = it },
                    onDeleteDialogPositiveButtonClicked = { playlist ->
                        showDeleteVideoPlaylist = false
                        videoSectionViewModel.removeVideoPlaylists(listOf(playlist))
                    },
                    onDeletedMessageShown = videoSectionViewModel::clearDeletedVideoPlaylistTitles,
                    deletedVideoPlaylistTitles = uiState.deletedVideoPlaylistTitles,
                    onDeletePlaylistsDialogPositiveButtonClicked = {
                        showDeleteVideoPlaylist = false
                        val removedPlaylists =
                            uiState.selectedVideoPlaylistHandles.mapNotNull {
                                uiState.videoPlaylists.firstOrNull { playlist ->
                                    playlist.id.longValue == it
                                }
                            }
                        videoSectionViewModel.removeVideoPlaylists(removedPlaylists)
                        onDeleteDialogButtonClicked()
                    },
                    onDeleteDialogNegativeButtonClicked = videoSectionViewModel::clearAllSelectedVideoPlaylists
                )
            },
            selectedTab = tabState.selectedTab,
            allLazyListState = allLazyListState,
            playlistsLazyListState = playlistsLazyListState,
            onTabSelected = { tab ->
                if (uiState.searchState == SearchWidgetState.COLLAPSED && !uiState.isInSelection) {
                    videoSectionViewModel.onTabSelected(selectTab = tab)
                    coroutineScope.launch {
                        pagerState.scrollToPage(tab.ordinal)
                    }
                }
            },
            swipeEnabled = uiState.searchState == SearchWidgetState.COLLAPSED && !uiState.isInSelection
        )
    }
}

internal const val videoSectionRoute = "videoSection/video_section"