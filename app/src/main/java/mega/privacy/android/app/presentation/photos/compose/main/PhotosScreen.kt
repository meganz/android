package mega.privacy.android.app.presentation.photos.compose.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
) {
    val photosViewState by photosViewModel.state.collectAsStateWithLifecycle()
    val timelineViewState by timelineViewModel.state.collectAsStateWithLifecycle()
    val albumsViewState by albumsViewModel.state.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(initialPage = photosViewState.selectedTab.ordinal)
    val timelineLazyGridState = rememberLazyGridState().also {
        viewComposeCoordinator.lazyGridState = it
    }
    val albumsLazyGridState = rememberLazyGridState()

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

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
                onFABClick = onNavigatePhotosFilter,
                onCardClick = timelineViewModel::onCardClick,
                onTimeBarTabSelected = timelineViewModel::onTimeBarTabSelected,
                enableCUView = {
                    EnableCU(
                        timelineViewState = timelineViewState,
                        onUploadVideosChanged = timelineViewModel::setCUUploadVideos,
                        onUseCellularConnectionChanged = timelineViewModel::setCUUseCellularConnection,
                        enableCUClick = onCameraUploadsClicked,
                    )
                },
                photosGridView = {
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
                    )
                },
                emptyView = {
                    EmptyState(
                        timelineViewState = timelineViewState,
                        onFABClick = onNavigatePhotosFilter,
                        setEnableCUPage = timelineViewModel::shouldEnableCUPage,
                    )
                }
            )
        },
        albumsView = {
            AlbumsView(
                albumsViewState = albumsViewState,
                openAlbum = {
                    Analytics.tracker.trackEvent(
                        AlbumSelectedEvent(selectionType = AlbumSelected.SelectionType.Single)
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
                    if (album.id in albumsViewState.selectedAlbumIds) {
                        Analytics.tracker.trackEvent(
                            AlbumSelectedEvent(selectionType = AlbumSelected.SelectionType.MultiRemove)
                        )
                        albumsViewModel.unselectAlbum(album)
                    } else {
                        Analytics.tracker.trackEvent(
                            AlbumSelectedEvent(selectionType = AlbumSelected.SelectionType.MultiAdd)
                        )
                        albumsViewModel.selectAlbum(album)
                    }
                },
                closeDeleteAlbumsConfirmation = {
                    albumsViewModel.closeDeleteAlbumsConfirmation()
                    albumsViewModel.clearAlbumSelection()
                },
                deleteAlbums = { albumIds ->
                    albumsViewModel.deleteAlbums(albumIds)

                    val albums = albumsViewState.albums
                    val message = context.resources.getQuantityString(
                        R.plurals.photos_album_deleted_message,
                        albumIds.size,
                        albumIds.size.takeIf { it > 1 } ?: albums.find {
                            it.id is Album.UserAlbum && it.id.id == albumIds.firstOrNull()
                        }?.title?.getTitleString(context),
                    )
                    albumsViewModel.updateAlbumDeletedMessage(message)
                },
                lazyGridState = albumsLazyGridState,
                onRemoveLinkDialogConfirmClick = albumsViewModel::removeAlbumsLinks,
                onRemoveLinkDialogCancelClick = albumsViewModel::hideRemoveLinkDialog,
                resetRemovedLinksCount = albumsViewModel::resetRemovedLinksCount,
                isUserAlbumsEnabled = { getFeatureFlagUseCase(AppFeatures.UserAlbums) }
            ) {
                getFeatureFlagUseCase(AppFeatures.AlbumSharing)
            }
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
