@file:OptIn(ExperimentalMaterial3Api::class)

package mega.privacy.android.feature.photos.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.tabs.MegaScrollableTabRow
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.TabItems
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.core.nodecomponents.components.AddContentFab
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.FilterMediaType
import mega.privacy.android.feature.photos.model.MediaAppBarAction
import mega.privacy.android.feature.photos.model.MediaAppBarAction.CameraUpload.CameraUploadStatus
import mega.privacy.android.feature.photos.model.MediaScreen
import mega.privacy.android.feature.photos.model.TimelineGridSize
import mega.privacy.android.feature.photos.presentation.albums.AlbumsTabRoute
import mega.privacy.android.feature.photos.presentation.timeline.TimelineFilterUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabActionUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabRoute
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabSortOptions
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabViewModel
import mega.privacy.android.feature.photos.presentation.videos.VideosTabRoute
import mega.privacy.android.feature.photos.presentation.timeline.component.TimelineFilterView
import mega.privacy.android.feature.photos.presentation.timeline.model.TimelineFilterRequest
import mega.privacy.android.navigation.destination.AlbumContentNavKey
import mega.privacy.android.navigation.destination.LegacyPhotoSelectionNavKey
import mega.privacy.android.shared.resources.R as sharedResR

@Composable
fun MediaMainRoute(
    navigateToAlbumContent: (AlbumContentNavKey) -> Unit,
    navigateToLegacyPhotoSelection: (LegacyPhotoSelectionNavKey) -> Unit,
    setNavigationItemVisibility: (Boolean) -> Unit,
    timelineViewModel: TimelineTabViewModel = hiltViewModel(),
    mediaCameraUploadViewModel: MediaCameraUploadViewModel = hiltViewModel(),
) {
    val timelineTabUiState by timelineViewModel.uiState.collectAsStateWithLifecycle()
    val timelineTabActionUiState by timelineViewModel.actionUiState.collectAsStateWithLifecycle()
    val timelineFilterUiState by timelineViewModel.filterUiState.collectAsStateWithLifecycle()
    val mediaCameraUploadUiState by mediaCameraUploadViewModel.uiState.collectAsStateWithLifecycle()

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
        navigateToLegacyPhotoSelection = navigateToLegacyPhotoSelection
    )
}

@Composable
fun MediaMainScreen(
    timelineTabUiState: TimelineTabUiState,
    timelineTabActionUiState: TimelineTabActionUiState,
    mediaCameraUploadUiState: MediaCameraUploadUiState,
    timelineFilterUiState: TimelineFilterUiState,
    setEnableCUPage: (Boolean) -> Unit,
    onTimelineGridSizeChange: (value: TimelineGridSize) -> Unit,
    onTimelineSortOptionChange: (value: TimelineTabSortOptions) -> Unit,
    loadTimelineNextPage: () -> Unit,
    onTimelineApplyFilterClick: (request: TimelineFilterRequest) -> Unit,
    setNavigationItemVisibility: (Boolean) -> Unit,
    resetCUButtonAndProgress: () -> Unit,
    navigateToAlbumContent: (AlbumContentNavKey) -> Unit,
    navigateToLegacyPhotoSelection: (LegacyPhotoSelectionNavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MediaMainViewModel = hiltViewModel(),
) {
    var currentTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showTimelineSortDialog by rememberSaveable { mutableStateOf(false) }
    var showTimelineFilter by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = showTimelineFilter) {
        showTimelineFilter = false
        setNavigationItemVisibility(true)
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
            MegaTopAppBar(
                navigationType = AppBarNavigationType.None,
                title = stringResource(sharedResR.string.media_feature_title),
                actions = buildList {
                    add(
                        MenuActionWithClick(
                            menuAction = MediaAppBarAction.CameraUpload(CameraUploadStatus.Default)
                        ) {
                            // Todo: Handle Camera Upload action click
                        }
                    )

                    add(
                        MenuActionWithClick(menuAction = MediaAppBarAction.Search) {
                            // Todo: Handle Search action click
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

                        if (timelineTabActionUiState.enableSort) {
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
    ) { paddingValues ->
        MegaScrollableTabRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            beyondViewportPageCount = 1,
            hideTabs = false,
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
                                    resetCUButtonAndProgress = resetCUButtonAndProgress
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
    modifier: Modifier = Modifier,
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

    when (this) {
        MediaScreen.Timeline -> {
            TimelineTabRoute(
                modifier = Modifier.fillMaxSize(),
                uiState = timelineTabUiState,
                mediaCameraUploadUiState = mediaCameraUploadUiState,
                timelineFilterUiState = timelineFilterUiState,
                showTimelineSortDialog = showTimelineSortDialog,
                clearCameraUploadsMessage = {},
                clearCameraUploadsCompletedMessage = {},
                onChangeCameraUploadsPermissions = {},
                clearCameraUploadsChangePermissionsMessage = {},
                loadNextPage = loadTimelineNextPage,
                onNavigateCameraUploadsSettings = {},
                setEnableCUPage = setEnableCUPage,
                onGridSizeChange = onTimelineGridSizeChange,
                onSortDialogDismissed = onTimelineSortDialogDismissed,
                onSortOptionChange = onTimelineSortOptionChange,
                resetCUButtonAndProgress = resetCUButtonAndProgress
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

        MediaScreen.Videos -> VideosTabRoute()

        // Todo: Implement Playlists Screens
        else -> {
            Box(modifier) {
                MegaText("Playlists Screen - To be implemented")
            }
        }
    }
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
            navigateToAlbumContent = {},
            setEnableCUPage = {},
            onTimelineGridSizeChange = {},
            onTimelineSortOptionChange = {},
            loadTimelineNextPage = {},
            onTimelineApplyFilterClick = {},
            setNavigationItemVisibility = {},
            resetCUButtonAndProgress = {},
            navigateToLegacyPhotoSelection = {}
        )
    }
}
