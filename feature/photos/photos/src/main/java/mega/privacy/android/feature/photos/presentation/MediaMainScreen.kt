@file:OptIn(ExperimentalMaterial3Api::class)

package mega.privacy.android.feature.photos.presentation

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.tabs.MegaCollapsibleTabRow
import mega.android.core.ui.model.SnackbarAttributes
import mega.android.core.ui.model.TabItems
import mega.android.core.ui.modifiers.applyScrollToHideFabBehavior
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
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.FilterMediaSource.Companion.toLegacyPhotosSource
import mega.privacy.android.feature.photos.model.MediaAppBarAction
import mega.privacy.android.feature.photos.model.MediaScreen
import mega.privacy.android.feature.photos.model.PhotosNodeContentItemV2
import mega.privacy.android.feature.photos.model.TimelineGridSize
import mega.privacy.android.feature.photos.presentation.albums.AlbumsTabRoute
import mega.privacy.android.feature.photos.presentation.albums.AlbumsTabUiState
import mega.privacy.android.feature.photos.presentation.albums.AlbumsTabViewModel
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumSelectionAction
import mega.privacy.android.feature.photos.presentation.component.MediaBottomBar
import mega.privacy.android.feature.photos.presentation.component.MediaTimePeriodSelector
import mega.privacy.android.feature.photos.presentation.component.MediaTopBar
import mega.privacy.android.feature.photos.presentation.effects.MediaMainEffects
import mega.privacy.android.feature.photos.presentation.effects.MediaNodeActionEffects
import mega.privacy.android.feature.photos.presentation.handler.MediaSelectionModeType
import mega.privacy.android.feature.photos.presentation.handler.MediaSelectionModeType.Companion.isAnActiveSelection
import mega.privacy.android.feature.photos.presentation.handler.MediaSelectionModelHandler
import mega.privacy.android.feature.photos.presentation.playlists.VideoPlaylistsTabRoute
import mega.privacy.android.feature.photos.presentation.playlists.VideoPlaylistsTabUiState
import mega.privacy.android.feature.photos.presentation.playlists.VideoPlaylistsTabViewModel
import mega.privacy.android.feature.photos.presentation.playlists.view.VideoPlaylistsTrashMenuAction
import mega.privacy.android.feature.photos.presentation.timeline.TimelineFilterUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabActionUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabRoute
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabSortOptions
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabSortOptions.Companion.toLegacySort
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabViewModel
import mega.privacy.android.feature.photos.presentation.timeline.component.TimelineFilterView
import mega.privacy.android.feature.photos.presentation.timeline.component.TimelineSortDialog
import mega.privacy.android.feature.photos.presentation.timeline.model.MediaTimePeriod
import mega.privacy.android.feature.photos.presentation.timeline.model.TimelineFilterRequest
import mega.privacy.android.feature.photos.presentation.timeline.revamp.TimelineRevampScreen
import mega.privacy.android.feature.photos.presentation.timeline.revamp.TimelineRevampUiState
import mega.privacy.android.feature.photos.presentation.timeline.revamp.TimelineRevampViewModel
import mega.privacy.android.feature.photos.presentation.videos.VideosTabRoute
import mega.privacy.android.feature.photos.presentation.videos.VideosTabUiState
import mega.privacy.android.feature.photos.presentation.videos.VideosTabViewModel
import mega.privacy.android.navigation.contract.NavOptions
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
    timelineRevampViewModel: TimelineRevampViewModel = hiltViewModel(),
    mediaCameraUploadViewModel: MediaCameraUploadViewModel = hiltViewModel(),
    nodeOptionsActionViewModel: NodeOptionsActionViewModel =
        hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
            creationCallback = { it.create(null) }
        ),
    videosTabViewModel: VideosTabViewModel = hiltViewModel(),
    videoPlaylistsTabViewModel: VideoPlaylistsTabViewModel = hiltViewModel(),
    mediaMainViewModel: MediaMainViewModel = hiltViewModel(),
) {
    val albumsTabUiState by albumsTabViewModel.uiState.collectAsStateWithLifecycle()
    val timelineTabUiState by timelineViewModel.uiState.collectAsStateWithLifecycle()
    val timelineRevampUiState by timelineRevampViewModel.uiState.collectAsStateWithLifecycle()
    val mediaMainUiState by mediaMainViewModel.uiState.collectAsStateWithLifecycle()
    val isTimelineRevampEnabled = mediaMainUiState.isTimelineRevampEnabled == true
    val selectedPhotosInTypedNodes by timelineViewModel.selectedPhotosInTypedNodesFlow.collectAsStateWithLifecycle()
    val timelineRevampSelectedPhotosInTypedNodes by timelineRevampViewModel.selectedPhotosInTypedNodesFlow.collectAsStateWithLifecycle()
    val timelineTabActionUiState by timelineViewModel.actionUiState.collectAsStateWithLifecycle()
    val timelineRevampActionUiState by timelineRevampViewModel.actionUiState.collectAsStateWithLifecycle()
    val timelineFilterUiState by timelineViewModel.filterUiState.collectAsStateWithLifecycle()
    val timelineRevampFilterUiState by timelineRevampViewModel.filterUiState.collectAsStateWithLifecycle()
    val timelineRevampSelectedTimePeriod by timelineRevampViewModel.selectedTimePeriod.collectAsStateWithLifecycle()
    val mediaCameraUploadUiState by mediaCameraUploadViewModel.uiState.collectAsStateWithLifecycle()
    val videosSelectionUiState by videosTabViewModel.selectionUiState.collectAsStateWithLifecycle()
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
        videosSelectionUiState,
        playlistsTabUiState
    ) {
        derivedStateOf {
            getSelectionModeType(
                timelineSelectedPhotoCount = timelineSelectedPhotoIds.size,
                albumsSelectedUserAlbumsCount = albumsTabUiState.selectedUserAlbums.size,
                videosSelectionUiState = videosSelectionUiState,
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
            val selectedNodes = if (isTimelineRevampEnabled) {
                timelineRevampViewModel.retrieveTypedNodeFromSelection(timelineSelectedPhotoIds)
            } else {
                timelineViewModel.retrieveTypedNodeFromSelection(timelineSelectedPhotoIds)
            }
            nodeOptionsActionViewModel.updateSelectionModeAvailableActions(
                selectedNodes = selectedNodes.toSet(),
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

    // The tab's MediaMainEffects above is keyed on the tab ui state, which stays loading while the
    // revamp drives the timeline. Mirror the same sort-enablement effect off the revamp's own state.
    LaunchedEffect(
        timelineRevampUiState,
        mediaCameraUploadUiState.enableCameraUploadPageShowing,
        timelineRevampFilterUiState.mediaSource,
    ) {
        if (timelineRevampUiState !is TimelineRevampUiState.Loading) {
            timelineRevampViewModel.updateSortActionEnablement(
                isEnableCameraUploadPageShowing = mediaCameraUploadUiState.enableCameraUploadPageShowing,
                mediaSource = timelineRevampFilterUiState.mediaSource,
            )
        }
    }

    LaunchedEffect(
        timelineRevampUiState,
        mediaCameraUploadUiState.status,
        timelineRevampFilterUiState.mediaSource,
    ) {
        val show = when (timelineRevampUiState) {
            is TimelineRevampUiState.Empty ->
                mediaCameraUploadUiState.status is CUStatusUiState.Disabled

            is TimelineRevampUiState.Data -> false
            else -> return@LaunchedEffect
        }
        mediaCameraUploadViewModel.shouldEnableCUPage(
            mediaSource = timelineRevampFilterUiState.mediaSource,
            show = show,
        )
    }

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
        timelineRevampUiState = timelineRevampUiState,
        onTimelineRevampVisibleRangeChanged = timelineRevampViewModel::onVisibleRangeChanged,
        onTimelineRevampGridSizeChange = timelineRevampViewModel::onGridSizeChange,
        onTimelineRevampZoomIn = timelineRevampViewModel::onZoomIn,
        onTimelineRevampZoomOut = timelineRevampViewModel::onZoomOut,
        onTimelineRevampNodeClicked = timelineRevampViewModel::onNodeClicked,
        onTimelineRevampTakenDownDialogConsumed = timelineRevampViewModel::onTakenDownDialogEventConsumed,
        timelineTabActionUiState = timelineTabActionUiState,
        timelineRevampActionUiState = timelineRevampActionUiState,
        mediaCameraUploadUiState = mediaCameraUploadUiState,
        timelineFilterUiState = timelineFilterUiState,
        videosSelectionUiState = videosSelectionUiState,
        playlistsTabUiState = playlistsTabUiState,
        nodeActionUiState = nodeActionUiState,
        selectedPhotoIds = timelineSelectedPhotoIds,
        selectionModeType = selectionModeType,
        selectedTimePeriod = timelineViewModel.selectedTimePeriod,
        showTimelineFilter = showTimelineFilter,
        selectedPhotosInTypedNode = {
            if (isTimelineRevampEnabled) timelineRevampSelectedPhotosInTypedNodes
            else selectedPhotosInTypedNodes
        },
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
            // Drive the revamp VM's sort action too; only the active VM's actionUiState is consumed.
            timelineRevampViewModel.updateSortActionBasedOnCUPageEnablement(
                isEnableCameraUploadPageShowing = mediaCameraUploadUiState.enableCameraUploadPageShowing,
                mediaSource = timelineRevampFilterUiState.mediaSource,
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
        onTimelineRevampSortOptionChange = timelineRevampViewModel::onSortOptionsChange,
        onTimelineApplyFilterClick = timelineViewModel::onFilterChange,
        timelineRevampFilterUiState = timelineRevampFilterUiState,
        onTimelineRevampApplyFilterClick = timelineRevampViewModel::onFilterChange,
        timelineRevampSelectedTimePeriod = timelineRevampSelectedTimePeriod,
        onTimelinePhotoSelected = {
            if (it in timelineSelectedPhotoIds) {
                timelineSelectedPhotoIds.remove(it)
            } else {
                timelineSelectedPhotoIds.add(it)
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
        onMediaTimePeriodSelected = timelineViewModel::onMediaTimePeriodSelected,
        onTimelineRevampMediaTimePeriodSelected = timelineRevampViewModel::onMediaTimePeriodSelected,
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
    timelineRevampUiState: TimelineRevampUiState,
    onTimelineRevampVisibleRangeChanged: (firstIndex: Int, lastIndex: Int) -> Unit,
    onTimelineRevampGridSizeChange: (value: TimelineGridSize) -> Unit,
    onTimelineRevampZoomIn: () -> Unit,
    onTimelineRevampZoomOut: () -> Unit,
    onTimelineRevampNodeClicked: (PhotosNodeContentItemV2?) -> Unit,
    onTimelineRevampTakenDownDialogConsumed: () -> Unit,
    timelineTabActionUiState: TimelineTabActionUiState,
    timelineRevampActionUiState: TimelineTabActionUiState,
    mediaCameraUploadUiState: MediaCameraUploadUiState,
    videosSelectionUiState: VideosTabUiState.Selection,
    playlistsTabUiState: VideoPlaylistsTabUiState,
    nodeActionUiState: NodeActionState,
    selectedPhotoIds: Set<Long>,
    selectionModeType: MediaSelectionModeType,
    selectedPhotosInTypedNode: () -> List<TypedNode>,
    selectedTimePeriod: MediaTimePeriod,
    multiNodeActionHandler: MultiNodeActionHandler,
    navigationHandler: NavigationHandler,
    timelineFilterUiState: TimelineFilterUiState,
    showTimelineFilter: Boolean,
    setEnableCUPage: (Boolean) -> Unit,
    onTimelineGridSizeChange: (value: TimelineGridSize) -> Unit,
    onTimelineSortOptionChange: (value: TimelineTabSortOptions) -> Unit,
    onTimelineRevampSortOptionChange: (value: TimelineTabSortOptions) -> Unit,
    onTimelineApplyFilterClick: (request: TimelineFilterRequest) -> Unit,
    timelineRevampFilterUiState: TimelineFilterUiState,
    onTimelineRevampApplyFilterClick: (request: TimelineFilterRequest) -> Unit,
    timelineRevampSelectedTimePeriod: MediaTimePeriod,
    navigateToMediaSearch: (NavKey) -> Unit,
    onTimelinePhotoSelected: (id: Long) -> Unit,
    onClearTimelinePhotosSelection: () -> Unit,
    onNavigateToTimelinePhotoPreview: (key: MediaTimelinePhotoPreviewNavKey) -> Unit,
    clearCameraUploadsCompletedMessage: () -> Unit,
    onNavigateToCameraUploadsSettings: (key: LegacySettingsCameraUploadsActivityNavKey) -> Unit,
    handleCameraUploadsPermissionsResult: () -> Unit,
    handleNotificationPermissionResult: () -> Unit,
    onCUBannerDismissRequest: (status: CUStatusUiState) -> Unit,
    onNavigateToUpgradeAccount: (key: UpgradeAccountNavKey) -> Unit,
    onMediaTimePeriodSelected: (MediaTimePeriod) -> Unit,
    onTimelineRevampMediaTimePeriodSelected: (MediaTimePeriod) -> Unit,
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

    // When the revamp is enabled the timeline is driven by TimelineRevampViewModel, so the shared
    // top bar and filter sheet read their inputs from it rather than the (inert) tab ViewModel.
    // Resolving them here keeps the revamp/tab branching in one place.
    val isTimelineRevampEnabled = mediaMainUiState.isTimelineRevampEnabled == true
    val effectiveTimelineFilterUiState =
        if (isTimelineRevampEnabled) timelineRevampFilterUiState else timelineFilterUiState
    val effectiveTimelineActionUiState =
        if (isTimelineRevampEnabled) timelineRevampActionUiState else timelineTabActionUiState
    val effectiveTimelineItemCount =
        if (isTimelineRevampEnabled) {
            timelineRevampUiState.mediaItemCount()
        } else {
            timelineTabUiState.displayedPhotos.size
        }
    val effectiveSelectedTimePeriod =
        if (isTimelineRevampEnabled) timelineRevampSelectedTimePeriod else selectedTimePeriod
    val onApplyTimelineFilter =
        if (isTimelineRevampEnabled) onTimelineRevampApplyFilterClick else onTimelineApplyFilterClick
    val effectiveOnSortOptionChange =
        if (isTimelineRevampEnabled) onTimelineRevampSortOptionChange else onTimelineSortOptionChange
    val effectiveOnMediaTimePeriodSelected =
        if (isTimelineRevampEnabled) onTimelineRevampMediaTimePeriodSelected else onMediaTimePeriodSelected
    val effectiveCurrentSort =
        if (isTimelineRevampEnabled) {
            (timelineRevampUiState as? TimelineRevampUiState.Data)?.currentSort
                ?: TimelineTabSortOptions.Newest
        } else {
            timelineTabUiState.currentSort
        }
    val showEnableCameraUploadsPageForRevamp = isTimelineRevampEnabled &&
            mediaCameraUploadUiState.enableCameraUploadPageShowing &&
            timelineRevampFilterUiState.mediaSource != FilterMediaSource.CloudDrive

    var currentTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showTimelineSortDialog by rememberSaveable { mutableStateOf(false) }

    var videosTabQuery by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedVideoNodes by remember { mutableStateOf(emptyList<TypedNode>()) }

    var selectedPlaylistCount by rememberSaveable { mutableIntStateOf(0) }
    var playlistsTabQuery by rememberSaveable { mutableStateOf<String?>(null) }

    var showVideoPlaylistRemovedDialog by rememberSaveable { mutableStateOf(false) }

    var isSearchModeForVideosOrPlaylists by rememberSaveable { mutableStateOf(false) }

    // Handling back handler for timeline filter
    BackHandler(enabled = showTimelineFilter) {
        if (showTimelineFilter) {
            onTimelineFilterVisibilityChange(false)
        }
    }

    LaunchedEffect(currentTabIndex) {
        if (currentTabIndex == MediaScreen.Albums.ordinal) albumsTabViewModel.initialize()
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
                modifier = Modifier
                    .applyScrollToHideFabBehavior()
                    .testTag(MEDIA_ALBUMS_FAB_TAG),
                visible = (currentTabIndex == MediaScreen.Albums.ordinal || currentTabIndex == MediaScreen.Playlists.ordinal)
                        && selectionModeType == MediaSelectionModeType.None
                        // Keep the create-album FAB hidden until albums finish loading, otherwise the
                        // auto-suggested album name is derived from a partial list and can collide
                        // with an existing album ("already exists"). See AND-23286.
                        && !(currentTabIndex == MediaScreen.Albums.ordinal && albumsTabUiState.isLoading),
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
                timelineTabActionUiState = effectiveTimelineActionUiState,
                timelineFilterUiState = effectiveTimelineFilterUiState,
                mediaCameraUploadUiState = mediaCameraUploadUiState,
                videosSelectionUiState = videosSelectionUiState,
                playlistsTabUiState = playlistsTabUiState,
                timelineItemCount = effectiveTimelineItemCount,
                timelineSelectedCount = selectedPhotoIds.size,
                selectedTimePeriod = effectiveSelectedTimePeriod,
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
                },
                onSearchingModeChanged = {
                    isSearchModeForVideosOrPlaylists = it
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
                playlistsActions = listOf(
                    VideoPlaylistsTrashMenuAction()
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

                        MediaSelectionModeType.Playlists -> {
                            if (action is VideoPlaylistsTrashMenuAction) {
                                showVideoPlaylistRemovedDialog = true
                            }
                        }

                        else -> Unit
                    }
                }
            )
        },
    ) { paddingValues ->
        val tabEntries = MediaScreen.entries

        MegaCollapsibleTabRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            beyondViewportPageCount = 1,
            hideTabs =
                selectionModeType.isAnActiveSelection() || isSearchModeForVideosOrPlaylists,
            pagerScrollEnabled =
                selectionModeType == MediaSelectionModeType.None && !isSearchModeForVideosOrPlaylists,
            initialSelectedIndex = currentTabIndex.coerceAtMost(tabEntries.lastIndex),
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
                                    timelineRevampUiState = timelineRevampUiState,
                                    onTimelineRevampVisibleRangeChanged = onTimelineRevampVisibleRangeChanged,
                                    onTimelineRevampGridSizeChange = onTimelineRevampGridSizeChange,
                                    onTimelineRevampZoomIn = onTimelineRevampZoomIn,
                                    onTimelineRevampZoomOut = onTimelineRevampZoomOut,
                                    onTimelineRevampNodeClicked = onTimelineRevampNodeClicked,
                                    onTimelineRevampTakenDownDialogConsumed = onTimelineRevampTakenDownDialogConsumed,
                                    showEnableCameraUploadsPage = showEnableCameraUploadsPageForRevamp,
                                    timelineFilterUiState = timelineFilterUiState,
                                    mediaCameraUploadUiState = mediaCameraUploadUiState,
                                    videosSelectionUiState = videosSelectionUiState,
                                    selectedPhotoIds = selectedPhotoIds,
                                    showTimelineSortDialog = showTimelineSortDialog,
                                    selectedTimePeriod = effectiveSelectedTimePeriod,
                                    setEnableCUPage = setEnableCUPage,
                                    onTimelineGridSizeChange = onTimelineGridSizeChange,
                                    onTimelineSortDialogDismissed = {
                                        showTimelineSortDialog = false
                                    },
                                    onTimelineSortOptionChange = {
                                        effectiveOnSortOptionChange(it)
                                        showTimelineSortDialog = false
                                    },
                                    onTimelinePhotoClick = {
                                        if (selectionModeType == MediaSelectionModeType.Timeline) {
                                            onTimelinePhotoSelected(it)
                                        } else {
                                            onNavigateToTimelinePhotoPreview(
                                                MediaTimelinePhotoPreviewNavKey(
                                                    id = it,
                                                    sortType = effectiveCurrentSort.toLegacySort().name,
                                                    filterType = effectiveTimelineFilterUiState.mediaType.name,
                                                    mediaSource = effectiveTimelineFilterUiState.mediaSource.toLegacyPhotosSource().name
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
                                    onNavigateToUpgradeAccount = onNavigateToUpgradeAccount,
                                    onMediaTimePeriodSelected = effectiveOnMediaTimePeriodSelected,
                                    showVideoPlaylistRemovedDialog = showVideoPlaylistRemovedDialog,
                                    dismissVideoPlaylistRemovedDialog = {
                                        showVideoPlaylistRemovedDialog = false
                                    },
                                    onCurrentVideosSearchQueryRequest = {
                                        onCurrentVideosSearchQueryRequest().let {
                                            if (it != videosTabQuery) {
                                                videosTabQuery = it
                                            }
                                        }
                                    },
                                    updateSelectionModeAvailableActions = { selectedNodes, nodeSourceType ->
                                        selectedVideoNodes = selectedNodes
                                        updateSelectionModeAvailableActions(
                                            selectedNodes.toSet(),
                                            NodeSourceType.CLOUD_DRIVE
                                        )
                                    }
                                )
                            }
                        )
                    }
                }
            }
        )
    }

    AnimatedVisibility(
        visible = showTimelineFilter,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        TimelineFilterView(
            modifier = Modifier.fillMaxSize(),
            currentFilter = effectiveTimelineFilterUiState,
            onApplyFilterClick = { request ->
                onApplyTimelineFilter(request)
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
    timelineRevampUiState: TimelineRevampUiState,
    onTimelineRevampVisibleRangeChanged: (firstIndex: Int, lastIndex: Int) -> Unit,
    onTimelineRevampGridSizeChange: (value: TimelineGridSize) -> Unit,
    onTimelineRevampZoomIn: () -> Unit,
    onTimelineRevampZoomOut: () -> Unit,
    onTimelineRevampNodeClicked: (PhotosNodeContentItemV2?) -> Unit,
    onTimelineRevampTakenDownDialogConsumed: () -> Unit,
    showEnableCameraUploadsPage: Boolean,
    mediaCameraUploadUiState: MediaCameraUploadUiState,
    timelineFilterUiState: TimelineFilterUiState,
    videosSelectionUiState: VideosTabUiState.Selection,
    selectedPhotoIds: Set<Long>,
    showTimelineSortDialog: Boolean,
    selectedTimePeriod: MediaTimePeriod,
    setEnableCUPage: (Boolean) -> Unit,
    onTimelineGridSizeChange: (value: TimelineGridSize) -> Unit,
    onTimelineSortDialogDismissed: () -> Unit,
    onTimelineSortOptionChange: (value: TimelineTabSortOptions) -> Unit,
    onTimelinePhotoClick: (id: Long) -> Unit,
    onTimelinePhotoSelected: (id: Long) -> Unit,
    clearCameraUploadsCompletedMessage: () -> Unit,
    onNavigateToCameraUploadsSettings: (key: LegacySettingsCameraUploadsActivityNavKey) -> Unit,
    navigationHandler: NavigationHandler,
    handleCameraUploadsPermissionsResult: () -> Unit,
    handleNotificationPermissionResult: () -> Unit,
    onCUBannerDismissRequest: (status: CUStatusUiState) -> Unit,
    onNavigateToUpgradeAccount: (key: UpgradeAccountNavKey) -> Unit,
    onMediaTimePeriodSelected: (MediaTimePeriod) -> Unit,
    showVideoPlaylistRemovedDialog: Boolean,
    dismissVideoPlaylistRemovedDialog: () -> Unit,
    onCurrentVideosSearchQueryRequest: () -> Unit,
    updateSelectionModeAvailableActions: (selectedNodes: List<TypedNode>, nodeSourceType: NodeSourceType) -> Unit,
    modifier: Modifier = Modifier,
    timelineContentPadding: PaddingValues = PaddingValues(),
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

    when (this) {
        MediaScreen.Timeline -> {
            when (uiState.isTimelineRevampEnabled) {
                true -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        TimelineRevampScreen(
                            modifier = Modifier.fillMaxSize(),
                            uiState = timelineRevampUiState,
                            mediaCameraUploadUiState = mediaCameraUploadUiState,
                            showEnableCameraUploadsPage = showEnableCameraUploadsPage,
                            onVisibleRangeChanged = onTimelineRevampVisibleRangeChanged,
                            onGridSizeChange = onTimelineRevampGridSizeChange,
                            onZoomIn = onTimelineRevampZoomIn,
                            onZoomOut = onTimelineRevampZoomOut,
                            onMediaTimePeriodSelected = onMediaTimePeriodSelected,
                            onNodeClicked = { node ->
                                when {
                                    node == null -> return@TimelineRevampScreen
                                    node.isTakenDown -> onTimelineRevampNodeClicked(node)
                                    else -> onTimelinePhotoClick(node.id)
                                }
                            },
                            onNodeSelected = { node -> onTimelinePhotoSelected(node.id) },
                            selectedPhotoIds = selectedPhotoIds,
                            onTakenDownDialogEventConsumed = onTimelineRevampTakenDownDialogConsumed,
                            clearCameraUploadsCompletedMessage = clearCameraUploadsCompletedMessage,
                            onNavigateToCameraUploadsSettings = {
                                onNavigateToCameraUploadsSettings(
                                    LegacySettingsCameraUploadsActivityNavKey()
                                )
                            },
                            onNavigateToMobileDataSettings = {
                                onNavigateToCameraUploadsSettings(
                                    LegacySettingsCameraUploadsActivityNavKey(
                                        isShowHowToUploadPrompt = true
                                    )
                                )
                            },
                            onNavigateToUpgradeAccount = {
                                onNavigateToUpgradeAccount(UpgradeAccountNavKey())
                            },
                            onCameraUploadsBannerDismiss = onCUBannerDismissRequest,
                            handleCameraUploadsPermissionsResult = handleCameraUploadsPermissionsResult,
                            handleNotificationPermissionResult = handleNotificationPermissionResult,
                        )

                        MediaTimePeriodSelector(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .align(Alignment.BottomCenter),
                            isVisible = timelineRevampUiState is TimelineRevampUiState.Data,
                            selectedTimePeriod = selectedTimePeriod,
                            onMediaTimePeriodSelected = onMediaTimePeriodSelected,
                            periods = listOf(
                                MediaTimePeriod.Years,
                                MediaTimePeriod.Months,
                                MediaTimePeriod.All,
                            ),
                        )
                    }

                    if (showTimelineSortDialog) {
                        TimelineSortDialog(
                            selected = (timelineRevampUiState as? TimelineRevampUiState.Data)
                                ?.currentSort ?: TimelineTabSortOptions.Newest,
                            onDismissRequest = onTimelineSortDialogDismissed,
                            onOptionSelected = onTimelineSortOptionChange,
                        )
                    }
                }

                false -> {
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
                        onNavigateToUpgradeAccount = onNavigateToUpgradeAccount,
                        onMediaTimePeriodSelected = onMediaTimePeriodSelected
                    )
                }

                else -> {}
            }
        }

        MediaScreen.Albums -> {
            AlbumsTabRoute(
                modifier = Modifier.fillMaxSize(),
                showNewAlbumDialogEvent = uiState.newAlbumDialogEvent,
                resetNewAlbumDialogEvent = mainViewModel::resetNewAlbumDialog,
                onNavigate = navigationHandler::navigate,
                viewModel = albumsTabViewModel,
                contentPadding = timelineContentPadding.excludeTopPadding(),
            )
        }

        MediaScreen.Videos -> {
            VideosTabRoute(
                videosSelectionUiState = videosSelectionUiState,
                navigationHandler = navigationHandler,
                onCurrentVideosSearchQueryRequest = onCurrentVideosSearchQueryRequest,
                updateSelectionModeAvailableActions = updateSelectionModeAvailableActions,
                contentPadding = timelineContentPadding.excludeTopPadding(),
            )
        }

        MediaScreen.Playlists -> {
            VideoPlaylistsTabRoute(
                showVideoPlaylistRemovedDialog = showVideoPlaylistRemovedDialog,
                dismissVideoPlaylistRemovedDialog = dismissVideoPlaylistRemovedDialog,
                modifier = modifier,
                navigate = navigationHandler::navigate,
                contentPadding = timelineContentPadding.excludeTopPadding(),
            )
        }
    }
}

/**
 * The number of media items currently represented by the revamp timeline (0 unless it has loaded
 * sections). Used to decide whether the timeline filter action should be shown.
 */
private fun TimelineRevampUiState.mediaItemCount(): Int =
    (this as? TimelineRevampUiState.Data)?.sections?.sumOf { it.count }?.toInt() ?: 0

private fun getSelectionModeType(
    timelineSelectedPhotoCount: Int,
    albumsSelectedUserAlbumsCount: Int,
    videosSelectionUiState: VideosTabUiState.Selection,
    playlistsTabUiState: VideoPlaylistsTabUiState,
): MediaSelectionModeType = when {
    timelineSelectedPhotoCount > 0 -> MediaSelectionModeType.Timeline
    albumsSelectedUserAlbumsCount > 0 -> MediaSelectionModeType.Albums
    videosSelectionUiState.count > 0 -> MediaSelectionModeType.Videos
    playlistsTabUiState is VideoPlaylistsTabUiState.Data && playlistsTabUiState.selectedPlaylists.isNotEmpty() -> MediaSelectionModeType.Playlists
    else -> MediaSelectionModeType.None
}

@CombinedThemePreviews
@Composable
private fun PhotosMainScreenPreview() {
    AndroidThemeForPreviews {
        MediaMainScreen(
            timelineTabUiState = TimelineTabUiState(),
            timelineRevampUiState = TimelineRevampUiState.Loading,
            onTimelineRevampVisibleRangeChanged = { _, _ -> },
            onTimelineRevampGridSizeChange = {},
            onTimelineRevampZoomIn = {},
            onTimelineRevampZoomOut = {},
            onTimelineRevampNodeClicked = {},
            onTimelineRevampTakenDownDialogConsumed = {},
            timelineTabActionUiState = TimelineTabActionUiState(),
            timelineRevampActionUiState = TimelineTabActionUiState(),
            timelineFilterUiState = TimelineFilterUiState(),
            mediaCameraUploadUiState = MediaCameraUploadUiState(),
            selectedPhotoIds = setOf(),
            selectedTimePeriod = MediaTimePeriod.All,
            showTimelineFilter = false,
            selectedPhotosInTypedNode = { emptyList() },
            setEnableCUPage = {},
            onTimelineGridSizeChange = {},
            onTimelineSortOptionChange = {},
            onTimelineRevampSortOptionChange = {},
            onTimelineApplyFilterClick = {},
            timelineRevampFilterUiState = TimelineFilterUiState(),
            onTimelineRevampApplyFilterClick = {},
            timelineRevampSelectedTimePeriod = MediaTimePeriod.All,
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
            onMediaTimePeriodSelected = {},
            onTimelineRevampMediaTimePeriodSelected = {},
            onNavigateToCameraUploadsProgressScreen = {},
            albumsTabUiState = AlbumsTabUiState(),
            videosSelectionUiState = VideosTabUiState.Selection(),
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