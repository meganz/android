package mega.privacy.android.app.presentation.videosection.view

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.presentation.extensions.getStorageState
import mega.privacy.android.app.presentation.node.NodeActionHandler
import mega.privacy.android.app.presentation.node.view.toolbar.NodeToolbarViewModel
import mega.privacy.android.app.presentation.videosection.VideoSectionViewModel
import mega.privacy.android.app.presentation.videosection.model.DurationFilterOption
import mega.privacy.android.app.presentation.videosection.model.LocationFilterOption
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistUIEntity
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction
import mega.privacy.android.app.presentation.videosection.model.VideoSectionTab
import mega.privacy.android.app.presentation.videosection.model.VideoUIEntity
import mega.privacy.android.app.presentation.videosection.model.VideosFilterOptionEntity
import mega.privacy.android.app.presentation.videosection.view.allvideos.AllVideosView
import mega.privacy.android.app.presentation.videosection.view.allvideos.VideosFilterBottomSheet
import mega.privacy.android.app.presentation.videosection.view.playlist.VideoPlaylistBottomSheet
import mega.privacy.android.app.presentation.videosection.view.playlist.VideoPlaylistsView
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.DurationFilterAllDurationsClickedEvent
import mega.privacy.mobile.analytics.event.DurationFilterBetween10and60SecondsClickedEvent
import mega.privacy.mobile.analytics.event.DurationFilterBetween1and4MinutesClickedEvent
import mega.privacy.mobile.analytics.event.DurationFilterBetween4and20MinutesClickedEvent
import mega.privacy.mobile.analytics.event.DurationFilterLessThan10SecondsClickedEvent
import mega.privacy.mobile.analytics.event.DurationFilterMoreThan20MinutesClickedEvent
import mega.privacy.mobile.analytics.event.LocationFilterAllLocationsClickedEvent
import mega.privacy.mobile.analytics.event.LocationFilterCameraUploadClickedEvent
import mega.privacy.mobile.analytics.event.LocationFilterCloudDriveClickedEvent
import mega.privacy.mobile.analytics.event.LocationFilterSharedItemClickedEvent
import mega.privacy.mobile.analytics.event.VideosScreenBackNavigationEvent

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
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
    retryActionCallback: () -> Unit,
    navHostController: NavHostController = rememberNavController(),
    handler: NodeActionHandler? = null,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    toolbarViewModel: NodeToolbarViewModel = hiltViewModel(),
) {
    val uiState by videoSectionViewModel.state.collectAsStateWithLifecycle()
    val tabState by videoSectionViewModel.tabState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var showLocationBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showDurationBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showPlaylistBottomSheet by rememberSaveable { mutableStateOf(false) }

    val locationModalSheetState = rememberModalBottomSheetState(true)
    val durationModalSheetState = rememberModalBottomSheetState(true)
    val playlistModalSheetState = rememberModalBottomSheetState(true)

    val allLazyListState = rememberLazyListState()
    val playlistsLazyListState = rememberLazyListState()

    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val pagerState = rememberPagerState(
        initialPage = tabState.selectedTab.ordinal,
        initialPageOffsetFraction = 0f
    ) {
        tabState.tabs.size
    }

    val locationTitle =
        stringResource(id = sharedR.string.video_section_videos_location_filter_title)
    val durationTitle =
        stringResource(id = sharedR.string.video_section_videos_duration_filter_title)

    var showDeleteVideoPlaylist by rememberSaveable { mutableStateOf(false) }
    var showRenameVideoPlaylistDialog by rememberSaveable { mutableStateOf(false) }

    val toolbarState by toolbarViewModel.state.collectAsStateWithLifecycle()
    val isVideoSectionActivityEnabled = handler != null

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

    if (isVideoSectionActivityEnabled) {
        LaunchedEffect(key1 = uiState.selectedVideoHandles) {
            toolbarViewModel.updateToolbarState(
                selectedNodes = videoSectionViewModel.getSelectedNodes().toSet(),
                resultCount = uiState.allVideos.size,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE
            )
        }
    }

    MegaScaffold(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        contentWindowInsets = WindowInsets.ime,
        scaffoldState = scaffoldState,
        topBar = {
            if (isVideoSectionActivityEnabled) {
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
                            is VideoSectionMenuAction.VideoSectionSelectAllAction ->
                                videoSectionViewModel.selectAllVideoPlaylists()

                            is VideoSectionMenuAction.VideoSectionClearSelectionAction ->
                                videoSectionViewModel.clearAllSelectedVideoPlaylists()

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

                            else -> {
                                Analytics.tracker.trackEvent(VideosScreenBackNavigationEvent)
                                onBackPressedDispatcher?.onBackPressed()
                            }
                        }
                    },
                    menuItems = toolbarState.toolbarMenuItems,
                    handler = handler,
                    navHostController = navHostController,
                    clearSelection = videoSectionViewModel::clearAllSelectedVideos,
                )
            } else {
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
                    isRemoveLinkMenuActionVisible = uiState.isRemoveLinkMenuActionVisible,
                )
            }
        }
    ) { paddingValues ->
        VideoSectionBodyView(
            modifier = Modifier.padding(paddingValues),
            pagerState = pagerState,
            tabs = tabState.tabs,
            allVideoView = {
                AllVideosView(
                    items = uiState.allVideos,
                    highlightText = uiState.highlightText,
                    shouldApplySensitiveMode = uiState.hiddenNodeEnabled
                            && uiState.accountType?.isPaid == true
                            && !uiState.isBusinessAccountExpired,
                    progressBarShowing = uiState.progressBarShowing,
                    scrollToTop = uiState.scrollToTop,
                    lazyListState = allLazyListState,
                    sortOrder = stringResource(
                        id = SortByHeaderViewModel.orderNameMap[uiState.sortOrder]
                            ?: R.string.sortby_name
                    ),
                    selectedDurationFilterOption = uiState.durationSelectedFilterOption,
                    selectedLocationFilterOption = uiState.locationSelectedFilterOption,
                    onLocationFilterClicked = {
                        coroutineScope.launch {
                            showLocationBottomSheet = true
                            locationModalSheetState.show()
                        }
                    },
                    onDurationFilterClicked = {
                        coroutineScope.launch {
                            showDurationBottomSheet = true
                            durationModalSheetState.show()
                        }
                    },
                    modifier = Modifier,
                    onSortOrderClick = onSortOrderClick,
                    onClick = onClick,
                    onLongClick = onLongClick,
                    onMenuClick = onMenuClick,
                    addToPlaylistsTitles = uiState.addToPlaylistTitles,
                    clearAddToPlaylistsTitles = {
                        videoSectionViewModel.updateAddToPlaylistHandle(null)
                        videoSectionViewModel.updateAddToPlaylistTitles(null)
                    },
                    retryActionCallback = retryActionCallback
                )
            },
            playlistsView = {
                VideoPlaylistsView(
                    items = uiState.videoPlaylists,
                    progressBarShowing = uiState.isPlaylistProgressBarShown,
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
                    isInputTitleValid = uiState.isInputTitleValid,
                    inputPlaceHolderText = uiState.createVideoPlaylistPlaceholderTitle,
                    setInputValidity = videoSectionViewModel::setNewPlaylistTitleValidity,
                    onCreateDialogPositiveButtonClicked = videoSectionViewModel::createNewPlaylist,
                    onRenameDialogPositiveButtonClicked = videoSectionViewModel::updateVideoPlaylistTitle,
                    showDeleteVideoPlaylistDialog = showDeleteVideoPlaylist,
                    showRenameVideoPlaylistDialog = showRenameVideoPlaylistDialog,
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
                    onDeleteDialogNegativeButtonClicked = videoSectionViewModel::clearAllSelectedVideoPlaylists,
                    updateShowRenameVideoPlaylist = { showRenameVideoPlaylistDialog = it },
                    onMenuClick = {
                        coroutineScope.launch {
                            showPlaylistBottomSheet = true
                            playlistModalSheetState.show()
                        }
                    },
                    showCreateVideoPlaylistDialog = uiState.showCreatedDialog,
                    updateShowCreateVideoPlaylist = videoSectionViewModel::updateShowCreateDialog
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

    if (showLocationBottomSheet) {
        VideosFilterBottomSheet(
            modifier = Modifier,
            sheetState = locationModalSheetState,
            title = locationTitle,
            options = LocationFilterOption.entries.map { option ->
                VideosFilterOptionEntity(
                    id = option.ordinal,
                    title = stringResource(id = option.titleResId),
                    isSelected = option == uiState.locationSelectedFilterOption
                )
            },
            onDismissRequest = {
                coroutineScope.launch {
                    showLocationBottomSheet = false
                    locationModalSheetState.hide()
                }
            },
            onItemSelected = { item ->
                coroutineScope.launch {
                    showLocationBottomSheet = false
                    locationModalSheetState.hide()
                }
                val locationOption =
                    if (item.id in LocationFilterOption.entries.indices) {
                        LocationFilterOption.entries.firstOrNull { it.ordinal == item.id }
                            ?: LocationFilterOption.AllLocations
                    } else {
                        LocationFilterOption.AllLocations
                    }
                when (locationOption) {
                    LocationFilterOption.AllLocations ->
                        Analytics.tracker.trackEvent(LocationFilterAllLocationsClickedEvent)

                    LocationFilterOption.CloudDrive ->
                        Analytics.tracker.trackEvent(LocationFilterCloudDriveClickedEvent)

                    LocationFilterOption.CameraUploads ->
                        Analytics.tracker.trackEvent(LocationFilterCameraUploadClickedEvent)

                    LocationFilterOption.SharedItems ->
                        Analytics.tracker.trackEvent(LocationFilterSharedItemClickedEvent)
                }
                videoSectionViewModel.setLocationSelectedFilterOption(locationOption)
            }
        )
    }

    if (showDurationBottomSheet) {
        VideosFilterBottomSheet(
            modifier = Modifier,
            sheetState = durationModalSheetState,
            title = durationTitle,
            options = DurationFilterOption.entries.map { option ->
                VideosFilterOptionEntity(
                    id = option.ordinal,
                    title = stringResource(id = option.titleResId),
                    isSelected = option == uiState.durationSelectedFilterOption
                )
            },
            onDismissRequest = {
                coroutineScope.launch {
                    showDurationBottomSheet = false
                    durationModalSheetState.hide()
                }
            },
            onItemSelected = { item ->
                coroutineScope.launch {
                    showDurationBottomSheet = false
                    durationModalSheetState.hide()
                }
                val durationOption =
                    if (item.id in DurationFilterOption.entries.indices) {
                        DurationFilterOption.entries.firstOrNull { it.ordinal == item.id }
                            ?: DurationFilterOption.AllDurations
                    } else {
                        DurationFilterOption.AllDurations
                    }

                when (durationOption) {
                    DurationFilterOption.AllDurations ->
                        Analytics.tracker.trackEvent(DurationFilterAllDurationsClickedEvent)

                    DurationFilterOption.LessThan10Seconds ->
                        Analytics.tracker.trackEvent(DurationFilterLessThan10SecondsClickedEvent)

                    DurationFilterOption.Between10And60Seconds ->
                        Analytics.tracker.trackEvent(DurationFilterBetween10and60SecondsClickedEvent)

                    DurationFilterOption.Between1And4 ->
                        Analytics.tracker.trackEvent(DurationFilterBetween1and4MinutesClickedEvent)

                    DurationFilterOption.Between4And20 ->
                        Analytics.tracker.trackEvent(DurationFilterBetween4and20MinutesClickedEvent)

                    DurationFilterOption.MoreThan20 ->
                        Analytics.tracker.trackEvent(DurationFilterMoreThan20MinutesClickedEvent)
                }
                videoSectionViewModel.setDurationSelectedFilterOption(durationOption)
            }
        )
    }

    if (showPlaylistBottomSheet) {
        VideoPlaylistBottomSheet(
            sheetState = playlistModalSheetState,
            onDismissRequest = {
                coroutineScope.launch {
                    showPlaylistBottomSheet = false
                    playlistModalSheetState.hide()
                }
            },
            onRenameVideoPlaylistClicked = {
                coroutineScope.launch {
                    showPlaylistBottomSheet = false
                    playlistModalSheetState.hide()
                }
                if (getStorageState() == StorageState.PayWall) {
                    showOverDiskQuotaPaywallWarning()
                } else {
                    showRenameVideoPlaylistDialog = true
                }
            },
            onDeleteVideoPlaylistClicked = {
                coroutineScope.launch {
                    showPlaylistBottomSheet = false
                    playlistModalSheetState.hide()
                }
                if (getStorageState() == StorageState.PayWall) {
                    showOverDiskQuotaPaywallWarning()
                } else {
                    showDeleteVideoPlaylist = true
                }
            }
        )
    }
}

internal const val videoSectionRoute = "videoSection/video_section"