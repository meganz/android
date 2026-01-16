package mega.privacy.android.feature.photos.navigation

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.photos.downloader.PhotoDownloaderViewModel
import mega.privacy.android.feature.photos.presentation.MediaMainRoute
import mega.privacy.android.feature.photos.presentation.albums.content.AlbumContentScreen
import mega.privacy.android.feature.photos.presentation.albums.content.AlbumContentViewModel
import mega.privacy.android.feature.photos.presentation.albums.coverselection.AlbumCoverSelectionScreen
import mega.privacy.android.feature.photos.presentation.albums.coverselection.AlbumCoverSelectionViewModel
import mega.privacy.android.feature.photos.presentation.albums.photosselection.AlbumPhotosSelectionScreen
import mega.privacy.android.feature.photos.presentation.albums.photosselection.AlbumPhotosSelectionViewModel
import mega.privacy.android.feature.photos.presentation.cuprogress.CameraUploadsProgressRoute
import mega.privacy.android.feature.photos.presentation.playlists.detail.VideoPlaylistDetailRoute
import mega.privacy.android.feature.photos.presentation.playlists.detail.VideoPlaylistDetailViewModel
import mega.privacy.android.feature.photos.presentation.search.MediaSearchScreenM3
import mega.privacy.android.feature.photos.presentation.search.PhotosSearchViewModel
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabViewModel
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.queue.snackbar.rememberSnackBarQueue
import mega.privacy.android.navigation.destination.AlbumContentNavKey
import mega.privacy.android.navigation.destination.AlbumCoverSelectionNavKey
import mega.privacy.android.navigation.destination.CameraUploadsProgressNavKey
import mega.privacy.android.navigation.destination.LegacyAddToAlbumActivityNavKey
import mega.privacy.android.navigation.destination.LegacyAlbumCoverSelectionNavKey
import mega.privacy.android.navigation.destination.LegacyImagePreviewNavKey
import mega.privacy.android.navigation.destination.LegacyPhotosSearchNavKey
import mega.privacy.android.navigation.destination.MediaMainNavKey
import mega.privacy.android.navigation.destination.MediaSearchNavKey
import mega.privacy.android.navigation.destination.PhotosSelectionNavKey
import mega.privacy.android.navigation.destination.VideoPlaylistDetailNavKey
import mega.privacy.android.shared.resources.R as sharedR

fun EntryProviderScope<NavKey>.mediaMainRoute(
    navigationHandler: NavigationHandler,
    setNavigationItemVisibility: (Boolean) -> Unit,
    photoSelectionResultFlow: (String) -> Flow<Long?>,
    timelineAddToAlbumResultFlow: (String) -> Flow<String?>,
    mediaAlbumNavigationFlow: (String) -> Flow<Pair<Long?, String>?>,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    entry<MediaMainNavKey> {
        val snackBarEventQueue = rememberSnackBarQueue()
        val photoSelectionResult by photoSelectionResultFlow(PhotosSelectionNavKey.RESULT)
            .collectAsStateWithLifecycle(null)
        val timelineAddToAlbumResult by timelineAddToAlbumResultFlow(LegacyAddToAlbumActivityNavKey.ADD_TO_ALBUM_RESULT)
            .collectAsStateWithLifecycle(null)
        val mediaAlbumNavigationFlow by mediaAlbumNavigationFlow(LegacyPhotosSearchNavKey.RESULT)
            .collectAsStateWithLifecycle(null)
        val nodeOptionsActionViewModel = hiltViewModel<NodeOptionsActionViewModel>()

        HandleNodeOptionsActionResult(
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            onNavigate = navigationHandler::navigate,
            onTransfer = onTransfer,
            nodeResultFlow = navigationHandler::monitorResult,
            clearResultFlow = navigationHandler::clearResult,
        )

        LaunchedEffect(photoSelectionResult) {
            if (photoSelectionResult != null) {
                navigationHandler.navigate(
                    AlbumContentNavKey(
                        id = photoSelectionResult,
                        type = "custom"
                    )
                )

                navigationHandler.clearResult(PhotosSelectionNavKey.RESULT)
            }
        }

        LaunchedEffect(timelineAddToAlbumResult) {
            timelineAddToAlbumResult?.let {
                snackBarEventQueue.queueMessage(it)
                navigationHandler.clearResult(LegacyAddToAlbumActivityNavKey.ADD_TO_ALBUM_RESULT)
            }
        }

        LaunchedEffect(mediaAlbumNavigationFlow) {
            mediaAlbumNavigationFlow?.let { (id, type) ->
                navigationHandler.navigate(AlbumContentNavKey(id = id, type = type))
                navigationHandler.clearResult(LegacyPhotosSearchNavKey.RESULT)
            }
        }


        val activity = LocalActivity.current as ComponentActivity
        val timelineViewModel: TimelineTabViewModel = hiltViewModel(activity)
        MediaMainRoute(
            timelineViewModel = timelineViewModel,
            navigationHandler = navigationHandler,
            setNavigationItemVisibility = setNavigationItemVisibility,
            onNavigateToTimelinePhotoPreview = navigationHandler::navigate,
            onNavigateToAddToAlbum = navigationHandler::navigate,
            onNavigateToCameraUploadsSettings = navigationHandler::navigate,
            onNavigateToUpgradeAccount = navigationHandler::navigate,
            onNavigateToCameraUploadsProgressScreen = {
                navigationHandler.navigate(destination = CameraUploadsProgressNavKey)
            },
        )
    }
}

fun EntryProviderScope<NavKey>.albumContentScreen(
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
    resultFlow: (String) -> Flow<String?>,
) {
    entry<AlbumContentNavKey> { args ->
        val albumCoverSelectionResult by resultFlow(LegacyAlbumCoverSelectionNavKey.MESSAGE)
            .collectAsStateWithLifecycle(null)
        val viewModel = hiltViewModel<AlbumContentViewModel, AlbumContentViewModel.Factory>(
            creationCallback = { it.create(args) }
        )
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = LocalSnackBarHostState.current

        LaunchedEffect(albumCoverSelectionResult) {
            coroutineScope.launch {
                albumCoverSelectionResult?.let {
                    snackbarHostState?.showSnackbar(it)
                    navigationHandler.clearResult(LegacyAlbumCoverSelectionNavKey.MESSAGE)
                }
            }
        }

        AlbumContentScreen(
            navigationHandler = navigationHandler,
            onTransfer = onTransfer,
            viewModel = viewModel,
        )
    }
}

fun EntryProviderScope<NavKey>.videoPlaylistDetailScreen(
    navigationHandler: NavigationHandler,
) {
    entry<VideoPlaylistDetailNavKey> { args ->
        val viewModel =
            hiltViewModel<VideoPlaylistDetailViewModel, VideoPlaylistDetailViewModel.Factory> { factory ->
                factory.create(args.playlistHandle, args.type)
            }

        VideoPlaylistDetailRoute(navigationHandler, viewModel)
    }
}

fun EntryProviderScope<NavKey>.mediaSearchScreen(
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    entry<MediaSearchNavKey> { args ->
        val photoDownloaderViewModel: PhotoDownloaderViewModel = hiltViewModel()
        val photosSearchViewModel: PhotosSearchViewModel = hiltViewModel()
        val nodeOptionsActionViewModel: NodeOptionsActionViewModel = hiltViewModel()
        val state by photosSearchViewModel.state.collectAsStateWithLifecycle()

        HandleNodeOptionsActionResult(
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            onNavigate = navigationHandler::navigate,
            onTransfer = onTransfer,
            nodeResultFlow = navigationHandler::monitorResult,
            clearResultFlow = navigationHandler::clearResult,
        )

        MediaSearchScreenM3(
            state = state,
            photoDownloaderViewModel = photoDownloaderViewModel,
            onOpenAlbum = navigationHandler::navigate,
            onOpenImagePreviewScreen = { photo ->
                navigationHandler.navigate(
                    LegacyImagePreviewNavKey(
                        imageIds = state.photos.map { it.id }.toSet(),
                        anchorImageId = photo.id
                    )
                )
            },
            onShowMoreMenu = { nodeId ->
                navigationHandler.navigate(
                    NodeOptionsBottomSheetNavKey(
                        nodeHandle = nodeId.longValue,
                        nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                        partiallyExpand = true
                    )
                )
            },
            updateQuery = photosSearchViewModel::updateQuery,
            updateSelectedQuery = {
                photosSearchViewModel.updateSelectedQuery(null)
            },
            updateRecentQueries = photosSearchViewModel::updateRecentQueries,
            searchPhotos = photosSearchViewModel::search,
            onCloseScreen = { navigationHandler.remove(args) },
        )
    }
}

fun EntryProviderScope<NavKey>.albumCoverSelectionScreen(
    navigationHandler: NavigationHandler,
) {
    entry<AlbumCoverSelectionNavKey> { args ->
        val snackbarEventQueue = rememberSnackBarQueue()
        val coroutineScope = rememberCoroutineScope()
        val viewModel =
            hiltViewModel<AlbumCoverSelectionViewModel, AlbumCoverSelectionViewModel.Factory>(
                creationCallback = { it.create(args.albumId) }
            )

        AlbumCoverSelectionScreen(
            viewModel = viewModel,
            onBackClicked = navigationHandler::back,
            onCompletion = { message ->
                coroutineScope.launch {
                    snackbarEventQueue.queueMessage(message)
                    navigationHandler.remove(args)
                }
            },
        )
    }
}

fun EntryProviderScope<NavKey>.albumPhotosSelectionScreen(
    navigationHandler: NavigationHandler,
) {
    entry<PhotosSelectionNavKey> { args ->
        val snackbarEventQueue = rememberSnackBarQueue()
        val coroutineScope = rememberCoroutineScope()
        val resources = LocalResources.current
        val viewModel =
            hiltViewModel<AlbumPhotosSelectionViewModel, AlbumPhotosSelectionViewModel.Factory>(
                creationCallback = { it.create(args.albumId, args.selectionMode) }
            )

        AlbumPhotosSelectionScreen(
            viewModel = viewModel,
            onBackClicked = navigationHandler::back,
            onCompletion = { album, numCommittedPhotos ->
                if (numCommittedPhotos > 0) {
                    coroutineScope.launch {
                        snackbarEventQueue.queueMessage(
                            resources.getQuantityString(
                                sharedR.plurals.album_photos_selection_success_message,
                                numCommittedPhotos,
                                numCommittedPhotos,
                                album.title
                            )
                        )
                    }
                }

                if (!args.captureResult) {
                    navigationHandler.back()
                    return@AlbumPhotosSelectionScreen
                }

                val result = album.id.id.takeIf { numCommittedPhotos > 0 }
                navigationHandler.returnResult(PhotosSelectionNavKey.RESULT, result)
            },
        )
    }
}

fun EntryProviderScope<NavKey>.cameraUploadsProgressRoute(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    entry<CameraUploadsProgressNavKey> { args ->
        CameraUploadsProgressRoute(
            modifier = modifier,
            onNavigateUp = onNavigateUp
        )
    }
}
