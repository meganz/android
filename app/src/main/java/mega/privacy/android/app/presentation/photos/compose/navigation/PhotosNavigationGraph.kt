package mega.privacy.android.app.presentation.photos.compose.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.photos.PhotoDownloaderViewModel
import mega.privacy.android.app.presentation.photos.PhotosViewComposeCoordinator
import mega.privacy.android.app.presentation.photos.PhotosViewModel
import mega.privacy.android.app.presentation.photos.albums.AlbumsViewModel
import mega.privacy.android.feature.photos.presentation.albums.model.UIAlbum
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.TimelineViewModel
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.photos.AlbumId

@Serializable
internal object PhotosNavigationGraph

internal fun NavGraphBuilder.photosNavigationGraph(
    navHostController: NavHostController,
    viewComposeCoordinator: PhotosViewComposeCoordinator,
    photosViewModel: PhotosViewModel,
    timelineViewModel: TimelineViewModel,
    albumsViewModel: AlbumsViewModel,
    photoDownloaderViewModel: PhotoDownloaderViewModel,
    fileTypeIconMapper: FileTypeIconMapper,
    onEnableCameraUploads: () -> Unit,
    onNavigateAlbumContent: (UIAlbum) -> Unit,
    onNavigateAlbumPhotosSelection: (AlbumId) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onNavigateCameraUploadsSettings: () -> Unit,
    onChangeCameraUploadsPermissions: () -> Unit,
    onNavigateMobileDataSetting: () -> Unit,
    onNavigateUpgradeScreen: () -> Unit
) {
    navigation<PhotosNavigationGraph>(
        startDestination = PhotosScreen
    ) {
        photosScreen(
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
            fileTypeIconMapper = fileTypeIconMapper,
            onNavigateCameraUploadsTransferScreen = {
                navHostController.navigate(CameraUploadsTransferScreen)
            },
            onNavigateMobileDataSetting = onNavigateMobileDataSetting,
            onNavigateUpgradeScreen = onNavigateUpgradeScreen
        )

        cameraUploadsTransferScreen(
            timelineViewModel = timelineViewModel,
            navHostController = navHostController,
            onSettingOptionClick = onNavigateCameraUploadsSettings
        )
    }
}