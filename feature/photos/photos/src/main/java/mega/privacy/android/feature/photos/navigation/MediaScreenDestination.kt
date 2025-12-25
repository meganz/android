package mega.privacy.android.feature.photos.navigation

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.photos.presentation.MediaMainRoute
import mega.privacy.android.feature.photos.presentation.albums.content.AlbumContentScreen
import mega.privacy.android.feature.photos.presentation.albums.content.AlbumContentViewModel
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabViewModel
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.queue.snackbar.rememberSnackBarQueue
import mega.privacy.android.navigation.destination.AlbumContentNavKey
import mega.privacy.android.navigation.destination.LegacyAddToAlbumActivityNavKey
import mega.privacy.android.navigation.destination.LegacyAlbumCoverSelectionNavKey
import mega.privacy.android.navigation.destination.LegacyPhotoSelectionNavKey
import mega.privacy.android.navigation.destination.LegacyPhotosSearchNavKey
import mega.privacy.android.navigation.destination.MediaMainNavKey

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
        val photoSelectionResult by photoSelectionResultFlow(LegacyPhotoSelectionNavKey.RESULT)
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
            shareFolderDialogResultFlow = navigationHandler::monitorResult,
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

                navigationHandler.clearResult(LegacyPhotoSelectionNavKey.RESULT)
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
            navigateToAlbumContent = navigationHandler::navigate,
            navigateToLegacyPhotoSelection = navigationHandler::navigate,
            setNavigationItemVisibility = setNavigationItemVisibility,
            onNavigateToTimelinePhotoPreview = navigationHandler::navigate,
            onTransfer = onTransfer,
            onNavigateToAddToAlbum = navigationHandler::navigate,
            onNavigateToCameraUploadsSettings = navigationHandler::navigate,
            onNavigateToUpgradeAccount = navigationHandler::navigate,
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
