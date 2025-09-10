package mega.privacy.android.app.presentation.photos.compose.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.photos.PhotoDownloaderViewModel
import mega.privacy.android.app.presentation.photos.PhotosViewComposeCoordinator
import mega.privacy.android.app.presentation.photos.PhotosViewModel
import mega.privacy.android.app.presentation.photos.albums.AlbumsViewModel
import mega.privacy.android.app.presentation.photos.albums.model.UIAlbum
import mega.privacy.android.app.presentation.photos.compose.main.PhotosScreen
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.TimelineViewModel
import mega.privacy.android.domain.entity.photos.AlbumId

@Serializable
internal object PhotosScreen

internal fun NavGraphBuilder.photosScreen(
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
    composable<PhotosScreen> {
        PhotosScreen(
            viewComposeCoordinator = viewComposeCoordinator,
            photosViewModel = photosViewModel,
            timelineViewModel = timelineViewModel,
            albumsViewModel = albumsViewModel,
            photoDownloaderViewModel = photoDownloaderViewModel,
            onEnableCameraUploads = onEnableCameraUploads,
            onNavigateAlbumContent = onNavigateAlbumContent,
            onNavigateAlbumPhotosSelection = onNavigateAlbumPhotosSelection,
            onZoomIn = onZoomIn,
            onZoomOut = onZoomOut,
            onNavigateCameraUploadsSettings = onNavigateCameraUploadsSettings,
            onChangeCameraUploadsPermissions = onChangeCameraUploadsPermissions,
        )
    }
}