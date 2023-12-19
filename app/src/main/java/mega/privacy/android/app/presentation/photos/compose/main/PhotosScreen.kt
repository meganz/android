package mega.privacy.android.app.presentation.photos.compose.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.account.CameraUploadsBusinessAlertDialog
import mega.privacy.android.app.presentation.photos.PhotoDownloaderViewModel
import mega.privacy.android.app.presentation.photos.PhotosViewComposeCoordinator
import mega.privacy.android.app.presentation.photos.PhotosViewModel
import mega.privacy.android.app.presentation.photos.albums.AlbumsViewModel
import mega.privacy.android.app.presentation.photos.albums.model.UIAlbum
import mega.privacy.android.app.presentation.photos.albums.view.AlbumsView
import mega.privacy.android.app.presentation.photos.model.PhotosTab
import mega.privacy.android.app.presentation.photos.timeline.view.EmptyState
import mega.privacy.android.app.presentation.photos.timeline.view.EnableCU
import mega.privacy.android.app.presentation.photos.timeline.view.EnableCameraUploadsScreen
import mega.privacy.android.app.presentation.photos.timeline.view.PhotosGridView
import mega.privacy.android.app.presentation.photos.timeline.view.TimelineView
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.TimelineViewModel
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.setCUUploadVideos
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.setCUUseCellularConnection
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.shouldEnableCUPage
import mega.privacy.android.app.presentation.photos.view.PhotosBodyView
import mega.privacy.android.app.presentation.photos.view.photosZoomGestureDetector
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.mobile.analytics.event.AlbumSelected
import mega.privacy.mobile.analytics.event.AlbumSelectedEvent

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotosScreen(
    /** A temporary field to support compatibility between view and compose architecture. */
    viewComposeCoordinator: PhotosViewComposeCoordinator,
    getFeatureFlagUseCase: GetFeatureFlagValueUseCase,
    photosViewModel: PhotosViewModel,
    timelineViewModel: TimelineViewModel,
    albumsViewModel: AlbumsViewModel,
    photoDownloaderViewModel: PhotoDownloaderViewModel,
    onCameraUploadsClicked: () -> Unit,
    onEnableCameraUploads: () -> Unit,
    onNavigatePhotosFilter: () -> Unit,
    onNavigateAlbumContent: (UIAlbum) -> Unit,
    onNavigateAlbumPhotosSelection: (AlbumId) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onNavigateCameraUploadsSettings: () -> Unit,
) {
    val photosViewState by photosViewModel.state.collectAsStateWithLifecycle()
    val timelineViewState by timelineViewModel.state.collectAsStateWithLifecycle()
    val albumsViewState by albumsViewModel.state.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(
        initialPage = photosViewState.selectedTab.ordinal,
        initialPageOffsetFraction = 0f
    ) {
        photosViewState.tabs.size
    }
    val timelineLazyGridState = rememberLazyGridState().also {
        viewComposeCoordinator.lazyGridState = it
    }
    val albumsLazyGridState = rememberLazyGridState()

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var isNewCUFlagReady by remember { mutableStateOf(false) }
    val isNewCUEnabled by produceState(initialValue = false) {
        value = getFeatureFlagUseCase(AppFeatures.NewCU)
        isNewCUFlagReady = true
    }

    LaunchedEffect(pagerState.currentPage) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            photosViewModel.onTabSelected(selectedTab = photosViewState.tabs[page])
            val photosTab = PhotosTab.values()[page]
            pagerState.scrollToPage(photosTab.ordinal)
            Analytics.tracker.trackEvent(photosTab.analyticsInfo)
        }
    }

    PhotosBodyView(
        tabs = photosViewState.tabs,
        selectedTab = photosViewState.selectedTab,
        pagerState = pagerState,
        onTabSelected = { tab ->
            Analytics.tracker.trackEvent(tab.analyticsInfo)
            photosViewModel.onTabSelected(selectedTab = tab)
            coroutineScope.launch {
                pagerState.scrollToPage(tab.ordinal)
            }
        },
        timelineLazyGridState = timelineLazyGridState,
        albumsLazyGridState = albumsLazyGridState,
        timelineView = {
            TimelineView(
                timelineViewState = timelineViewState,
                photoDownload = photoDownloaderViewModel::downloadPhoto,
                lazyGridState = timelineLazyGridState,
                onTextButtonClick = onCameraUploadsClicked,
                onFilterFabClick = onNavigatePhotosFilter,
                onCardClick = timelineViewModel::onCardClick,
                onTimeBarTabSelected = timelineViewModel::onTimeBarTabSelected,
                enableCUView = {
                    if (isNewCUEnabled) {
                        EnableCameraUploadsScreen(
                            onEnable = onNavigateCameraUploadsSettings,
                        )
                    } else {
                        EnableCU(
                            timelineViewState = timelineViewState,
                            onUploadVideosChanged = timelineViewModel::setCUUploadVideos,
                            onUseCellularConnectionChanged = timelineViewModel::setCUUseCellularConnection,
                            enableCUClick = onCameraUploadsClicked,
                        )
                    }
                },
                photosGridView = {
                    if (isNewCUFlagReady) {
                        PhotosGridView(
                            modifier = Modifier
                                .photosZoomGestureDetector(
                                    onZoomIn = onZoomIn,
                                    onZoomOut = onZoomOut,
                                ),
                            timelineViewState = timelineViewState,
                            downloadPhoto = photoDownloaderViewModel::downloadPhoto,
                            lazyGridState = timelineLazyGridState,
                            onClick = timelineViewModel::onClick,
                            onLongPress = timelineViewModel::onLongPress,
                            isNewCUEnabled = isNewCUEnabled,
                            onEnableCameraUploads = onNavigateCameraUploadsSettings,
                            onCloseCameraUploadsLimitedAccess = {
                                timelineViewModel.setCameraUploadsLimitedAccess(false)
                            }
                        )
                    }
                },
                emptyView = {
                    EmptyState(
                        timelineViewState = timelineViewState,
                        isNewCUEnabled = isNewCUEnabled,
                        setEnableCUPage = timelineViewModel::shouldEnableCUPage,
                        onEnableCameraUploads = onNavigateCameraUploadsSettings,
                    )
                },
                isNewCUEnabled = isNewCUEnabled,
                onClickCameraUploadsSync = { /* TODO */ },
                onClickCameraUploadsUploading ={ /* TODO */ },
                onClickCameraUploadsComplete = { /* TODO */ },
                onClickCameraUploadsWarning = { /* TODO */ },
                onChangeCameraUploadsPermissions = onNavigateCameraUploadsSettings,
                clearCameraUploadsMessage = {
                    timelineViewModel.setCameraUploadsMessage("")
                },
                clearCameraUploadsChangePermissionsMessage = {
                    timelineViewModel.showCameraUploadsChangePermissionsMessage(false)
                }
            )
        },
        albumsView = {
            AlbumsView(
                albumsViewState = albumsViewState,
                openAlbum = {
                    Analytics.tracker.trackEvent(
                        AlbumSelectedEvent(
                            selectionType = AlbumSelected.SelectionType.Single,
                            imageCount = getSelectedAlbumImageCount(it),
                            videoCount = getSelectedAlbumVideoCount(it),
                        )
                    )
                    onNavigateAlbumContent(it)
                },
                downloadPhoto = photoDownloaderViewModel::downloadPhoto,
                onDialogPositiveButtonClicked = albumsViewModel::createNewAlbum,
                setDialogInputPlaceholder = albumsViewModel::setPlaceholderAlbumTitle,
                setShowCreateAlbumDialog = albumsViewModel::setShowCreateAlbumDialog,
                setInputValidity = albumsViewModel::setNewAlbumNameValidity,
                openPhotosSelectionActivity = onNavigateAlbumPhotosSelection,
                setIsAlbumCreatedSuccessfully = albumsViewModel::setIsAlbumCreatedSuccessfully,
                allPhotos = timelineViewState.photos,
                clearAlbumDeletedMessage = { albumsViewModel.updateAlbumDeletedMessage(message = "") },
                onAlbumSelection = { album ->
                    if (album.id is Album.UserAlbum) {
                        val userAlbum = album.id
                        if (userAlbum.id in albumsViewState.selectedAlbumIds) {
                            Analytics.tracker.trackEvent(
                                AlbumSelectedEvent(
                                    selectionType = AlbumSelected.SelectionType.MultiRemove,
                                    imageCount = null,
                                    videoCount = null,
                                )
                            )
                            albumsViewModel.unselectAlbum(userAlbum)
                        } else {
                            Analytics.tracker.trackEvent(
                                AlbumSelectedEvent(
                                    selectionType = AlbumSelected.SelectionType.MultiAdd,
                                    imageCount = getSelectedAlbumImageCount(album),
                                    videoCount = getSelectedAlbumVideoCount(album),
                                )
                            )
                            albumsViewModel.selectAlbum(album.id)
                        }
                    }
                },
                closeDeleteAlbumsConfirmation = {
                    albumsViewModel.closeDeleteAlbumsConfirmation()
                    albumsViewModel.clearAlbumSelection()
                },
                deleteAlbums = { albumIds ->
                    albumsViewModel.deleteAlbums(albumIds)

                    val albums = albumsViewState.albums
                    val message = if (albumIds.size == 1) {
                        context.resources.getString(
                            R.string.photos_album_deleted_message_singular,
                            albums.find {
                                it.id is Album.UserAlbum && it.id.id == albumIds.firstOrNull()
                            }?.title?.getTitleString(context),
                        )
                    } else {
                        context.resources.getQuantityString(
                            R.plurals.photos_album_deleted_message,
                            albumIds.size,
                            albumIds.size.takeIf { it > 1 } ?: albums.find {
                                it.id is Album.UserAlbum && it.id.id == albumIds.firstOrNull()
                            }?.title?.getTitleString(context),
                        )
                    }
                    albumsViewModel.updateAlbumDeletedMessage(message)
                },
                lazyGridState = albumsLazyGridState,
                onRemoveLinkDialogConfirmClick = albumsViewModel::removeAlbumsLinks,
                onRemoveLinkDialogCancelClick = albumsViewModel::hideRemoveLinkDialog,
                resetRemovedLinksCount = albumsViewModel::resetRemovedLinksCount,
                isAlbumSharingEnabled = { getFeatureFlagUseCase(AppFeatures.AlbumSharing) },
            )
        },
        timelineViewState = timelineViewState,
        albumsViewState = albumsViewState,
    )

    CameraUploadsBusinessAlertDialog(
        show = timelineViewState.shouldShowBusinessAccountPrompt,
        onConfirm = {
            onEnableCameraUploads()
            timelineViewModel.setBusinessAccountPromptState(shouldShow = false)
        },
        onDeny = { timelineViewModel.setBusinessAccountPromptState(shouldShow = false) },
    )
}

private fun getSelectedAlbumImageCount(album: UIAlbum): Int? = if (album.id !is Album.UserAlbum) {
    null
} else {
    album.imageCount
}

private fun getSelectedAlbumVideoCount(album: UIAlbum): Int? = if (album.id !is Album.UserAlbum) {
    null
} else {
    album.videoCount
}
