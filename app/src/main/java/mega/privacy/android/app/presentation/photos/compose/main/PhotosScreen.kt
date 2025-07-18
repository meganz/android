package mega.privacy.android.app.presentation.photos.compose.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.getStorageState
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewActivity
import mega.privacy.android.app.presentation.imagepreview.fetcher.TimelineImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.photos.PhotoDownloaderViewModel
import mega.privacy.android.app.presentation.photos.PhotosViewComposeCoordinator
import mega.privacy.android.app.presentation.photos.PhotosViewModel
import mega.privacy.android.app.presentation.photos.albums.AlbumsViewModel
import mega.privacy.android.app.presentation.photos.albums.model.UIAlbum
import mega.privacy.android.app.presentation.photos.albums.view.AlbumsView
import mega.privacy.android.app.presentation.photos.model.PhotosTab
import mega.privacy.android.app.presentation.photos.timeline.view.EmptyState
import mega.privacy.android.app.presentation.photos.timeline.view.EnableCameraUploadsScreen
import mega.privacy.android.app.presentation.photos.timeline.view.PhotosGridView
import mega.privacy.android.app.presentation.photos.timeline.view.TimelineView
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.TimelineViewModel
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.getCurrentSort
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.getFilterType
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.getMediaSource
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.shouldEnableCUPage
import mega.privacy.android.app.presentation.photos.view.PhotosBodyView
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.CameraUploadsBusinessAccountDialog
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.mobile.analytics.event.AlbumSelected
import mega.privacy.mobile.analytics.event.AlbumSelectedEvent

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotosScreen(
    /** A temporary field to support compatibility between view and compose architecture. */
    viewComposeCoordinator: PhotosViewComposeCoordinator,
    photosViewModel: PhotosViewModel,
    timelineViewModel: TimelineViewModel,
    albumsViewModel: AlbumsViewModel,
    photoDownloaderViewModel: PhotoDownloaderViewModel,
    onEnableCameraUploads: () -> Unit,
    onNavigateAlbumContent: (UIAlbum) -> Unit,
    onNavigateAlbumPhotosSelection: (AlbumId) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onNavigateCameraUploadsSettings: () -> Unit,
    onChangeCameraUploadsPermissions: () -> Unit,
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
    val onPhotoClick = remember {
        { photo: Photo ->
            if (timelineViewModel.getSelectedPhotosCount() == 0) {
                val intent = ImagePreviewActivity.createIntent(
                    context = context,
                    imageSource = ImagePreviewFetcherSource.TIMELINE,
                    menuOptionsSource = ImagePreviewMenuSource.TIMELINE,
                    anchorImageNodeId = NodeId(photo.id),
                    params = mapOf(
                        TimelineImageNodeFetcher.TIMELINE_SORT_TYPE to timelineViewModel.getCurrentSort(),
                        TimelineImageNodeFetcher.TIMELINE_FILTER_TYPE to timelineViewModel.getFilterType(),
                        TimelineImageNodeFetcher.TIMELINE_MEDIA_SOURCE to timelineViewModel.getMediaSource(),
                    ),
                    enableAddToAlbum = true,
                )
                context.startActivity(intent)
            } else {
                timelineViewModel.onLongPress(photo)
            }
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            photosViewModel.onTabSelected(selectedTab = photosViewState.tabs[page])
            val photosTab = PhotosTab.entries[page]
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
                onCardClick = timelineViewModel::onCardClick,
                onTimeBarTabSelected = timelineViewModel::onTimeBarTabSelected,
                enableCUView = {
                    EnableCameraUploadsScreen(
                        onEnable = onNavigateCameraUploadsSettings,
                    )
                },
                photosGridView = {
                    PhotosGridView(
                        timelineViewState = timelineViewState,
                        downloadPhoto = photoDownloaderViewModel::downloadPhoto,
                        lazyGridState = timelineLazyGridState,
                        onClick = onPhotoClick,
                        onLongPress = timelineViewModel::onLongPress,
                        onEnableCameraUploads = onNavigateCameraUploadsSettings,
                        onChangeCameraUploadsPermissions = onChangeCameraUploadsPermissions,
                        onCloseCameraUploadsLimitedAccess = {
                            timelineViewModel.setCameraUploadsLimitedAccess(false)
                        },
                        onZoomIn = onZoomIn,
                        onZoomOut = onZoomOut,
                    )
                },
                emptyView = {
                    EmptyState(
                        timelineViewState = timelineViewState,
                        setEnableCUPage = timelineViewModel::shouldEnableCUPage,
                        onEnableCameraUploads = onNavigateCameraUploadsSettings,
                    )
                },
                onClickCameraUploadsSync = { /* TODO */ },
                onClickCameraUploadsUploading = {
                    timelineViewModel.setCameraUploadsMessage(
                        message = context.resources.getQuantityString(
                            R.plurals.photos_camera_uploads_pending,
                            timelineViewState.pending,
                            timelineViewState.pending,
                        )
                    )
                },
                onChangeCameraUploadsPermissions = onChangeCameraUploadsPermissions,
                onCloseCameraUploadsLimitedAccess = {
                    timelineViewModel.setCameraUploadsLimitedAccess(false)
                },
                clearCameraUploadsMessage = {
                    timelineViewModel.setCameraUploadsMessage("")
                },
                clearCameraUploadsChangePermissionsMessage = {
                    timelineViewModel.showCameraUploadsChangePermissionsMessage(false)
                },
                clearCameraUploadsCompletedMessage = {
                    timelineViewModel.setCameraUploadsCompletedMessage(false)
                },
                loadPhotos = timelineViewModel::loadPhotos
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
                isStorageExceeded = { getStorageState() == StorageState.PayWall },
            )
        },
        timelineViewState = timelineViewState,
        albumsViewState = albumsViewState,
    )

    if (timelineViewState.shouldShowBusinessAccountPrompt) {
        CameraUploadsBusinessAccountDialog(
            onAlertAcknowledged = {
                onEnableCameraUploads()
                timelineViewModel.setBusinessAccountPromptState(shouldShow = false)
            },
            onAlertDismissed = {
                timelineViewModel.setBusinessAccountPromptState(shouldShow = false)
            },
        )
    }
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
