@file:OptIn(ExperimentalMaterial3Api::class)

package mega.privacy.android.feature.photos.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
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
import mega.privacy.android.feature.photos.model.MediaAppBarAction
import mega.privacy.android.feature.photos.model.MediaAppBarAction.CameraUpload.CameraUploadStatus
import mega.privacy.android.feature.photos.model.MediaScreen
import mega.privacy.android.feature.photos.model.TimelineGridSize
import mega.privacy.android.feature.photos.presentation.albums.AlbumsTabRoute
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabActionUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabRoute
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabSortOptions
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabViewModel
import mega.privacy.android.navigation.destination.AlbumContentNavKey
import mega.privacy.android.shared.resources.R as sharedResR

@Composable
fun MediaMainRoute(
    navigateToAlbumContent: (AlbumContentNavKey) -> Unit,
    timelineViewModel: TimelineTabViewModel = hiltViewModel(),
    mediaCameraUploadViewModel: MediaCameraUploadViewModel = hiltViewModel(),
    mediaFilterViewModel: MediaFilterViewModel = hiltViewModel(),
) {
    val timelineTabUiState by timelineViewModel.uiState.collectAsStateWithLifecycle()
    val timelineTabActionUiState by timelineViewModel.actionUiState.collectAsStateWithLifecycle()
    val mediaCameraUploadUiState by mediaCameraUploadViewModel.uiState.collectAsStateWithLifecycle()
    val mediaFilterUiState by mediaFilterViewModel.uiState.collectAsStateWithLifecycle()

    MediaMainScreen(
        timelineTabUiState = timelineTabUiState,
        timelineTabActionUiState = timelineTabActionUiState,
        mediaCameraUploadUiState = mediaCameraUploadUiState,
        mediaFilterUiState = mediaFilterUiState,
        navigateToAlbumContent = navigateToAlbumContent,
        setEnableCUPage = { shouldShow ->
            mediaCameraUploadViewModel.shouldEnableCUPage(
                mediaSource = mediaFilterUiState.mediaSource,
                show = shouldShow
            )
            timelineViewModel.updateSortActionBasedOnCUPageEnablement(
                isEnableCameraUploadPageShowing = mediaCameraUploadUiState.enableCameraUploadPageShowing,
                mediaSource = mediaFilterUiState.mediaSource,
                isCUPageEnabled = shouldShow
            )
        },
        onTimelineGridSizeChange = { size ->
            timelineViewModel.onGridSizeChange(
                size = size,
                isEnableCameraUploadPageShowing = mediaCameraUploadUiState.enableCameraUploadPageShowing,
                mediaSource = mediaFilterUiState.mediaSource
            )
        },
        onTimelineSortOptionChange = timelineViewModel::onSortOptionsChange,
        loadTimelineNextPage = timelineViewModel::loadNextPage
    )
}

@Composable
fun MediaMainScreen(
    timelineTabUiState: TimelineTabUiState,
    timelineTabActionUiState: TimelineTabActionUiState,
    mediaCameraUploadUiState: MediaCameraUploadUiState,
    mediaFilterUiState: MediaFilterUiState,
    setEnableCUPage: (Boolean) -> Unit,
    onTimelineGridSizeChange: (value: TimelineGridSize) -> Unit,
    onTimelineSortOptionChange: (value: TimelineTabSortOptions) -> Unit,
    loadTimelineNextPage: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MediaMainViewModel = hiltViewModel(),
    navigateToAlbumContent: (AlbumContentNavKey) -> Unit,
) {
    var currentTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showTimelineSortDialog by rememberSaveable { mutableStateOf(false) }

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
                                    mediaFilterUiState = mediaFilterUiState,
                                    mediaCameraUploadUiState = mediaCameraUploadUiState,
                                    showTimelineSortDialog = showTimelineSortDialog,
                                    navigateToAlbumContent = navigateToAlbumContent,
                                    setEnableCUPage = setEnableCUPage,
                                    onTimelineGridSizeChange = onTimelineGridSizeChange,
                                    onTimelineSortDialogDismissed = {
                                        showTimelineSortDialog = false
                                    },
                                    onTimelineSortOptionChange = {
                                        onTimelineSortOptionChange(it)
                                        showTimelineSortDialog = false
                                    },
                                    loadTimelineNextPage = loadTimelineNextPage
                                )
                            }
                        )
                    }
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
    mediaFilterUiState: MediaFilterUiState,
    showTimelineSortDialog: Boolean,
    navigateToAlbumContent: (AlbumContentNavKey) -> Unit,
    setEnableCUPage: (Boolean) -> Unit,
    onTimelineGridSizeChange: (value: TimelineGridSize) -> Unit,
    onTimelineSortDialogDismissed: () -> Unit,
    onTimelineSortOptionChange: (value: TimelineTabSortOptions) -> Unit,
    loadTimelineNextPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

    when (this) {
        MediaScreen.Timeline -> {
            TimelineTabRoute(
                modifier = Modifier.fillMaxSize(),
                uiState = timelineTabUiState,
                mediaCameraUploadUiState = mediaCameraUploadUiState,
                mediaFilterUiState = mediaFilterUiState,
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
                onSortOptionChange = onTimelineSortOptionChange
            )
        }

        MediaScreen.Albums -> {
            AlbumsTabRoute(
                modifier = Modifier.fillMaxSize(),
                showNewAlbumDialogEvent = uiState.newAlbumDialogEvent,
                resetNewAlbumDialogEvent = mainViewModel::resetNewAlbumDialog,
                navigateToAlbumContent = navigateToAlbumContent
            )
        }

        // Todo: Implement Videos and Playlists Screens
        else -> {
            Box(modifier) {
                MegaText("Video / Playlists Screen - To be implemented")
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
            mediaCameraUploadUiState = MediaCameraUploadUiState(),
            mediaFilterUiState = MediaFilterUiState(),
            navigateToAlbumContent = {},
            setEnableCUPage = {},
            onTimelineGridSizeChange = {},
            onTimelineSortOptionChange = {},
            loadTimelineNextPage = {}
        )
    }
}
