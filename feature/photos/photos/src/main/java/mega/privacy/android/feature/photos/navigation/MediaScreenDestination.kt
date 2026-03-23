package mega.privacy.android.feature.photos.navigation

import android.content.Intent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.entity.videosection.PlaylistType
import mega.privacy.android.feature.photos.downloader.PhotoDownloaderViewModel
import mega.privacy.android.feature.photos.presentation.MediaMainRoute
import mega.privacy.android.feature.photos.presentation.albums.content.AlbumContentScreen
import mega.privacy.android.feature.photos.presentation.albums.content.AlbumContentViewModel
import mega.privacy.android.feature.photos.presentation.albums.coverselection.AlbumCoverSelectionScreen
import mega.privacy.android.feature.photos.presentation.albums.coverselection.AlbumCoverSelectionViewModel
import mega.privacy.android.feature.photos.presentation.albums.decryptionkey.AlbumDecryptionKeyScreen
import mega.privacy.android.feature.photos.presentation.albums.getlink.AlbumGetLinkScreen
import mega.privacy.android.feature.photos.presentation.albums.getlink.AlbumGetLinkViewModel
import mega.privacy.android.feature.photos.presentation.albums.getmultiplelinks.AlbumGetMultipleLinksScreen
import mega.privacy.android.feature.photos.presentation.albums.getmultiplelinks.AlbumGetMultipleLinksViewModel
import mega.privacy.android.feature.photos.presentation.albums.importlink.AlbumImportScreen
import mega.privacy.android.feature.photos.presentation.albums.importlink.AlbumImportViewModel
import mega.privacy.android.feature.photos.presentation.albums.photosselection.AlbumPhotosSelectionScreen
import mega.privacy.android.feature.photos.presentation.albums.photosselection.AlbumPhotosSelectionViewModel
import mega.privacy.android.feature.photos.presentation.cuprogress.CameraUploadsProgressRoute
import mega.privacy.android.feature.photos.presentation.mediadiscovery.CloudDriveMediaDiscoveryRoute
import mega.privacy.android.feature.photos.presentation.mediadiscovery.CloudDriveMediaDiscoveryViewModel
import mega.privacy.android.feature.photos.presentation.playlists.detail.VideoPlaylistDetailRoute
import mega.privacy.android.feature.photos.presentation.playlists.detail.VideoPlaylistDetailViewModel
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.SelectVideosForPlaylistRoute
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.SelectVideosForPlaylistViewModel
import mega.privacy.android.feature.photos.presentation.search.MediaSearchScreenM3
import mega.privacy.android.feature.photos.presentation.search.PhotosSearchViewModel
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabViewModel
import mega.privacy.android.feature.photos.presentation.videos.VideoRecentlyWatchedRoute
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.queue.snackbar.rememberSnackBarQueue
import mega.privacy.android.navigation.contract.shared.sharedViewModel
import mega.privacy.android.navigation.destination.AlbumContentNavKey
import mega.privacy.android.navigation.destination.AlbumCoverSelectionNavKey
import mega.privacy.android.navigation.destination.AlbumDecryptionKeyNavKey
import mega.privacy.android.navigation.destination.AlbumGetLinkNavKey
import mega.privacy.android.navigation.destination.AlbumGetMultipleLinksNavKey
import mega.privacy.android.navigation.destination.AlbumImportNavKey
import mega.privacy.android.navigation.destination.CameraUploadsProgressNavKey
import mega.privacy.android.navigation.destination.CloudDriveMediaDiscoveryNavKey
import mega.privacy.android.navigation.destination.DriveSyncNavKey
import mega.privacy.android.navigation.destination.FileExplorerNavKey
import mega.privacy.android.navigation.destination.LegacyAlbumCoverSelectionNavKey
import mega.privacy.android.navigation.destination.LegacyImageViewerNavKey
import mega.privacy.android.navigation.destination.MediaMainNavKey
import mega.privacy.android.navigation.destination.MediaSearchNavKey
import mega.privacy.android.navigation.destination.OverDiskQuotaPaywallWarningNavKey
import mega.privacy.android.navigation.destination.PhotosSelectionNavKey
import mega.privacy.android.navigation.destination.SelectVideosForPlaylistNavKey
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey
import mega.privacy.android.navigation.destination.VideoPlaylistDetailNavKey
import mega.privacy.android.navigation.destination.VideoRecentlyWatchedNavKey
import mega.privacy.android.shared.resources.R as sharedR

fun EntryProviderScope<NavKey>.mediaMainRoute(
    navigationHandler: NavigationHandler,
    setNavigationItemVisibility: (Boolean) -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    entry<MediaMainNavKey> {
        val photoSelectionResult by navigationHandler.monitorResult<Long?>(PhotosSelectionNavKey.RESULT)
            .collectAsStateWithLifecycle(null)
        val nodeOptionsActionViewModel =
            hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
                creationCallback = { it.create(null) }
            )

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

        val timelineViewModel: TimelineTabViewModel = sharedViewModel()
        MediaMainRoute(
            timelineViewModel = timelineViewModel,
            navigationHandler = navigationHandler,
            setNavigationItemVisibility = setNavigationItemVisibility,
            onNavigateToTimelinePhotoPreview = navigationHandler::navigate,
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
    onTransfer: (TransferTriggerEvent) -> Unit,
    resultFlow: (String) -> Flow<Int?>,
) {
    entry<VideoPlaylistDetailNavKey> { key ->
        val numberOfAddedVideos by resultFlow(SelectVideosForPlaylistNavKey.RESULT)
            .collectAsStateWithLifecycle(null)
        val viewModel =
            hiltViewModel<VideoPlaylistDetailViewModel, VideoPlaylistDetailViewModel.Factory> { factory ->
                factory.create(
                    VideoPlaylistDetailViewModel.Args(
                        playlistHandle = key.playlistHandle,
                        type = key.type,
                    )
                )
            }

        val nodeOptionsActionViewModel: NodeOptionsActionViewModel =
            hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
                creationCallback = { it.create(null) }
            )
        HandleNodeOptionsActionResult(
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            onNavigate = navigationHandler::navigate,
            onTransfer = onTransfer,
            nodeResultFlow = navigationHandler::monitorResult,
            clearResultFlow = navigationHandler::clearResult,
        )

        VideoPlaylistDetailRoute(
            numberOfAddedVideos = numberOfAddedVideos,
            clearResult = navigationHandler::clearResult,
            navigate = navigationHandler::navigate,
            onBack = navigationHandler::back,
            viewModel = viewModel
        )
    }
}

fun EntryProviderScope<NavKey>.mediaSearchScreen(
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    entry<MediaSearchNavKey> { args ->
        val photoDownloaderViewModel: PhotoDownloaderViewModel = hiltViewModel()
        val photosSearchViewModel: PhotosSearchViewModel = hiltViewModel()
        val nodeOptionsActionViewModel: NodeOptionsActionViewModel =
            hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
                creationCallback = { it.create(null) }
            )
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
                    LegacyImageViewerNavKey(
                        nodeHandle = photo.id,
                        parentNodeHandle = -1L,
                        nodeIds = state.photos.map { it.id },
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

fun EntryProviderScope<NavKey>.albumDecryptionKey(
    navigationHandler: NavigationHandler,
) {
    entry<AlbumDecryptionKeyNavKey> {
        AlbumDecryptionKeyScreen(
            modifier = Modifier.fillMaxSize(),
            onBack = navigationHandler::back
        )
    }
}

fun EntryProviderScope<NavKey>.cameraUploadsProgressRoute(
    onNavigateUp: () -> Unit,
    onNavigateToCameraUploadsSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    entry<CameraUploadsProgressNavKey> { args ->
        CameraUploadsProgressRoute(
            modifier = modifier,
            onNavigateUp = onNavigateUp,
            onNavigateToCameraUploadsSettings = onNavigateToCameraUploadsSettings
        )
    }
}

fun EntryProviderScope<NavKey>.albumGetLink(
    navigationHandler: NavigationHandler,
) {
    entry<AlbumGetLinkNavKey> { args ->
        val context = LocalContext.current
        val albumGetLinkViewModel =
            hiltViewModel<AlbumGetLinkViewModel, AlbumGetLinkViewModel.Factory> {
                it.create(args.albumId)
            }
        AlbumGetLinkScreen(
            albumGetLinkViewModel = albumGetLinkViewModel,
            onBack = navigationHandler::back,
            onLearnMore = {
                navigationHandler.navigate(AlbumDecryptionKeyNavKey)
            },
            onShareLink = { album, link ->
                with(context) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, album?.title.orEmpty())
                        putExtra(Intent.EXTRA_TEXT, link)
                    }
                    val shareIntent = Intent.createChooser(
                        intent,
                        getString(sharedR.string.general_share)
                    )
                    startActivity(shareIntent)
                }
            },
        )
    }
}

fun EntryProviderScope<NavKey>.albumGetMultipleLinks(
    navigationHandler: NavigationHandler,
) {
    entry<AlbumGetMultipleLinksNavKey> { args ->
        val context = LocalContext.current
        val albumGetMultipleLinksViewModel =
            hiltViewModel<AlbumGetMultipleLinksViewModel, AlbumGetMultipleLinksViewModel.Factory> {
                it.create(albumIds = args.albumIds.toLongArray())
            }
        AlbumGetMultipleLinksScreen(
            viewModel = albumGetMultipleLinksViewModel,
            onBack = navigationHandler::back,
            onShareLinks = { links ->
                with(context) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        val linksText = links.joinToString(System.lineSeparator()) { it.link }
                        putExtra(Intent.EXTRA_TEXT, linksText)
                    }
                    val shareIntent = Intent.createChooser(
                        intent,
                        getString(sharedR.string.general_share)
                    )
                    startActivity(shareIntent)
                }
            },
        )
    }
}

fun EntryProviderScope<NavKey>.selectVideosForPlaylistScreen(
    navigationHandler: NavigationHandler,
) {
    entry<SelectVideosForPlaylistNavKey> { key ->
        val viewModel =
            hiltViewModel<SelectVideosForPlaylistViewModel, SelectVideosForPlaylistViewModel.Factory> {
                it.create(
                    SelectVideosForPlaylistViewModel.Args(
                        nodeHandle = key.nodeHandle,
                        nodeName = key.nodeName,
                        playlistHandle = key.playlistHandle,
                        isNewlyCreated = key.isNewlyCreated
                    )
                )
            }
        SelectVideosForPlaylistRoute(
            isNewlyCreated = key.isNewlyCreated,
            playlistHandle = key.playlistHandle,
            onNavigateToFolder = navigationHandler::navigate,
            returnResult = { key, numberOfAddedVideos ->
                navigationHandler.returnResult(key, numberOfAddedVideos)
            },
            onBack = navigationHandler::back,
            navigateAndClearTo = {
                navigationHandler.navigateAndClearTo(
                    destination = VideoPlaylistDetailNavKey(
                        playlistHandle = key.playlistHandle,
                        type = PlaylistType.User
                    ),
                    newParent = SelectVideosForPlaylistNavKey(
                        playlistHandle = key.playlistHandle,
                        isNewlyCreated = key.isNewlyCreated
                    ),
                    inclusive = true
                )
            },
            viewModel = viewModel
        )
    }
}

fun EntryProviderScope<NavKey>.albumImports(
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    entry<AlbumImportNavKey> { args ->
        val fileExplorerResultFlow by navigationHandler
            .monitorResult<Long?>(FileExplorerNavKey.RESULT_FOLDER_HANDLE)
            .collectAsStateWithLifecycle(null)
        val context = LocalContext.current
        val albumImportViewModel: AlbumImportViewModel =
            hiltViewModel<AlbumImportViewModel, AlbumImportViewModel.Factory> {
                it.create(args.link)
            }
        val uiState by albumImportViewModel.stateFlow.collectAsStateWithLifecycle()

        LaunchedEffect(fileExplorerResultFlow) {
            fileExplorerResultFlow?.let {
                albumImportViewModel.importAlbum(NodeId(it))
            }
        }

        EventEffect(
            event = uiState.addToCloudDriveFinishedEvent,
            onConsumed = albumImportViewModel::resetAlbumSelectionFinishedEvent,
            action = {
                navigationHandler.clearResult(FileExplorerNavKey.RESULT_FOLDER_HANDLE)
                navigationHandler.navigateAndClearBackStack(DriveSyncNavKey())
            }
        )

        AlbumImportScreen(
            albumImportViewModel = albumImportViewModel,
            onShareLink = { link ->
                if (uiState.storageState == StorageState.PayWall) {
                    navigationHandler.navigate(OverDiskQuotaPaywallWarningNavKey)
                } else {
                    with(context) {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, link)
                        }
                        val shareIntent = Intent.createChooser(
                            intent,
                            getString(sharedR.string.general_share)
                        )
                        startActivity(shareIntent)
                    }
                }
            },
            onPreviewPhoto = {
                navigationHandler.navigate(
                    LegacyImageViewerNavKey(
                        nodeHandle = it.id,
                        parentNodeHandle = -1L,
                        nodeIds = listOf(it.id)
                    )
                )
            },
            onNavigateFileExplorer = {
                navigationHandler.navigate(
                    FileExplorerNavKey(action = "ACTION_IMPORT_ALBUM")
                )
            },
            onUpgradeAccount = {
                navigationHandler.navigate(UpgradeAccountNavKey())
            },
            onBack = navigationHandler::back,
            onTransfer = onTransfer,
            showOverDiskQuotaPaywallWarning = {
                navigationHandler.navigate(OverDiskQuotaPaywallWarningNavKey)
            }
        )
    }
}

fun EntryProviderScope<NavKey>.videoRecentlyWatchedScreen(
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    entry<VideoRecentlyWatchedNavKey> {
        val nodeOptionsActionViewModel: NodeOptionsActionViewModel =
            hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
                creationCallback = { it.create(null) }
            )

        HandleNodeOptionsActionResult(
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            onNavigate = navigationHandler::navigate,
            onTransfer = onTransfer,
            nodeResultFlow = navigationHandler::monitorResult,
            clearResultFlow = navigationHandler::clearResult,
        )
        VideoRecentlyWatchedRoute(
            onBack = navigationHandler::back,
            navigate = navigationHandler::navigate
        )
    }
}

/**
 * Entry for Cloud Drive Media Discovery Screen
 * @param navigationHandler Navigation handler to handle navigation actions
 * @param onTransfer Callback to handle transfer events
 */
fun EntryProviderScope<NavKey>.cloudDriveMediaDiscoveryScreen(
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    entry<CloudDriveMediaDiscoveryNavKey>(
        metadata = NavDisplay.transitionSpec {
            EnterTransition.None togetherWith ExitTransition.None
        }
    ) { key ->
        val viewModel =
            hiltViewModel<CloudDriveMediaDiscoveryViewModel, CloudDriveMediaDiscoveryViewModel.Factory>(
                creationCallback = {
                    it.create(
                        folderId = key.folderId,
                        folderName = key.folderName,
                        fromFolderLink = key.fromFolderLink,
                        nodeSourceType = key.nodeSourceType
                    )
                }
            )

        val nodeOptionsActionViewModel =
            hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
                creationCallback = { it.create(null) }
            )

        HandleNodeOptionsActionResult(
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            onNavigate = navigationHandler::navigate,
            onTransfer = onTransfer,
            nodeResultFlow = navigationHandler::monitorResult,
            clearResultFlow = navigationHandler::clearResult,
        )

        CloudDriveMediaDiscoveryRoute(
            viewModel = viewModel,
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            navigationHandler = navigationHandler,
            onTransfer = onTransfer,
            onMoreOptionsClicked = {
                if (key.folderId != -1L) {
                    navigationHandler.navigate(
                        NodeOptionsBottomSheetNavKey(
                            nodeHandle = key.folderId,
                            nodeSourceType = key.nodeSourceType,
                        )
                    )
                }
            },
        )
    }
}