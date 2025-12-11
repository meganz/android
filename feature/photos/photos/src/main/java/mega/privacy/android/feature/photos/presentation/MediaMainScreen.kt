@file:OptIn(ExperimentalMaterial3Api::class)

package mega.privacy.android.feature.photos.presentation

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.tabs.MegaScrollableTabRow
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.TabItems
import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.action.NodeActionHandler
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberNodeActionHandler
import mega.privacy.android.core.nodecomponents.components.AddContentFab
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeAppBar
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeBottomBar
import mega.privacy.android.core.nodecomponents.components.selectionmode.SelectionModeBottomBar
import mega.privacy.android.core.nodecomponents.dialog.rename.RenameNodeDialogNavKey
import mega.privacy.android.core.nodecomponents.menu.menuaction.CopyMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.DownloadMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.GetLinkMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.HideMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.MoveMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveLinkMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.SendToChatMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ShareMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.TrashMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.UnhideMenuAction
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.FilterMediaSource.Companion.toLegacyPhotosSource
import mega.privacy.android.feature.photos.model.FilterMediaType
import mega.privacy.android.feature.photos.model.MediaAppBarAction
import mega.privacy.android.feature.photos.model.MediaAppBarAction.CameraUpload.CameraUploadStatus
import mega.privacy.android.feature.photos.model.MediaScreen
import mega.privacy.android.feature.photos.model.PhotoNodeUiState
import mega.privacy.android.feature.photos.model.TimelineGridSize
import mega.privacy.android.feature.photos.presentation.albums.AlbumsTabRoute
import mega.privacy.android.feature.photos.presentation.timeline.TimelineFilterUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabActionUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabRoute
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabSortOptions
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabSortOptions.Companion.toLegacySort
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabViewModel
import mega.privacy.android.feature.photos.presentation.timeline.component.TimelineFilterView
import mega.privacy.android.feature.photos.presentation.timeline.component.TimelineTabActionBottomSheet
import mega.privacy.android.feature.photos.presentation.timeline.model.TimelineFilterRequest
import mega.privacy.android.feature.photos.presentation.timeline.model.TimelineSelectionMenuAction
import mega.privacy.android.feature.photos.presentation.videos.VideosTabRoute
import mega.privacy.android.feature.photos.presentation.videos.VideosTabUiState
import mega.privacy.android.feature.photos.presentation.videos.VideosTabViewModel
import mega.privacy.android.feature.photos.presentation.videos.view.VideosTabToolbar
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.queue.snackbar.rememberSnackBarQueue
import mega.privacy.android.navigation.destination.AlbumContentNavKey
import mega.privacy.android.navigation.destination.LegacyAddToAlbumActivityNavKey
import mega.privacy.android.navigation.destination.LegacyPhotoSelectionNavKey
import mega.privacy.android.navigation.destination.LegacySettingsCameraUploadsActivityNavKey
import mega.privacy.android.navigation.destination.LegacyPhotosSearchNavKey
import mega.privacy.android.navigation.destination.MediaTimelinePhotoPreviewNavKey
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.navigation.extensions.rememberMegaResultContract
import mega.privacy.android.shared.resources.R as sharedResR
import mega.privacy.mobile.analytics.event.TimelineHideNodeMenuItemEvent
import timber.log.Timber

@SuppressLint("ComposeViewModelForwarding")
@Composable
fun MediaMainRoute(
    navigationHandler: NavigationHandler,
    navigateToAlbumContent: (AlbumContentNavKey) -> Unit,
    navigateToLegacyPhotoSelection: (LegacyPhotoSelectionNavKey) -> Unit,
    setNavigationItemVisibility: (Boolean) -> Unit,
    onNavigateToTimelinePhotoPreview: (key: MediaTimelinePhotoPreviewNavKey) -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    onNavigateToAddToAlbum: (key: LegacyAddToAlbumActivityNavKey) -> Unit,
    onNavigateToCameraUploadsSettings: (key: LegacySettingsCameraUploadsActivityNavKey) -> Unit,
    timelineViewModel: TimelineTabViewModel = hiltViewModel(),
    mediaCameraUploadViewModel: MediaCameraUploadViewModel = hiltViewModel(),
    nodeOptionsActionViewModel: NodeOptionsActionViewModel = hiltViewModel(),
    videosTabViewModel: VideosTabViewModel = hiltViewModel(),
) {
    val timelineTabUiState by timelineViewModel.uiState.collectAsStateWithLifecycle()
    val timelineTabActionUiState by timelineViewModel.actionUiState.collectAsStateWithLifecycle()
    val timelineFilterUiState by timelineViewModel.filterUiState.collectAsStateWithLifecycle()
    val mediaCameraUploadUiState by mediaCameraUploadViewModel.uiState.collectAsStateWithLifecycle()
    val videosTabUiState by videosTabViewModel.uiState.collectAsStateWithLifecycle()
    val megaNavigator = rememberMegaNavigator()
    val nodeActionHandler = rememberNodeActionHandler(
        viewModel = nodeOptionsActionViewModel,
        navigationHandler = navigationHandler,
        megaNavigator = megaNavigator
    )
    val snackBarEventQueue = rememberSnackBarQueue()
    val megaResultContract = rememberMegaResultContract()
    val scope = rememberCoroutineScope()
    val nameCollisionLauncher = rememberLauncherForActivityResult(
        contract = megaResultContract.nameCollisionActivityContract
    ) { message ->
        if (!message.isNullOrEmpty()) {
            scope.launch {
                snackBarEventQueue.queueMessage(message)
            }
        }
    }

    NodeActionEventHandler(
        nodeOptionsActionViewModel = nodeOptionsActionViewModel,
        nameCollisionLauncher = nameCollisionLauncher,
        onTransfer = onTransfer,
        onShowSnackBar = {
            scope.launch {
                snackBarEventQueue.queueMessage(it)
            }
        },
        onActionFinished = {
            if (videosTabUiState is VideosTabUiState.Data) {
                val state = videosTabUiState as VideosTabUiState.Data
                if (state.selectedTypedNodes.isNotEmpty()) {
                    videosTabViewModel.clearSelection()
                }
            }
        },
        onNavigateToRenameNav = {
            navigationHandler.navigate(it)
        },
    )

    LaunchedEffect(
        timelineTabUiState.displayedPhotos,
        timelineTabUiState.isLoading,
    ) {
        if (!timelineTabUiState.isLoading) {
            mediaCameraUploadViewModel.updateCUPageEnablementBasedOnDisplayedPhotos(
                photos = timelineTabUiState.displayedPhotos
            )
        }
    }

    LaunchedEffect(
        mediaCameraUploadUiState.enableCameraUploadPageShowing,
        timelineFilterUiState.mediaSource,
        timelineTabUiState.isLoading,
    ) {
        if (!timelineTabUiState.isLoading) {
            timelineViewModel.updateSortActionEnablement(
                isEnableCameraUploadPageShowing = mediaCameraUploadUiState.enableCameraUploadPageShowing,
                mediaSource = timelineFilterUiState.mediaSource
            )
        }
    }

    MediaMainScreen(
        timelineTabUiState = timelineTabUiState,
        timelineTabActionUiState = timelineTabActionUiState,
        mediaCameraUploadUiState = mediaCameraUploadUiState,
        timelineFilterUiState = timelineFilterUiState,
        actionHandler = nodeActionHandler::invoke,
        navigateToAlbumContent = navigateToAlbumContent,
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
        loadTimelineNextPage = timelineViewModel::loadNextPage,
        onTimelineApplyFilterClick = timelineViewModel::onFilterChange,
        setNavigationItemVisibility = setNavigationItemVisibility,
        resetCUButtonAndProgress = mediaCameraUploadViewModel::resetCUButtonAndProgress,
        navigateToLegacyPhotoSelection = navigateToLegacyPhotoSelection,
        onTimelinePhotoSelected = timelineViewModel::onPhotoSelected,
        onAllTimelinePhotosSelected = timelineViewModel::onSelectAllPhotos,
        onClearTimelinePhotosSelection = timelineViewModel::onDeselectAllPhotos,
        onNavigateToTimelinePhotoPreview = onNavigateToTimelinePhotoPreview,
        onNavigateToAddToAlbum = onNavigateToAddToAlbum,
        clearCameraUploadsMessage = {
            mediaCameraUploadViewModel.setCameraUploadsMessage("")
        },
        clearCameraUploadsCompletedMessage = {
            mediaCameraUploadViewModel.setCameraUploadsCompletedMessage(false)
        },
        onChangeCameraUploadsPermissions = {},
        clearCameraUploadsChangePermissionsMessage = {
            mediaCameraUploadViewModel.showCameraUploadsChangePermissionsMessage(false)
        },
        onNavigateCameraUploadsSettings = {
            onNavigateToCameraUploadsSettings(LegacySettingsCameraUploadsActivityNavKey)
        },
        onDismissEnableCameraUploadsBanner = mediaCameraUploadViewModel::dismissEnableCUBanner,
        nodeActionHandle = nodeActionHandler,
        navigateToLegacyPhotosSearch = navigationHandler::navigate,
        onTransfer = onTransfer,
        navigationHandler = navigationHandler,
    )
}

@Composable
fun MediaMainScreen(
    timelineTabUiState: TimelineTabUiState,
    timelineTabActionUiState: TimelineTabActionUiState,
    mediaCameraUploadUiState: MediaCameraUploadUiState,
    nodeActionHandle: NodeActionHandler,
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
    timelineFilterUiState: TimelineFilterUiState,
    actionHandler: (MenuAction, List<TypedNode>) -> Unit,
    setEnableCUPage: (Boolean) -> Unit,
    onTimelineGridSizeChange: (value: TimelineGridSize) -> Unit,
    onTimelineSortOptionChange: (value: TimelineTabSortOptions) -> Unit,
    loadTimelineNextPage: () -> Unit,
    onTimelineApplyFilterClick: (request: TimelineFilterRequest) -> Unit,
    setNavigationItemVisibility: (Boolean) -> Unit,
    resetCUButtonAndProgress: () -> Unit,
    navigateToAlbumContent: (AlbumContentNavKey) -> Unit,
    navigateToLegacyPhotoSelection: (LegacyPhotoSelectionNavKey) -> Unit,
    navigateToLegacyPhotosSearch: (LegacyPhotosSearchNavKey) -> Unit,
    onTimelinePhotoSelected: (nodes: PhotoNodeUiState) -> Unit,
    onAllTimelinePhotosSelected: () -> Unit,
    onClearTimelinePhotosSelection: () -> Unit,
    onNavigateToTimelinePhotoPreview: (key: MediaTimelinePhotoPreviewNavKey) -> Unit,
    onNavigateToAddToAlbum: (key: LegacyAddToAlbumActivityNavKey) -> Unit,
    clearCameraUploadsMessage: () -> Unit,
    clearCameraUploadsCompletedMessage: () -> Unit,
    onChangeCameraUploadsPermissions: () -> Unit,
    clearCameraUploadsChangePermissionsMessage: () -> Unit,
    onNavigateCameraUploadsSettings: () -> Unit,
    onDismissEnableCameraUploadsBanner: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MediaMainViewModel = hiltViewModel(),
    videosTabViewModel: VideosTabViewModel = hiltViewModel(),
    nodeOptionsActionViewModel: NodeOptionsActionViewModel = hiltViewModel(),
) {
    var currentTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showTimelineSortDialog by rememberSaveable { mutableStateOf(false) }
    var showTimelineFilter by rememberSaveable { mutableStateOf(false) }
    val isTimelineInSelectionMode by remember(timelineTabUiState.selectedPhotoCount) {
        derivedStateOf { timelineTabUiState.selectedPhotoCount > 0 }
    }
    val areAllTimelinePhotosSelected by remember(
        timelineTabUiState.selectedPhotoCount,
        timelineTabUiState.displayedPhotos
    ) {
        derivedStateOf { timelineTabUiState.selectedPhotoCount == timelineTabUiState.displayedPhotos.size }
    }
    var showBottomSheetActions by rememberSaveable { mutableStateOf(false) }

    val nodeOptionsActionUiState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()
    val videosTabUiState by videosTabViewModel.uiState.collectAsStateWithLifecycle()
    var isVideosTabSearchBarVisible by rememberSaveable { mutableStateOf(false) }
    var videosTabQuery by rememberSaveable { mutableStateOf<String?>(null) }

    var selectedVideoCount by rememberSaveable { mutableIntStateOf(0) }
    var areAllVideosSelected by rememberSaveable { mutableStateOf(false) }
    var isVideosSelectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedVideoNodes by remember { mutableStateOf(emptyList<TypedNode>()) }

    // Handling back handler for timeline filter
    BackHandler(enabled = showTimelineFilter) {
        if (showTimelineFilter) {
            showTimelineFilter = false
            setNavigationItemVisibility(true)
        }
    }

    // Handling back handler for timeline selection mode
    BackHandler(enabled = isTimelineInSelectionMode) {
        if (isTimelineInSelectionMode) {
            onClearTimelinePhotosSelection()
        }
    }

    LaunchedEffect(isTimelineInSelectionMode) {
        setNavigationItemVisibility(!isTimelineInSelectionMode)
    }

    BackHandler(isVideosTabSearchBarVisible) {
        if (isVideosTabSearchBarVisible) {
            isVideosTabSearchBarVisible = false
            videosTabViewModel.searchQuery(null)
        }
    }

    BackHandler(isVideosSelectionMode) {
        if (isVideosSelectionMode) {
            videosTabViewModel.clearSelection()
        }
    }

    LaunchedEffect(isVideosSelectionMode) {
        setNavigationItemVisibility(!isVideosSelectionMode)
    }

    LaunchedEffect(videosTabUiState) {
        if (videosTabUiState is VideosTabUiState.Data) {
            val state = videosTabUiState as VideosTabUiState.Data
            videosTabViewModel.getCurrentSearchQuery().let {
                if (it != videosTabQuery) {
                    videosTabQuery = it
                }
            }
            if (selectedVideoCount != state.selectedTypedNodes.size) {
                selectedVideoNodes = state.selectedTypedNodes
                selectedVideoCount = state.selectedTypedNodes.size
                areAllVideosSelected = selectedVideoCount == state.allVideoEntities.size
                isVideosSelectionMode = state.selectedTypedNodes.isNotEmpty()
                nodeOptionsActionViewModel.updateSelectionModeAvailableActions(
                    selectedNodes = state.selectedTypedNodes.toSet(),
                    nodeSourceType = NodeSourceType.CLOUD_DRIVE
                )
            }
        }
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            AddContentFab(
                visible = currentTabIndex == MediaScreen.Albums.ordinal,
                onClick = viewModel::showNewAlbumDialog
            )
        },
        topBar = {
            when {
                isTimelineInSelectionMode -> NodeSelectionModeAppBar(
                    count = timelineTabUiState.selectedPhotoCount,
                    isAllSelected = areAllTimelinePhotosSelected,
                    isSelecting = false,
                    onSelectAllClicked = onAllTimelinePhotosSelected,
                    onCancelSelectionClicked = onClearTimelinePhotosSelection
                )

                isVideosTabSearchBarVisible || isVideosSelectionMode ->
                    VideosTabToolbar(
                        count = selectedVideoCount,
                        isAllSelected = areAllVideosSelected,
                        isSelectionMode = isVideosSelectionMode,
                        onSelectAllClicked = videosTabViewModel::selectAllVideos,
                        onCancelSelectionClicked = videosTabViewModel::clearSelection,
                        searchQuery = videosTabQuery,
                        updateSearchQuery = videosTabViewModel::searchQuery,
                        onSearchingModeChanged = { isSearching ->
                            if (!isSearching) {
                                isVideosTabSearchBarVisible = false
                                videosTabViewModel.searchQuery(null)
                            }
                        }
                    )

                else -> MegaTopAppBar(
                    navigationType = AppBarNavigationType.None,
                    title = stringResource(sharedResR.string.media_feature_title),
                    actions = buildList {
                        add(
                            MenuActionWithClick(
                                menuAction = MediaAppBarAction.CameraUpload(CameraUploadStatus.Default),
                                onClick = onNavigateCameraUploadsSettings
                            )
                        )

                        add(
                            MenuActionWithClick(menuAction = MediaAppBarAction.Search) {
                                if (currentTabIndex == MediaScreen.Videos.ordinal) {
                                    isVideosTabSearchBarVisible = true
                                }

                                navigateToLegacyPhotosSearch(LegacyPhotosSearchNavKey)
                            }
                        )

                        // Menu actions for timeline tab
                        if (currentTabIndex == MediaScreen.Timeline.ordinal) {
                            val isFilterApplied =
                                timelineFilterUiState.mediaType != FilterMediaType.ALL_MEDIA || timelineFilterUiState.mediaSource != FilterMediaSource.AllPhotos
                            if (isFilterApplied) {
                                add(
                                    MenuActionWithClick(menuAction = MediaAppBarAction.Filter) {
                                        showTimelineFilter = true
                                    }
                                )
                            }

                            add(
                                MenuActionWithClick(menuAction = MediaAppBarAction.FilterSecondary) {
                                    showTimelineFilter = true
                                }
                            )

                            if (timelineTabActionUiState.normalModeItem.enableSort) {
                                add(
                                    MenuActionWithClick(menuAction = MediaAppBarAction.SortBy) {
                                        showTimelineSortDialog = true
                                    }
                                )
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            SelectionModeBottomBar(
                visible = isTimelineInSelectionMode,
                actions = timelineTabActionUiState.selectionModeItem.bottomBarActions,
                onActionPressed = {
                    when (it) {
                        TimelineSelectionMenuAction.Download -> {
                            actionHandler(
                                DownloadMenuAction(),
                                timelineTabUiState.selectedPhotosInTypedNode
                            )
                            onClearTimelinePhotosSelection()
                        }

                        TimelineSelectionMenuAction.ShareLink -> {
                            actionHandler(
                                GetLinkMenuAction(),
                                timelineTabUiState.selectedPhotosInTypedNode
                            )
                            onClearTimelinePhotosSelection()
                        }

                        TimelineSelectionMenuAction.SendToChat -> {
                            actionHandler(
                                SendToChatMenuAction(),
                                timelineTabUiState.selectedPhotosInTypedNode
                            )
                            onClearTimelinePhotosSelection()
                        }

                        TimelineSelectionMenuAction.Share -> {
                            actionHandler(
                                ShareMenuAction(),
                                timelineTabUiState.selectedPhotosInTypedNode
                            )
                            onClearTimelinePhotosSelection()
                        }

                        TimelineSelectionMenuAction.MoveToRubbishBin -> {
                            actionHandler(
                                TrashMenuAction(),
                                timelineTabUiState.selectedPhotosInTypedNode
                            )
                            onClearTimelinePhotosSelection()
                        }

                        TimelineSelectionMenuAction.More -> {
                            showBottomSheetActions = true
                        }
                    }
                }
            )

            NodeSelectionModeBottomBar(
                availableActions = nodeOptionsActionUiState.availableActions,
                visibleActions = nodeOptionsActionUiState.visibleActions,
                visible = nodeOptionsActionUiState.visibleActions.isNotEmpty() && isVideosSelectionMode,
                nodeActionHandler = nodeActionHandle,
                selectedNodes = selectedVideoNodes,
                isSelecting = false
            )
        }
    ) { paddingValues ->
        MegaScrollableTabRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            beyondViewportPageCount = 1,
            hideTabs = isTimelineInSelectionMode || isVideosSelectionMode,
            pagerScrollEnabled = true,
            initialSelectedIndex = currentTabIndex,
            onTabSelected = { index ->
                currentTabIndex = index
                true
            },
            cells = {
                MediaScreen.entries.forEach { tab ->
                    with(tab) {
                        addTextTabWithScrollableContent(
                            tabItem = getTabItem(),
                            content = { _, modifier ->
                                MediaContent(
                                    modifier = Modifier.fillMaxSize(),
                                    timelineContentPadding = PaddingValues(
                                        bottom = if (isTimelineInSelectionMode || isVideosSelectionMode) {
                                            paddingValues.calculateBottomPadding()
                                        } else {
                                            50.dp
                                        }
                                    ),
                                    mainViewModel = viewModel,
                                    timelineTabUiState = timelineTabUiState,
                                    timelineFilterUiState = timelineFilterUiState,
                                    mediaCameraUploadUiState = mediaCameraUploadUiState,
                                    showTimelineSortDialog = showTimelineSortDialog,
                                    navigateToAlbumContent = navigateToAlbumContent,
                                    navigateToLegacyPhotoSelection = navigateToLegacyPhotoSelection,
                                    setEnableCUPage = setEnableCUPage,
                                    onTimelineGridSizeChange = onTimelineGridSizeChange,
                                    onTimelineSortDialogDismissed = {
                                        showTimelineSortDialog = false
                                    },
                                    onTimelineSortOptionChange = {
                                        onTimelineSortOptionChange(it)
                                        showTimelineSortDialog = false
                                    },
                                    loadTimelineNextPage = loadTimelineNextPage,
                                    resetCUButtonAndProgress = resetCUButtonAndProgress,
                                    onTimelinePhotoClick = {
                                        if (isTimelineInSelectionMode) {
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
                                    clearCameraUploadsMessage = clearCameraUploadsMessage,
                                    clearCameraUploadsCompletedMessage = clearCameraUploadsCompletedMessage,
                                    onChangeCameraUploadsPermissions = onChangeCameraUploadsPermissions,
                                    clearCameraUploadsChangePermissionsMessage = clearCameraUploadsChangePermissionsMessage,
                                    onNavigateCameraUploadsSettings = onNavigateCameraUploadsSettings,
                                    onDismissEnableCameraUploadsBanner = onDismissEnableCameraUploadsBanner,
                                    onTransfer = onTransfer,
                                    navigationHandler = navigationHandler,
                                )
                            }
                        )
                    }
                }
            }
        )
    }

    if (showTimelineFilter) {
        setNavigationItemVisibility(false)
        TimelineFilterView(
            modifier = Modifier.fillMaxSize(),
            currentFilter = timelineFilterUiState,
            onApplyFilterClick = { request ->
                onTimelineApplyFilterClick(request)
                showTimelineFilter = false
                setNavigationItemVisibility(true)
            },
            onClose = {
                showTimelineFilter = false
                setNavigationItemVisibility(true)
            },
        )
    }

    if (showBottomSheetActions) {
        TimelineTabActionBottomSheet(
            actions = timelineTabActionUiState.selectionModeItem.bottomSheetActions,
            onDismissRequest = { showBottomSheetActions = false },
            onActionPressed = { action ->
                when (action) {
                    TimelineSelectionMenuAction.RemoveLink -> {
                        actionHandler(
                            RemoveLinkMenuAction(),
                            timelineTabUiState.selectedPhotosInTypedNode
                        )
                        onClearTimelinePhotosSelection()
                    }

                    TimelineSelectionMenuAction.Hide -> {
                        Analytics.tracker.trackEvent(TimelineHideNodeMenuItemEvent)
                        actionHandler(
                            HideMenuAction(),
                            timelineTabUiState.selectedPhotosInTypedNode
                        )
                        onClearTimelinePhotosSelection()
                    }

                    TimelineSelectionMenuAction.Unhide -> {
                        actionHandler(
                            UnhideMenuAction(),
                            timelineTabUiState.selectedPhotosInTypedNode
                        )
                        onClearTimelinePhotosSelection()
                    }

                    TimelineSelectionMenuAction.Move -> {
                        actionHandler(
                            MoveMenuAction(),
                            timelineTabUiState.selectedPhotosInTypedNode
                        )
                        onClearTimelinePhotosSelection()
                    }

                    TimelineSelectionMenuAction.Copy -> {
                        actionHandler(
                            CopyMenuAction(),
                            timelineTabUiState.selectedPhotosInTypedNode
                        )
                        onClearTimelinePhotosSelection()
                    }

                    TimelineSelectionMenuAction.AddToAlbum -> {
                        onNavigateToAddToAlbum(
                            LegacyAddToAlbumActivityNavKey(
                                photoIds = timelineTabUiState.selectedPhotosInTypedNode.map { it.id.longValue },
                                viewType = 0
                            )
                        )
                        onClearTimelinePhotosSelection()
                    }

                    else -> Unit
                }
            }
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
    timelineTabUiState: TimelineTabUiState,
    mediaCameraUploadUiState: MediaCameraUploadUiState,
    timelineFilterUiState: TimelineFilterUiState,
    showTimelineSortDialog: Boolean,
    navigateToAlbumContent: (AlbumContentNavKey) -> Unit,
    navigateToLegacyPhotoSelection: (LegacyPhotoSelectionNavKey) -> Unit,
    setEnableCUPage: (Boolean) -> Unit,
    onTimelineGridSizeChange: (value: TimelineGridSize) -> Unit,
    onTimelineSortDialogDismissed: () -> Unit,
    onTimelineSortOptionChange: (value: TimelineTabSortOptions) -> Unit,
    loadTimelineNextPage: () -> Unit,
    resetCUButtonAndProgress: () -> Unit,
    onTimelinePhotoClick: (node: PhotoNodeUiState) -> Unit,
    onTimelinePhotoSelected: (node: PhotoNodeUiState) -> Unit,
    clearCameraUploadsMessage: () -> Unit,
    clearCameraUploadsCompletedMessage: () -> Unit,
    onChangeCameraUploadsPermissions: () -> Unit,
    clearCameraUploadsChangePermissionsMessage: () -> Unit,
    onNavigateCameraUploadsSettings: () -> Unit,
    onDismissEnableCameraUploadsBanner: () -> Unit,
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
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
                showTimelineSortDialog = showTimelineSortDialog,
                clearCameraUploadsMessage = clearCameraUploadsMessage,
                clearCameraUploadsCompletedMessage = clearCameraUploadsCompletedMessage,
                onChangeCameraUploadsPermissions = onChangeCameraUploadsPermissions,
                clearCameraUploadsChangePermissionsMessage = clearCameraUploadsChangePermissionsMessage,
                loadNextPage = loadTimelineNextPage,
                onNavigateCameraUploadsSettings = onNavigateCameraUploadsSettings,
                setEnableCUPage = setEnableCUPage,
                onGridSizeChange = onTimelineGridSizeChange,
                onSortDialogDismissed = onTimelineSortDialogDismissed,
                onSortOptionChange = onTimelineSortOptionChange,
                resetCUButtonAndProgress = resetCUButtonAndProgress,
                onPhotoClick = onTimelinePhotoClick,
                onPhotoSelected = onTimelinePhotoSelected,
                onDismissEnableCameraUploadsBanner = onDismissEnableCameraUploadsBanner
            )
        }

        MediaScreen.Albums -> {
            AlbumsTabRoute(
                modifier = Modifier.fillMaxSize(),
                showNewAlbumDialogEvent = uiState.newAlbumDialogEvent,
                resetNewAlbumDialogEvent = mainViewModel::resetNewAlbumDialog,
                navigateToAlbumContent = navigateToAlbumContent,
                navigateToLegacyPhotoSelection = navigateToLegacyPhotoSelection
            )
        }

        MediaScreen.Videos -> VideosTabRoute(
            onTransfer = onTransfer,
            navigationHandler = navigationHandler
        )

        //TODO: Implement Playlists Screens
        else -> {
            Box(modifier) {
                MegaText("Playlists Screen - To be implemented")
            }
        }
    }
}

@Composable
private fun NodeActionEventHandler(
    nodeOptionsActionViewModel: NodeOptionsActionViewModel,
    nameCollisionLauncher: ManagedActivityResultLauncher<ArrayList<NameCollision>, String?>,
    onTransfer: (TransferTriggerEvent) -> Unit,
    onShowSnackBar: (message: String) -> Unit,
    onActionFinished: () -> Unit = {},
    onNavigateToRenameNav: (NavKey) -> Unit = {},
) {
    val context = LocalContext.current
    val nodeActionState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()

    EventEffect(
        event = nodeActionState.downloadEvent,
        onConsumed = nodeOptionsActionViewModel::markDownloadEventConsumed,
        action = { event ->
            onTransfer(event)
            onActionFinished()
        }
    )

    EventEffect(
        event = nodeActionState.infoToShowEvent,
        onConsumed = nodeOptionsActionViewModel::onInfoToShowEventConsumed,
        action = { info ->
            onShowSnackBar(info.get(context))
            onActionFinished()
        }
    )

    EventEffect(
        event = nodeActionState.nodeNameCollisionsResult,
        onConsumed = nodeOptionsActionViewModel::markHandleNodeNameCollisionResult,
        action = {
            if (it.conflictNodes.isNotEmpty()) {
                nameCollisionLauncher.launch(it.conflictNodes.values.toCollection(ArrayList()))
            }
            if (it.noConflictNodes.isNotEmpty()) {
                when (it.type) {
                    NodeNameCollisionType.MOVE -> nodeOptionsActionViewModel.moveNodes(it.noConflictNodes)
                    NodeNameCollisionType.COPY -> nodeOptionsActionViewModel.copyNodes(it.noConflictNodes)
                    else -> Timber.d("Not implemented")
                }
            }
            onActionFinished()
        }
    )

    EventEffect(
        event = nodeActionState.renameNodeRequestEvent,
        onConsumed = nodeOptionsActionViewModel::resetRenameNodeRequest,
        action = { nodeId ->
            onNavigateToRenameNav(RenameNodeDialogNavKey(nodeId = nodeId.longValue))
            onActionFinished()
        }
    )
}

@CombinedThemePreviews
@Composable
fun PhotosMainScreenPreview() {
    AndroidThemeForPreviews {
        MediaMainScreen(
            timelineTabUiState = TimelineTabUiState(),
            timelineTabActionUiState = TimelineTabActionUiState(),
            timelineFilterUiState = TimelineFilterUiState(),
            mediaCameraUploadUiState = MediaCameraUploadUiState(),
            actionHandler = { _, _ -> },
            navigateToAlbumContent = {},
            setEnableCUPage = {},
            onTimelineGridSizeChange = {},
            onTimelineSortOptionChange = {},
            loadTimelineNextPage = {},
            onTimelineApplyFilterClick = {},
            setNavigationItemVisibility = {},
            resetCUButtonAndProgress = {},
            navigateToLegacyPhotoSelection = {},
            onTimelinePhotoSelected = {},
            onAllTimelinePhotosSelected = {},
            onClearTimelinePhotosSelection = {},
            onNavigateToTimelinePhotoPreview = {},
            onNavigateToAddToAlbum = {},
            clearCameraUploadsMessage = {},
            clearCameraUploadsCompletedMessage = {},
            onChangeCameraUploadsPermissions = {},
            clearCameraUploadsChangePermissionsMessage = {},
            onNavigateCameraUploadsSettings = {},
            onDismissEnableCameraUploadsBanner = {},
            nodeActionHandle = rememberNodeActionHandler(),
            navigateToLegacyPhotosSearch = {},
            onTransfer = {},
            navigationHandler = object : NavigationHandler {
                override fun back() {}
                override fun remove(navKey: NavKey) {}
                override fun navigate(destination: NavKey) {}
                override fun navigate(destinations: List<NavKey>) {}
                override fun backTo(destination: NavKey, inclusive: Boolean) {}
                override fun navigateAndClearBackStack(destination: NavKey) {}
                override fun navigateAndClearTo(
                    destination: NavKey,
                    newParent: NavKey,
                    inclusive: Boolean,
                ) {
                }

                override fun <T> returnResult(key: String, value: T) {}
                override fun clearResult(key: String) {}
                override fun <T> monitorResult(key: String): Flow<T?> = flowOf(null)
            },
        )
    }
}
