@file:OptIn(ExperimentalMaterial3Api::class)

package mega.privacy.android.feature.photos.presentation

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.tabs.MegaScrollableTabRow
import mega.android.core.ui.model.SnackbarAttributes
import mega.android.core.ui.model.TabItems
import mega.android.core.ui.modifiers.excludeTopPadding
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.action.MultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberMultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.components.AddContentFab
import mega.privacy.android.core.nodecomponents.model.NodeActionState
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.feature.photos.extensions.toTrackingEvent
import mega.privacy.android.feature.photos.model.FilterMediaSource.Companion.toLegacyPhotosSource
import mega.privacy.android.feature.photos.model.MediaAppBarAction
import mega.privacy.android.feature.photos.model.MediaScreen
import mega.privacy.android.feature.photos.model.PhotoNodeUiState
import mega.privacy.android.feature.photos.model.TimelineGridSize
import mega.privacy.android.feature.photos.presentation.albums.AlbumsTabRoute
import mega.privacy.android.feature.photos.presentation.albums.AlbumsTabUiState
import mega.privacy.android.feature.photos.presentation.albums.AlbumsTabViewModel
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumSelectionAction
import mega.privacy.android.feature.photos.presentation.component.MediaBottomBar
import mega.privacy.android.feature.photos.presentation.component.MediaTopBar
import mega.privacy.android.feature.photos.presentation.effects.MediaMainEffects
import mega.privacy.android.feature.photos.presentation.effects.MediaNodeActionEffects
import mega.privacy.android.feature.photos.presentation.handler.MediaSelectionModeType
import mega.privacy.android.feature.photos.presentation.handler.MediaSelectionModeType.Companion.isAnActiveSelection
import mega.privacy.android.feature.photos.presentation.handler.MediaSelectionModelHandler
import mega.privacy.android.feature.photos.presentation.playlists.VideoPlaylistsTabRoute
import mega.privacy.android.feature.photos.presentation.playlists.VideoPlaylistsTabUiState
import mega.privacy.android.feature.photos.presentation.playlists.VideoPlaylistsTabViewModel
import mega.privacy.android.feature.photos.presentation.timeline.TimelineFilterUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabActionUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabRoute
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabSortOptions
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabSortOptions.Companion.toLegacySort
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabViewModel
import mega.privacy.android.feature.photos.presentation.timeline.component.TimelineFilterView
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotoModificationTimePeriod
import mega.privacy.android.feature.photos.presentation.timeline.model.TimelineFilterRequest
import mega.privacy.android.feature.photos.presentation.videos.VideosTabRoute
import mega.privacy.android.feature.photos.presentation.videos.VideosTabUiState
import mega.privacy.android.feature.photos.presentation.videos.VideosTabViewModel
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.queue.snackbar.rememberSnackBarQueue
import mega.privacy.android.navigation.contract.state.ReportSelectionMode
import mega.privacy.android.navigation.destination.LegacySettingsCameraUploadsActivityNavKey
import mega.privacy.android.navigation.destination.MediaTimelinePhotoPreviewNavKey
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey
import mega.privacy.android.navigation.destination.VideoRecentlyWatchedNavKey
import mega.privacy.android.navigation.extensions.rememberMegaResultContract
import mega.privacy.android.shared.resources.R as sharedResR

@SuppressLint("ComposeViewModelForwarding")
@Composable
fun MediaMainRoute(
    navigationHandler: NavigationHandler,
    setNavigationItemVisibility: (Boolean) -> Unit,
    onNavigateToTimelinePhotoPreview: (key: MediaTimelinePhotoPreviewNavKey) -> Unit,
    onNavigateToCameraUploadsSettings: (key: LegacySettingsCameraUploadsActivityNavKey) -> Unit,
    onNavigateToUpgradeAccount: (key: UpgradeAccountNavKey) -> Unit,
    onNavigateToCameraUploadsProgressScreen: () -> Unit,
    albumsTabViewModel: AlbumsTabViewModel = hiltViewModel(),
    timelineViewModel: TimelineTabViewModel = hiltViewModel(),
    mediaCameraUploadViewModel: MediaCameraUploadViewModel = hiltViewModel(),
    nodeOptionsActionViewModel: NodeOptionsActionViewModel =
        hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
            creationCallback = { it.create(null) }
        ),
    videosTabViewModel: VideosTabViewModel = hiltViewModel(),
    videoPlaylistsTabViewModel: VideoPlaylistsTabViewModel = hiltViewModel(),
) {
    val albumsTabUiState by albumsTabViewModel.uiState.collectAsStateWithLifecycle()
    val timelineTabUiState by timelineViewModel.uiState.collectAsStateWithLifecycle()
    val selectedPhotosInTypedNodes by timelineViewModel.selectedPhotosInTypedNodesFlow.collectAsStateWithLifecycle()
    val timelineTabActionUiState by timelineViewModel.actionUiState.collectAsStateWithLifecycle()
    val timelineFilterUiState by timelineViewModel.filterUiState.collectAsStateWithLifecycle()
    val mediaCameraUploadUiState by mediaCameraUploadViewModel.uiState.collectAsStateWithLifecycle()
    val videosTabUiState by videosTabViewModel.uiState.collectAsStateWithLifecycle()
    val playlistsTabUiState by videoPlaylistsTabViewModel.uiState.collectAsStateWithLifecycle()
    val nodeActionUiState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()
    val timelineSelectedPhotoIds = rememberSaveable { mutableStateSetOf<Long>() }

    val selectionModeActionHandler = rememberMultiNodeActionHandler(
        viewModel = nodeOptionsActionViewModel,
        navigationHandler = navigationHandler
    )
    val selectionModeType by remember(
        timelineSelectedPhotoIds,
        albumsTabUiState.selectedUserAlbums,
        videosTabUiState,
        playlistsTabUiState
    ) {
        derivedStateOf {
            getSelectionModeType(
                timelineSelectedPhotoCount = timelineSelectedPhotoIds.size,
                albumsSelectedUserAlbumsCount = albumsTabUiState.selectedUserAlbums.size,
                videosTabUiState = videosTabUiState,
                playlistsTabUiState = playlistsTabUiState
            )
        }
    }
    val snackBarEventQueue = rememberSnackBarQueue()
    val megaResultContract = rememberMegaResultContract()
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    var addToPlaylistIsRetry by rememberSaveable { mutableStateOf(false) }
    var addedVideoHandle by rememberSaveable { mutableStateOf<Long?>(null) }

    val videoToPlaylistLauncher = rememberLauncherForActivityResult(
        contract = megaResultContract.videoToPlaylistActivityContract
    ) { result ->
        scope.launch {
            if (result == null) return@launch
            addedVideoHandle = result.videoHandle
            if (result.isRetry) {
                val attributes = SnackbarAttributes(
                    message = result.message,
                    action = resources.getString(sharedResR.string.transfers_retry_failed_snackbar_action),
                    actionClick = {
                        addToPlaylistIsRetry = true
                    }
                )
                snackBarEventQueue.queueMessage(attributes)
            } else {
                snackBarEventQueue.queueMessage(result.message)
            }
        }
    }

    var showTimelineFilter by rememberSaveable { mutableStateOf(false) }
    val shouldShowNavigationItem by remember {
        derivedStateOf {
            selectionModeType == MediaSelectionModeType.None && !showTimelineFilter
        }
    }

    ReportSelectionMode(isInSelectionMode = selectionModeType != MediaSelectionModeType.None)

    LaunchedEffect(shouldShowNavigationItem) {
        setNavigationItemVisibility(shouldShowNavigationItem)
    }

    LaunchedEffect(addToPlaylistIsRetry) {
        if (addToPlaylistIsRetry && addedVideoHandle != null) {
            addedVideoHandle?.let {
                videoToPlaylistLauncher.launch(it)
                addToPlaylistIsRetry = false
                addedVideoHandle = null
            }
        }
    }

    LaunchedEffect(timelineSelectedPhotoIds.size) {
        if (timelineSelectedPhotoIds.isNotEmpty()) {
            nodeOptionsActionViewModel.updateSelectionModeAvailableActions(
                selectedNodes = timelineViewModel
                    .retrieveTypedNodeFromSelection(selectedIds = timelineSelectedPhotoIds)
                    .toSet(),
                nodeSourceType = NodeSourceType.TIMELINE,
            )
        }
    }

    MediaMainEffects(
        timelineTabUiState = timelineTabUiState,
        timelineFilterUiState = timelineFilterUiState,
        mediaCameraUploadUiState = mediaCameraUploadUiState,
        checkCameraUploadsPermissions = mediaCameraUploadViewModel::checkCameraUploadsPermissions,
        checkNotificationPermission = mediaCameraUploadViewModel::updateNotificationPermission,
        updateCUPageEnablementBasedOnDisplayedPhotos = mediaCameraUploadViewModel::updateCUPageEnablementBasedOnDisplayedPhotos,
        updateSortActionEnablement = timelineViewModel::updateSortActionEnablement
    )

    MediaNodeActionEffects(
        nodeActionState = nodeActionUiState,
        onDismissRequest = {
            when (selectionModeType) {
                MediaSelectionModeType.Timeline -> timelineSelectedPhotoIds.clear()
                MediaSelectionModeType.Videos -> videosTabViewModel.clearSelection()
                else -> Unit
            }
        },
        onDismissEventConsumed = nodeOptionsActionViewModel::resetDismiss,
        onActionTriggered = {
            when (selectionModeType) {
                MediaSelectionModeType.Timeline -> timelineSelectedPhotoIds.clear()
                MediaSelectionModeType.Videos -> videosTabViewModel.clearSelection()
                else -> Unit
            }
        },
        onActionTriggeredEventConsumed = nodeOptionsActionViewModel::resetActionTriggered,
        onAddVideoToPlaylistResult = { result ->
            scope.launch {
                nodeOptionsActionViewModel.dismiss()
                if (result.isRetry) {
                    val attribute = SnackbarAttributes(
                        message = result.message,
                        action = resources.getString(sharedResR.string.transfers_retry_failed_snackbar_action),
                        actionClick = {
                            videoToPlaylistLauncher.launch(result.videoHandle)
                        }
                    )
                    snackBarEventQueue.queueMessage(attribute)
                } else {
                    snackBarEventQueue.queueMessage(result.message)
                }
            }
        },
        onResetAddVideoToPlaylistResultEventConsumed = nodeOptionsActionViewModel::resetAddVideoToPlaylistResultEvent
    )

    MediaSelectionModelHandler(
        type = selectionModeType,
        onClearTimelinePhotosSelection = timelineSelectedPhotoIds::clear,
        onClearAlbumsSelection = albumsTabViewModel::clearAlbumsSelection,
        onClearVideosSelection = videosTabViewModel::clearSelection,
        onClearPlaylistsSelection = videoPlaylistsTabViewModel::clearSelection,
    )

    MediaMainScreen(
        albumsTabUiState = albumsTabUiState,
        timelineTabUiState = timelineTabUiState,
        timelineTabActionUiState = timelineTabActionUiState,
        mediaCameraUploadUiState = mediaCameraUploadUiState,
        timelineFilterUiState = timelineFilterUiState,
        videosTabUiState = videosTabUiState,
        playlistsTabUiState = playlistsTabUiState,
        nodeActionUiState = nodeActionUiState,
        selectedPhotoIds = timelineSelectedPhotoIds,
        selectionModeType = selectionModeType,
        selectedTimePeriod = timelineViewModel.selectedTimePeriod,
        showTimelineFilter = showTimelineFilter,
        selectedPhotosInTypedNode = { selectedPhotosInTypedNodes },
        setEnableCUPage = { shouldShow ->
            mediaCameraUploadViewModel.shouldEnableCUPage(
                mediaSource = timelineFilterUiState.mediaSource,
                show = shouldShow
            )
            timelineViewModel.updateSortActionBasedOnCUPageEnablement(
                isEnableCameraUploadPageShowing = mediaCameraUploadUiState.enableCameraUploadPageShowing,
                mediaSource = timelineFilterUiState.mediaSource,
                isCUPageEnabled = shouldShow
            )
        },
        onTimelineGridSizeChange = { size ->
            timelineViewModel.onGridSizeChange(
                size = size,
                isEnableCameraUploadPageShowing = mediaCameraUploadUiState.enableCameraUploadPageShowing,
                mediaSource = timelineFilterUiState.mediaSource
            )
        },
        onTimelineSortOptionChange = timelineViewModel::onSortOptionsChange,
        onTimelineApplyFilterClick = timelineViewModel::onFilterChange,
        onTimelinePhotoSelected = {
            if (it.photo.id in timelineSelectedPhotoIds) {
                timelineSelectedPhotoIds.remove(it.photo.id)
            } else {
                timelineSelectedPhotoIds.add(it.photo.id)
            }
        },
        onClearTimelinePhotosSelection = { timelineSelectedPhotoIds.clear() },
        onNavigateToTimelinePhotoPreview = onNavigateToTimelinePhotoPreview,
        clearCameraUploadsCompletedMessage = mediaCameraUploadViewModel::onConsumeUploadCompleteEvent,
        onNavigateToCameraUploadsSettings = onNavigateToCameraUploadsSettings,
        multiNodeActionHandler = selectionModeActionHandler,
        navigateToMediaSearch = navigationHandler::navigate,
        navigationHandler = navigationHandler,
        handleCameraUploadsPermissionsResult = mediaCameraUploadViewModel::handleCameraUploadsPermissionsResult,
        handleNotificationPermissionResult = mediaCameraUploadViewModel::updateNotificationPermission,
        onCUBannerDismissRequest = mediaCameraUploadViewModel::dismissCUBanner,
        onNavigateToUpgradeAccount = onNavigateToUpgradeAccount,
        onPhotoTimePeriodSelected = timelineViewModel::onPhotoTimePeriodSelected,
        onNavigateToCameraUploadsProgressScreen = onNavigateToCameraUploadsProgressScreen,
        onUpdateVideosSearchQuery = videosTabViewModel::searchQuery,
        onUpdatePlaylistSearchQuery = videoPlaylistsTabViewModel::searchQuery,
        onCurrentVideosSearchQueryRequest = videosTabViewModel::getCurrentSearchQuery,
        updateSelectionModeAvailableActions = nodeOptionsActionViewModel::updateSelectionModeAvailableActions,
        onSelectAllVideos = videosTabViewModel::selectAllVideos,
        onClearVideosSelection = videosTabViewModel::clearSelection,
        onSelectAllPlaylists = videoPlaylistsTabViewModel::selectAllVideos,
        onClearPlaylistsSelection = videoPlaylistsTabViewModel::clearSelection,
        onTimelineFilterVisibilityChange = { showTimelineFilter = it }
    )
}

@SuppressLint("ComposeModifierMissing")
@Composable
fun MediaMainScreen(
    albumsTabUiState: AlbumsTabUiState,
    timelineTabUiState: TimelineTabUiState,
    timelineTabActionUiState: TimelineTabActionUiState,
    mediaCameraUploadUiState: MediaCameraUploadUiState,
    videosTabUiState: VideosTabUiState,
    playlistsTabUiState: VideoPlaylistsTabUiState,
    nodeActionUiState: NodeActionState,
    selectedPhotoIds: Set<Long>,
    selectionModeType: MediaSelectionModeType,
    selectedPhotosInTypedNode: () -> List<TypedNode>,
    selectedTimePeriod: PhotoModificationTimePeriod,
    multiNodeActionHandler: MultiNodeActionHandler,
    navigationHandler: NavigationHandler,
    timelineFilterUiState: TimelineFilterUiState,
    showTimelineFilter: Boolean,
    setEnableCUPage: (Boolean) -> Unit,
    onTimelineGridSizeChange: (value: TimelineGridSize) -> Unit,
    onTimelineSortOptionChange: (value: TimelineTabSortOptions) -> Unit,
    onTimelineApplyFilterClick: (request: TimelineFilterRequest) -> Unit,
    navigateToMediaSearch: (NavKey) -> Unit,
    onTimelinePhotoSelected: (nodes: PhotoNodeUiState) -> Unit,
    onClearTimelinePhotosSelection: () -> Unit,
    onNavigateToTimelinePhotoPreview: (key: MediaTimelinePhotoPreviewNavKey) -> Unit,
    clearCameraUploadsCompletedMessage: () -> Unit,
    onNavigateToCameraUploadsSettings: (key: LegacySettingsCameraUploadsActivityNavKey) -> Unit,
    handleCameraUploadsPermissionsResult: () -> Unit,
    handleNotificationPermissionResult: () -> Unit,
    onCUBannerDismissRequest: (status: CUStatusUiState) -> Unit,
    onNavigateToUpgradeAccount: (key: UpgradeAccountNavKey) -> Unit,
    onPhotoTimePeriodSelected: (PhotoModificationTimePeriod) -> Unit,
    onNavigateToCameraUploadsProgressScreen: () -> Unit,
    onUpdateVideosSearchQuery: (value: String?) -> Unit,
    onUpdatePlaylistSearchQuery: (value: String?) -> Unit,
    onCurrentVideosSearchQueryRequest: () -> String,
    updateSelectionModeAvailableActions: (selectedNodes: Set<TypedNode>, nodeSourceType: NodeSourceType) -> Unit,
    onSelectAllVideos: () -> Unit,
    onClearVideosSelection: () -> Unit,
    onSelectAllPlaylists: () -> Unit,
    onClearPlaylistsSelection: () -> Unit,
    onTimelineFilterVisibilityChange: (shouldShow: Boolean) -> Unit,
    viewModel: MediaMainViewModel = hiltViewModel(),
    albumsTabViewModel: AlbumsTabViewModel = hiltViewModel(),
    videoPlaylistsTabViewModel: VideoPlaylistsTabViewModel = hiltViewModel(),
) {
    val mediaMainUiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showTimelineSortDialog by rememberSaveable { mutableStateOf(false) }

    var videosTabQuery by rememberSaveable { mutableStateOf<String?>(null) }

    var selectedVideoCount by rememberSaveable { mutableIntStateOf(0) }
    var selectedVideoNodes by remember { mutableStateOf(emptyList<TypedNode>()) }
    var shouldHideTabs by remember { mutableStateOf(false) }

    var selectedPlaylistCount by rememberSaveable { mutableIntStateOf(0) }
    var playlistsTabQuery by rememberSaveable { mutableStateOf<String?>(null) }

    var showVideoPlaylistRemovedDialog by rememberSaveable { mutableStateOf(false) }

    // Handling back handler for timeline filter
    BackHandler(enabled = showTimelineFilter) {
        if (showTimelineFilter) {
            onTimelineFilterVisibilityChange(false)
        }
    }

    LaunchedEffect(videosTabUiState) {
        if (videosTabUiState is VideosTabUiState.Data) {
            onCurrentVideosSearchQueryRequest().let {
                if (it != videosTabQuery) {
                    videosTabQuery = it
                }
            }
            if (selectedVideoCount != videosTabUiState.selectedTypedNodes.size) {
                selectedVideoNodes = videosTabUiState.selectedTypedNodes
                selectedVideoCount = videosTabUiState.selectedTypedNodes.size
                updateSelectionModeAvailableActions(
                    videosTabUiState.selectedTypedNodes.toSet(),
                    NodeSourceType.CLOUD_DRIVE
                )
            }
        }
    }

    LaunchedEffect(playlistsTabUiState) {
        if (playlistsTabUiState is VideoPlaylistsTabUiState.Data) {
            if (playlistsTabQuery != playlistsTabUiState.query) {
                playlistsTabQuery = playlistsTabUiState.query
            }
            if (selectedPlaylistCount != playlistsTabUiState.selectedPlaylists.size) {
                selectedPlaylistCount = playlistsTabUiState.selectedPlaylists.size
            }
        }
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            AddContentFab(
                modifier = Modifier.testTag(MEDIA_ALBUMS_FAB_TAG),
                visible = (currentTabIndex == MediaScreen.Albums.ordinal || currentTabIndex == MediaScreen.Playlists.ordinal)
                        && selectionModeType == MediaSelectionModeType.None,
                onClick = {
                    if (currentTabIndex == MediaScreen.Albums.ordinal) {
                        viewModel.showNewAlbumDialog()
                    } else {
                        videoPlaylistsTabViewModel.showCreateVideoPlaylistDialog()
                    }
                }
            )
        },
        topBar = {
            MediaTopBar(
                currentTabIndex = currentTabIndex,
                selectionModeType = selectionModeType,
                albumsTabUiState = albumsTabUiState,
                timelineTabActionUiState = timelineTabActionUiState,
                timelineFilterUiState = timelineFilterUiState,
                mediaCameraUploadUiState = mediaCameraUploadUiState,
                videosTabUiState = videosTabUiState,
                playlistsTabUiState = playlistsTabUiState,
                timelineItemCount = timelineTabUiState.displayedPhotos.size,
                timelineSelectedCount = selectedPhotoIds.size,
                selectedTimePeriod = selectedTimePeriod,
                videosTabQuery = videosTabQuery,
                playlistsTabQuery = playlistsTabQuery,
                onClearTimelinePhotosSelection = onClearTimelinePhotosSelection,
                onClearAlbumsSelection = albumsTabViewModel::clearAlbumsSelection,
                onAllVideosSelected = onSelectAllVideos,
                onClearVideosSelection = onClearVideosSelection,
                onUpdateVideosSearchQuery = onUpdateVideosSearchQuery,
                onAllPlaylistsSelected = onSelectAllPlaylists,
                onClearPlaylistsSelection = onClearPlaylistsSelection,
                onUpdatePlaylistSearchQuery = onUpdatePlaylistSearchQuery,
                removePlaylist = { showVideoPlaylistRemovedDialog = true },
                onNavigateToCameraUploadsSettings = {
                    MediaAppBarAction.CameraUploadsSettings.toTrackingEvent()
                        ?.let { Analytics.tracker.trackEvent(it) }
                    onNavigateToCameraUploadsSettings(
                        LegacySettingsCameraUploadsActivityNavKey()
                    )
                },
                onNavigateToCameraUploadsProgressScreen = onNavigateToCameraUploadsProgressScreen,
                navigateToMediaSearch = { key ->
                    (MediaAppBarAction.Search as MediaAppBarAction).toTrackingEvent()
                        ?.let { Analytics.tracker.trackEvent(it) }
                    navigateToMediaSearch(key)
                },
                onFilterActionClick = {
                    MediaAppBarAction.FilterSecondary.toTrackingEvent()
                        ?.let { Analytics.tracker.trackEvent(it) }
                    onTimelineFilterVisibilityChange(true)
                },
                onSortActionClick = {
                    MediaAppBarAction.SortBy.toTrackingEvent()
                        ?.let { Analytics.tracker.trackEvent(it) }
                    showTimelineSortDialog = true
                },
                navigateToRecentlyWatched = {
                    navigationHandler.navigate(VideoRecentlyWatchedNavKey)
                }
            )
        },
        bottomBar = {
            MediaBottomBar(
                selectionModeType = selectionModeType,
                nodeActionUiState = nodeActionUiState,
                albumsActions = listOf(
                    AlbumSelectionAction.ManageLink,
                    AlbumSelectionAction.Delete
                ),
                selectedNodes = when (selectionModeType) {
                    MediaSelectionModeType.Timeline -> selectedPhotosInTypedNode()
                    MediaSelectionModeType.Videos -> selectedVideoNodes
                    else -> emptyList()
                },
                multiNodeActionHandler = multiNodeActionHandler,
                onActionPressed = { mode, action ->
                    when (mode) {
                        MediaSelectionModeType.Timeline -> {
                            action.toTrackingEvent()?.let { event ->
                                Analytics.tracker.trackEvent(event)
                            }
                        }

                        MediaSelectionModeType.Albums -> {
                            albumsTabViewModel.handleSelectionAction(action)
                        }

                        else -> Unit
                    }
                }
            )
        },
    ) { paddingValues ->
        val tabEntries = remember(mediaMainUiState.isMediaRevampPhase2Enabled) {
            if (mediaMainUiState.isMediaRevampPhase2Enabled) {
                MediaScreen.entries
            } else {
                MediaScreen.entries.filter {
                    it == MediaScreen.Timeline || it == MediaScreen.Albums
                }
            }
        }

        key(tabEntries.size) {
            MegaScrollableTabRow(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding()),
                beyondViewportPageCount = 1,
                hideTabs = selectionModeType.isAnActiveSelection() || shouldHideTabs,
                pagerScrollEnabled = selectionModeType == MediaSelectionModeType.None,
                initialSelectedIndex = currentTabIndex,
                onTabSelected = { index ->
                    currentTabIndex = index
                    tabEntries.getOrNull(index)?.let { selectedTab ->
                        Analytics.tracker.trackEvent(selectedTab.analyticsInfo)
                    }
                    true
                },
                cells = {
                    tabEntries.forEach { tab ->
                        with(tab) {
                            addTextTabWithScrollableContent(
                                tabItem = getTabItem(),
                                content = { _, modifier ->
                                    MediaContent(
                                        modifier = modifier,
                                        timelineContentPadding = paddingValues,
                                        mainViewModel = viewModel,
                                        albumsTabViewModel = albumsTabViewModel,
                                        timelineTabUiState = timelineTabUiState,
                                        timelineFilterUiState = timelineFilterUiState,
                                        mediaCameraUploadUiState = mediaCameraUploadUiState,
                                        selectedPhotoIds = selectedPhotoIds,
                                        showTimelineSortDialog = showTimelineSortDialog,
                                        selectedTimePeriod = selectedTimePeriod,
                                        setEnableCUPage = setEnableCUPage,
                                        onTimelineGridSizeChange = onTimelineGridSizeChange,
                                        onTimelineSortDialogDismissed = {
                                            showTimelineSortDialog = false
                                        },
                                        onTimelineSortOptionChange = {
                                            onTimelineSortOptionChange(it)
                                            showTimelineSortDialog = false
                                        },
                                        onTimelinePhotoClick = {
                                            if (selectionModeType == MediaSelectionModeType.Timeline) {
                                                onTimelinePhotoSelected(it)
                                            } else {
                                                onNavigateToTimelinePhotoPreview(
                                                    MediaTimelinePhotoPreviewNavKey(
                                                        id = it.photo.id,
                                                        sortType = timelineTabUiState.currentSort.toLegacySort().name,
                                                        filterType = timelineFilterUiState.mediaType.name,
                                                        mediaSource = timelineFilterUiState.mediaSource.toLegacyPhotosSource().name
                                                    )
                                                )
                                            }
                                        },
                                        onTimelinePhotoSelected = onTimelinePhotoSelected,
                                        clearCameraUploadsCompletedMessage = clearCameraUploadsCompletedMessage,
                                        onNavigateToCameraUploadsSettings = onNavigateToCameraUploadsSettings,
                                        navigationHandler = navigationHandler,
                                        handleCameraUploadsPermissionsResult = handleCameraUploadsPermissionsResult,
                                        handleNotificationPermissionResult = handleNotificationPermissionResult,
                                        onCUBannerDismissRequest = onCUBannerDismissRequest,
                                        onTabsVisibilityChange = { shouldHideTabs = it },
                                        onNavigateToUpgradeAccount = onNavigateToUpgradeAccount,
                                        onPhotoTimePeriodSelected = onPhotoTimePeriodSelected,
                                        showVideoPlaylistRemovedDialog = showVideoPlaylistRemovedDialog,
                                        dismissVideoPlaylistRemovedDialog = {
                                            showVideoPlaylistRemovedDialog = false
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            )
        }
    }

    AnimatedVisibility(
        visible = showTimelineFilter,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        TimelineFilterView(
            modifier = Modifier.fillMaxSize(),
            currentFilter = timelineFilterUiState,
            onApplyFilterClick = { request ->
                onTimelineApplyFilterClick(request)
                onTimelineFilterVisibilityChange(false)
            },
            onClose = {
                onTimelineFilterVisibilityChange(false)
            },
        )
    }
}

@Composable
private fun MediaScreen.getTabItem() = when (this) {
    MediaScreen.Timeline -> TabItems(title = stringResource(sharedResR.string.media_timeline_tab_title))
    MediaScreen.Albums -> TabItems(title = stringResource(sharedResR.string.media_albums_tab_title))
    MediaScreen.Videos -> TabItems(title = stringResource(sharedResR.string.media_videos_tab_title))
    MediaScreen.Playlists -> TabItems(title = stringResource(sharedResR.string.media_playlists_tab_title))
}

@Composable
private fun MediaScreen.MediaContent(
    mainViewModel: MediaMainViewModel,
    albumsTabViewModel: AlbumsTabViewModel,
    timelineTabUiState: TimelineTabUiState,
    mediaCameraUploadUiState: MediaCameraUploadUiState,
    timelineFilterUiState: TimelineFilterUiState,
    selectedPhotoIds: Set<Long>,
    showTimelineSortDialog: Boolean,
    selectedTimePeriod: PhotoModificationTimePeriod,
    setEnableCUPage: (Boolean) -> Unit,
    onTimelineGridSizeChange: (value: TimelineGridSize) -> Unit,
    onTimelineSortDialogDismissed: () -> Unit,
    onTimelineSortOptionChange: (value: TimelineTabSortOptions) -> Unit,
    onTimelinePhotoClick: (node: PhotoNodeUiState) -> Unit,
    onTimelinePhotoSelected: (node: PhotoNodeUiState) -> Unit,
    clearCameraUploadsCompletedMessage: () -> Unit,
    onNavigateToCameraUploadsSettings: (key: LegacySettingsCameraUploadsActivityNavKey) -> Unit,
    navigationHandler: NavigationHandler,
    handleCameraUploadsPermissionsResult: () -> Unit,
    handleNotificationPermissionResult: () -> Unit,
    onCUBannerDismissRequest: (status: CUStatusUiState) -> Unit,
    onTabsVisibilityChange: (shouldHide: Boolean) -> Unit,
    onNavigateToUpgradeAccount: (key: UpgradeAccountNavKey) -> Unit,
    onPhotoTimePeriodSelected: (PhotoModificationTimePeriod) -> Unit,
    showVideoPlaylistRemovedDialog: Boolean,
    dismissVideoPlaylistRemovedDialog: () -> Unit,
    modifier: Modifier = Modifier,
    timelineContentPadding: PaddingValues = PaddingValues(),
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

    when (this) {
        MediaScreen.Timeline -> {
            TimelineTabRoute(
                modifier = Modifier.fillMaxSize(),
                contentPadding = timelineContentPadding,
                uiState = timelineTabUiState,
                mediaCameraUploadUiState = mediaCameraUploadUiState,
                timelineFilterUiState = timelineFilterUiState,
                selectedPhotoIds = selectedPhotoIds,
                showTimelineSortDialog = showTimelineSortDialog,
                selectedTimePeriod = selectedTimePeriod,
                clearCameraUploadsCompletedMessage = clearCameraUploadsCompletedMessage,
                onNavigateToCameraUploadsSettings = onNavigateToCameraUploadsSettings,
                setEnableCUPage = setEnableCUPage,
                onGridSizeChange = onTimelineGridSizeChange,
                onSortDialogDismissed = onTimelineSortDialogDismissed,
                onSortOptionChange = onTimelineSortOptionChange,
                onPhotoClick = onTimelinePhotoClick,
                onPhotoSelected = onTimelinePhotoSelected,
                handleCameraUploadsPermissionsResult = handleCameraUploadsPermissionsResult,
                handleNotificationPermissionResult = handleNotificationPermissionResult,
                onCUBannerDismissRequest = onCUBannerDismissRequest,
                onTabsVisibilityChange = onTabsVisibilityChange,
                onNavigateToUpgradeAccount = onNavigateToUpgradeAccount,
                onPhotoTimePeriodSelected = onPhotoTimePeriodSelected
            )
        }

        MediaScreen.Albums -> {
            AlbumsTabRoute(
                modifier = Modifier.fillMaxSize(),
                showNewAlbumDialogEvent = uiState.newAlbumDialogEvent,
                resetNewAlbumDialogEvent = mainViewModel::resetNewAlbumDialog,
                onNavigate = navigationHandler::navigate,
                viewModel = albumsTabViewModel,
                contentPadding = timelineContentPadding.excludeTopPadding()
            )
        }

        MediaScreen.Videos -> VideosTabRoute(navigationHandler)
        MediaScreen.Playlists -> VideoPlaylistsTabRoute(
            showVideoPlaylistRemovedDialog = showVideoPlaylistRemovedDialog,
            dismissVideoPlaylistRemovedDialog = dismissVideoPlaylistRemovedDialog,
            modifier = modifier,
            navigate = navigationHandler::navigate
        )
    }
}

private fun getSelectionModeType(
    timelineSelectedPhotoCount: Int,
    albumsSelectedUserAlbumsCount: Int,
    videosTabUiState: VideosTabUiState,
    playlistsTabUiState: VideoPlaylistsTabUiState,
): MediaSelectionModeType = when {
    timelineSelectedPhotoCount > 0 -> MediaSelectionModeType.Timeline
    albumsSelectedUserAlbumsCount > 0 -> MediaSelectionModeType.Albums
    videosTabUiState is VideosTabUiState.Data && videosTabUiState.selectedTypedNodes.isNotEmpty() -> MediaSelectionModeType.Videos
    playlistsTabUiState is VideoPlaylistsTabUiState.Data && playlistsTabUiState.selectedPlaylists.isNotEmpty() -> MediaSelectionModeType.Playlists
    else -> MediaSelectionModeType.None
}

@CombinedThemePreviews
@Composable
private fun PhotosMainScreenPreview() {
    AndroidThemeForPreviews {
        MediaMainScreen(
            timelineTabUiState = TimelineTabUiState(),
            timelineTabActionUiState = TimelineTabActionUiState(),
            timelineFilterUiState = TimelineFilterUiState(),
            mediaCameraUploadUiState = MediaCameraUploadUiState(),
            selectedPhotoIds = setOf(),
            selectedTimePeriod = PhotoModificationTimePeriod.All,
            showTimelineFilter = false,
            selectedPhotosInTypedNode = { emptyList() },
            setEnableCUPage = {},
            onTimelineGridSizeChange = {},
            onTimelineSortOptionChange = {},
            onTimelineApplyFilterClick = {},
            onTimelinePhotoSelected = {},
            onClearTimelinePhotosSelection = {},
            onNavigateToTimelinePhotoPreview = {},
            clearCameraUploadsCompletedMessage = {},
            onNavigateToCameraUploadsSettings = {},
            multiNodeActionHandler = rememberMultiNodeActionHandler(),
            navigateToMediaSearch = {},
            navigationHandler = object : NavigationHandler {
                override fun back() {}
                override fun remove(navKey: NavKey) {}
                override fun navigate(destination: NavKey, navOptions: NavOptions?) {}
                override fun navigate(destinations: List<NavKey>, navOptions: NavOptions?) {}
                override fun backTo(destination: NavKey, inclusive: Boolean) {}
                override fun navigateAndClearBackStack(destination: NavKey) {}
                override fun navigateAndClearTo(
                    destination: NavKey,
                    newParent: NavKey,
                    inclusive: Boolean,
                ) {
                }

                override fun navigateAndClearTo(
                    destination: List<NavKey>,
                    newParent: NavKey,
                    inclusive: Boolean,
                ) {
                }

                override fun <T> returnResult(key: String, value: T) {}
                override fun clearResult(key: String) {}
                override fun <T> monitorResult(key: String): Flow<T?> = flowOf(null)
                override fun clearAllResults() {}
            },
            handleCameraUploadsPermissionsResult = {},
            handleNotificationPermissionResult = {},
            onCUBannerDismissRequest = {},
            onNavigateToUpgradeAccount = {},
            onPhotoTimePeriodSelected = {},
            onNavigateToCameraUploadsProgressScreen = {},
            albumsTabUiState = AlbumsTabUiState(),
            videosTabUiState = VideosTabUiState.Data(),
            playlistsTabUiState = VideoPlaylistsTabUiState.Data(),
            nodeActionUiState = NodeActionState(),
            selectionModeType = MediaSelectionModeType.None,
            onUpdateVideosSearchQuery = {},
            onUpdatePlaylistSearchQuery = {},
            onCurrentVideosSearchQueryRequest = { "" },
            updateSelectionModeAvailableActions = { _, _ -> },
            onSelectAllVideos = {},
            onClearVideosSelection = {},
            onSelectAllPlaylists = {},
            onClearPlaylistsSelection = {},
            onTimelineFilterVisibilityChange = {}
        )
    }
}

internal const val MEDIA_ALBUMS_FAB_TAG = "media_screen_albums_tab:add_content_fab"