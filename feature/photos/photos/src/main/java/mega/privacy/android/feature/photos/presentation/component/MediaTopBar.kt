package mega.privacy.android.feature.photos.presentation.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeAppBar
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.FilterMediaType
import mega.privacy.android.feature.photos.model.MediaAppBarAction
import mega.privacy.android.feature.photos.model.MediaScreen
import mega.privacy.android.feature.photos.presentation.CUStatusUiState
import mega.privacy.android.feature.photos.presentation.MediaCameraUploadUiState
import mega.privacy.android.feature.photos.presentation.MediaMainUiState
import mega.privacy.android.feature.photos.presentation.albums.AlbumsTabUiState
import mega.privacy.android.feature.photos.presentation.handler.MediaSelectionModeType
import mega.privacy.android.feature.photos.presentation.playlists.VideoPlaylistsTabUiState
import mega.privacy.android.feature.photos.presentation.playlists.view.VideoPlaylistsTabAppBar
import mega.privacy.android.feature.photos.presentation.timeline.TimelineFilterUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabActionUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabUiState
import mega.privacy.android.feature.photos.presentation.timeline.component.CameraUploadStatusToolbarAction
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotoModificationTimePeriod
import mega.privacy.android.feature.photos.presentation.videos.VideosTabUiState
import mega.privacy.android.feature.photos.presentation.videos.view.VideosTabToolbar
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.destination.LegacyPhotosSearchNavKey
import mega.privacy.android.navigation.destination.MediaSearchNavKey
import mega.privacy.android.shared.resources.R as SharedR

@Composable
internal fun MediaTopBar(
    currentTabIndex: Int,
    selectionModeType: MediaSelectionModeType,
    mediaMainUiState: MediaMainUiState,
    albumsTabUiState: AlbumsTabUiState,
    timelineTabUiState: TimelineTabUiState,
    timelineTabActionUiState: TimelineTabActionUiState,
    timelineFilterUiState: TimelineFilterUiState,
    mediaCameraUploadUiState: MediaCameraUploadUiState,
    videosTabUiState: VideosTabUiState,
    playlistsTabUiState: VideoPlaylistsTabUiState,
    selectedTimePeriod: PhotoModificationTimePeriod,
    videosTabQuery: String?,
    playlistsTabQuery: String?,
    onAllTimelinePhotosSelected: () -> Unit,
    onClearTimelinePhotosSelection: () -> Unit,
    areAllAlbumsSelected: () -> Boolean,
    onAllAlbumsSelected: () -> Unit,
    onClearAlbumsSelection: () -> Unit,
    onAllVideosSelected: () -> Unit,
    onClearVideosSelection: () -> Unit,
    onUpdateVideosSearchQuery: (value: String?) -> Unit,
    onAllPlaylistsSelected: () -> Unit,
    onClearPlaylistsSelection: () -> Unit,
    onUpdatePlaylistSearchQuery: (value: String?) -> Unit,
    removePlaylist: () -> Unit,
    onNavigateToCameraUploadsSettings: () -> Unit,
    onNavigateToCameraUploadsProgressScreen: () -> Unit,
    navigateToMediaSearch: (NavKey) -> Unit,
    onFilterActionClick: () -> Unit,
    onSortActionClick: () -> Unit,
) {
    val shouldShowTimelineActions by remember(
        selectedTimePeriod,
        mediaCameraUploadUiState.enableCameraUploadPageShowing
    ) {
        derivedStateOf {
            selectedTimePeriod == PhotoModificationTimePeriod.All && !mediaCameraUploadUiState.enableCameraUploadPageShowing
        }
    }
    val areAllTimelinePhotosSelected by remember(
        timelineTabUiState.selectedPhotoCount,
        timelineTabUiState.displayedPhotos
    ) {
        derivedStateOf { timelineTabUiState.selectedPhotoCount == timelineTabUiState.displayedPhotos.size }
    }
    val areAllAlbumsSelected by remember(
        albumsTabUiState.selectedUserAlbums,
        albumsTabUiState.albums
    ) {
        derivedStateOf { areAllAlbumsSelected() }
    }
    val areAllVideosSelected by remember(videosTabUiState) {
        derivedStateOf {
            if (videosTabUiState is VideosTabUiState.Data) {
                videosTabUiState.selectedTypedNodes.size == videosTabUiState.allVideoEntities.size
            } else false
        }
    }
    val areAllPlaylistsSelected by remember(playlistsTabUiState) {
        derivedStateOf {
            if (playlistsTabUiState is VideoPlaylistsTabUiState.Data) {
                playlistsTabUiState.selectedPlaylists.size == playlistsTabUiState.videoPlaylistEntities.size
            } else false
        }
    }
    var isVideosTabSearchBarVisible by rememberSaveable { mutableStateOf(false) }
    var isPlaylistsTabSearchBarVisible by rememberSaveable { mutableStateOf(false) }

    BackHandler(isVideosTabSearchBarVisible) {
        if (isVideosTabSearchBarVisible) {
            isVideosTabSearchBarVisible = false
            onUpdateVideosSearchQuery(null)
        }
    }

    BackHandler(isPlaylistsTabSearchBarVisible) {
        if (isPlaylistsTabSearchBarVisible) {
            isPlaylistsTabSearchBarVisible = false
            onUpdatePlaylistSearchQuery(null)
        }
    }

    when {
        selectionModeType == MediaSelectionModeType.Timeline -> {
            NodeSelectionModeAppBar(
                count = timelineTabUiState.selectedPhotoCount,
                isAllSelected = areAllTimelinePhotosSelected,
                isSelecting = false,
                onSelectAllClicked = onAllTimelinePhotosSelected,
                onCancelSelectionClicked = onClearTimelinePhotosSelection
            )
        }

        selectionModeType == MediaSelectionModeType.Albums -> {
            NodeSelectionModeAppBar(
                count = albumsTabUiState.selectedUserAlbumsCount,
                isAllSelected = areAllAlbumsSelected,
                isSelecting = false,
                onSelectAllClicked = onAllAlbumsSelected,
                onCancelSelectionClicked = onClearAlbumsSelection
            )
        }

        isVideosTabSearchBarVisible || selectionModeType == MediaSelectionModeType.Videos ->
            VideosTabToolbar(
                count = if (videosTabUiState is VideosTabUiState.Data) videosTabUiState.selectedTypedNodes.size else 0,
                isAllSelected = areAllVideosSelected,
                isSelectionMode = selectionModeType == MediaSelectionModeType.Videos,
                onSelectAllClicked = onAllVideosSelected,
                onCancelSelectionClicked = onClearVideosSelection,
                searchQuery = videosTabQuery,
                updateSearchQuery = onUpdateVideosSearchQuery,
                onSearchingModeChanged = { isSearching ->
                    if (!isSearching) {
                        isVideosTabSearchBarVisible = false
                        onUpdateVideosSearchQuery(null)
                    }
                }
            )

        isPlaylistsTabSearchBarVisible || selectionModeType == MediaSelectionModeType.Playlists -> {
            VideoPlaylistsTabAppBar(
                count = if (playlistsTabUiState is VideoPlaylistsTabUiState.Data) playlistsTabUiState.selectedPlaylists.size else 0,
                isAllSelected = areAllPlaylistsSelected,
                onSelectAllClicked = onAllPlaylistsSelected,
                onCancelSelectionClicked = onClearPlaylistsSelection,
                removePlaylist = removePlaylist,
                isSelectionMode = selectionModeType == MediaSelectionModeType.Playlists,
                searchQuery = playlistsTabQuery,
                updateSearchQuery = onUpdatePlaylistSearchQuery,
                onSearchingModeChanged = { isSearching ->
                    if (!isSearching) {
                        isPlaylistsTabSearchBarVisible = false
                        onUpdatePlaylistSearchQuery(null)
                    }
                }
            )
        }

        else -> MegaTopAppBar(
            navigationType = AppBarNavigationType.None,
            title = stringResource(SharedR.string.media_feature_title),
            subtitle = when {
                currentTabIndex == MediaScreen.Timeline.ordinal -> {
                    when (mediaCameraUploadUiState.status) {
                        is CUStatusUiState.UpToDate -> {
                            stringResource(id = SharedR.string.media_main_screen_camera_uploads_up_to_date_toolbar_subtitle)
                        }

                        is CUStatusUiState.Sync -> {
                            stringResource(id = SharedR.string.camera_uploads_banner_checking_uploads_text)
                        }

                        is CUStatusUiState.UploadInProgress -> {
                            pluralStringResource(
                                id = SharedR.plurals.camera_uploads_tranfer_top_bar_subtitle,
                                count = mediaCameraUploadUiState.status.pending,
                                mediaCameraUploadUiState.status.pending,
                            )
                        }

                        is CUStatusUiState.UploadComplete -> {
                            stringResource(id = SharedR.string.camera_uploads_banner_complete_title)
                        }

                        else -> null
                    }
                }

                else -> null
            },
            trailingIcons = {
                if (currentTabIndex == MediaScreen.Timeline.ordinal) {
                    val isFilterApplied =
                        timelineFilterUiState.mediaType != FilterMediaType.ALL_MEDIA || timelineFilterUiState.mediaSource != FilterMediaSource.AllPhotos
                    if (isFilterApplied) {
                        MegaIcon(
                            modifier = Modifier
                                .clickable(onClick = onFilterActionClick)
                                .padding(end = 24.dp),
                            imageVector = IconPack.Medium.Thin.Outline.Filter,
                            tint = IconColor.Primary
                        )
                    }

                    CameraUploadStatusToolbarAction(
                        modifier = Modifier.padding(end = 14.dp),
                        cameraUploadsStatus = mediaCameraUploadUiState.status,
                        onNavigateToCameraUploadsSettings = onNavigateToCameraUploadsSettings,
                        onNavigateToCameraUploadsProgressScreen = onNavigateToCameraUploadsProgressScreen
                    )
                }
            },
            actions = buildList {
                add(
                    MenuActionWithClick(menuAction = MediaAppBarAction.Search) {
                        when (currentTabIndex) {
                            MediaScreen.Videos.ordinal -> isVideosTabSearchBarVisible = true

                            MediaScreen.Playlists.ordinal -> isPlaylistsTabSearchBarVisible = true

                            else -> if (mediaMainUiState.isMediaRevampPhase2Enabled) {
                                navigateToMediaSearch(MediaSearchNavKey)
                            } else {
                                navigateToMediaSearch(LegacyPhotosSearchNavKey)
                            }
                        }
                    }
                )

                // Menu actions for timeline tab
                if (currentTabIndex == MediaScreen.Timeline.ordinal && shouldShowTimelineActions) {
                    add(
                        MenuActionWithClick(menuAction = MediaAppBarAction.FilterSecondary) {
                            onFilterActionClick()
                        }
                    )

                    if (timelineTabActionUiState.normalModeItem.enableSort) {
                        add(
                            MenuActionWithClick(menuAction = MediaAppBarAction.SortBy) {
                                onSortActionClick()
                            }
                        )
                    }

                    if (mediaCameraUploadUiState.status !is CUStatusUiState.Disabled) {
                        add(
                            MenuActionWithClick(
                                menuAction = MediaAppBarAction.CameraUploadsSettings,
                                onClick = onNavigateToCameraUploadsSettings
                            )
                        )
                    }
                }
            }
        )
    }
}
