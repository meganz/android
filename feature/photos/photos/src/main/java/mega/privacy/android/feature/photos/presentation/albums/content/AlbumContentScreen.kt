package mega.privacy.android.feature.photos.presentation.albums.content

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberNodeActionHandler
import mega.privacy.android.core.nodecomponents.components.selectionmode.SelectionModeBottomBar
import mega.privacy.android.core.nodecomponents.menu.menuaction.DownloadMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.HideMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.SendToChatMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ShareMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeActionState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.feature.photos.presentation.albums.content.model.AlbumContentSelectionAction
import mega.privacy.android.feature.photos.presentation.albums.view.AlbumDynamicContentGrid
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.mobile.analytics.event.AlbumContentDeleteAlbumEvent

@Composable
fun AlbumContentScreen(
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
    viewModel: AlbumContentViewModel = hiltViewModel(),
    actionViewModel: NodeOptionsActionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val actionState by actionViewModel.uiState.collectAsStateWithLifecycle()
    val nodeActionHandler = rememberNodeActionHandler(
        viewModel = actionViewModel,
        navigationHandler = navigationHandler
    )

    AlbumContentScreen(
        uiState = uiState,
        actionState = actionState,
        actionHandler = nodeActionHandler::invoke,
        onBack = navigationHandler::back,
        togglePhotoSelection = viewModel::togglePhotoSelection,
        selectAll = viewModel::selectAllPhotos,
        deselectAll = viewModel::clearSelectedPhotos,
        savePhotosToDevice = viewModel::savePhotosToDevice,
        resetSavePhotosToDeviceEvent = viewModel::resetSavePhotosToDevice,
        sharePhotos = viewModel::sharePhotos,
        resetSharePhotosEvent = viewModel::resetSharePhotos,
        sendPhotosToChatEvent = viewModel::sendPhotosToChat,
        resetSendPhotosToChatEvent = viewModel::resetSendPhotosToChat,
        hidePhotosEvent = viewModel::hidePhotos,
        resetHidePhotosEvent = viewModel::resetHidePhotos,
        onTransfer = onTransfer,
        consumeDownloadEvent = actionViewModel::markDownloadEventConsumed,
        consumeInfoToShowEvent = actionViewModel::onInfoToShowEventConsumed
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlbumContentScreen(
    uiState: AlbumContentUiState,
    actionState: NodeActionState,
    actionHandler: (MenuAction, List<TypedNode>) -> Unit,
    onBack: () -> Unit,
    togglePhotoSelection: (PhotoUiState) -> Unit,
    selectAll: () -> Unit,
    deselectAll: () -> Unit,
    savePhotosToDevice: () -> Unit,
    resetSavePhotosToDeviceEvent: () -> Unit,
    sharePhotos: () -> Unit,
    resetSharePhotosEvent: () -> Unit,
    sendPhotosToChatEvent: () -> Unit,
    resetSendPhotosToChatEvent: () -> Unit,
    hidePhotosEvent: () -> Unit,
    resetHidePhotosEvent: () -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    consumeDownloadEvent: () -> Unit,
    consumeInfoToShowEvent: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackBarHostState.current
    val lazyListState = rememberLazyListState()
    val configuration = LocalConfiguration.current
    val smallWidth = remember(configuration) {
        (configuration.screenWidthDp.dp - 1.dp) / 3
    }
    var showDeletePhotosConfirmation by remember { mutableStateOf(false) }

    EventEffect(
        event = actionState.downloadEvent,
        onConsumed = consumeDownloadEvent,
        action = onTransfer
    )

    EventEffect(
        event = uiState.savePhotosToDeviceEvent,
        onConsumed = resetSavePhotosToDeviceEvent,
        action = { photos ->
            actionHandler(DownloadMenuAction(), photos)
        }
    )

    EventEffect(
        event = uiState.sharePhotosEvent,
        onConsumed = resetSharePhotosEvent,
        action = { photos ->
            actionHandler(ShareMenuAction(), photos)
        }
    )

    EventEffect(
        event = uiState.sendPhotosToChatEvent,
        onConsumed = resetSendPhotosToChatEvent,
        action = { photos ->
            actionHandler(SendToChatMenuAction(), photos)
        }
    )

    EventEffect(
        event = uiState.hidePhotosEvent,
        onConsumed = resetHidePhotosEvent,
        action = { photos ->
            actionHandler(HideMenuAction(), photos)
        }
    )

    EventEffect(
        event = actionState.infoToShowEvent,
        onConsumed = consumeInfoToShowEvent,
        action = { info ->
            snackbarHostState?.showAutoDurationSnackbar(info.get(context))
        }
    )

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        topBar = {
            if (uiState.selectedPhotos.isNotEmpty()) {
                MegaTopAppBar(
                    modifier = Modifier
                        .testTag(ALBUM_CONTENT_SCREEN_SELECTION_TOOLBAR),
                    title = uiState.selectedPhotos.size.toString(),
                    navigationType = AppBarNavigationType.Close(deselectAll),
                    actions = listOf(AlbumContentSelectionAction.SelectAll),
                    onActionPressed = { action ->
                        when (action) {
                            is AlbumContentSelectionAction.SelectAll -> selectAll()
                            else -> return@MegaTopAppBar
                        }
                    }
                )
            } else {
                MegaTopAppBar(
                    modifier = Modifier
                        .testTag(ALBUM_CONTENT_SCREEN_DEFAULT_TOOLBAR),
                    navigationType = AppBarNavigationType.Back(onBack),
                    title = uiState.uiAlbum?.title?.getTitleString(context).orEmpty(),
                    actions = listOf(
                        MenuActionWithClick(AlbumContentSelectionAction.More) {
                            // Todo show bottom sheet on More click
                        }
                    )
                )
            }
        },
        bottomBar = {
            SelectionModeBottomBar(
                modifier = Modifier
                    .testTag(ALBUM_CONTENT_SCREEN_SELECTION_BOTTOM_BAR),
                visible = uiState.selectedPhotos.isNotEmpty(),
                actions = AlbumContentSelectionAction.bottomBarItems,
                onActionPressed = {
                    when (it) {
                        is AlbumContentSelectionAction.Delete -> {
                            Analytics.tracker.trackEvent(AlbumContentDeleteAlbumEvent)
                            showDeletePhotosConfirmation = true
                            // Todo show dialog and delete photos
                        }

                        is AlbumContentSelectionAction.Download -> {
                            savePhotosToDevice()
                            deselectAll()
                        }

                        is AlbumContentSelectionAction.Share -> {
                            sharePhotos()
                            deselectAll()
                        }

                        is AlbumContentSelectionAction.Hide -> {
                            hidePhotosEvent()
                            deselectAll()
                        }

                        is AlbumContentSelectionAction.SendToChat -> {
                            sendPhotosToChatEvent()
                            deselectAll()
                        }
                    }
                }
            )
        },
    ) { innerPadding ->
        AlbumDynamicContentGrid(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            lazyListState = lazyListState,
            photos = uiState.photos,
            smallWidth = smallWidth,
            onClick = { photo ->
                if (uiState.selectedPhotos.isEmpty()) {
                    //onNavigatePhotoPreview(photo, photos)
                } else {
                    togglePhotoSelection(photo)
                }
            },
            onLongPress = { photo ->
                togglePhotoSelection(photo)
            },
            selectedPhotos = uiState.selectedPhotos,
            shouldApplySensitiveMode = uiState.hiddenNodeEnabled
                    && uiState.accountType?.isPaid == true
                    && !uiState.isBusinessAccountExpired,
            contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding())
        )
    }
}

@CombinedThemePreviews
@Composable
private fun AlbumContentScreenPreview() {
    AndroidThemeForPreviews {
        AlbumContentScreen(
            uiState = AlbumContentUiState(),
            actionState = NodeActionState(),
            actionHandler = { _, _ -> },
            onBack = {},
            togglePhotoSelection = {},
            selectAll = {},
            deselectAll = {},
            savePhotosToDevice = {},
            resetSavePhotosToDeviceEvent = {},
            sharePhotos = {},
            resetSharePhotosEvent = {},
            sendPhotosToChatEvent = {},
            resetSendPhotosToChatEvent = {},
            hidePhotosEvent = {},
            resetHidePhotosEvent = {},
            onTransfer = {},
            consumeDownloadEvent = {},
            consumeInfoToShowEvent = {}
        )
    }
}

internal const val ALBUM_CONTENT_SCREEN_DEFAULT_TOOLBAR = "album_content_screen:default_toolbar"
internal const val ALBUM_CONTENT_SCREEN_SELECTION_TOOLBAR = "album_content_screen:selection_toolbar"
internal const val ALBUM_CONTENT_SCREEN_SELECTION_BOTTOM_BAR =
    "album_content_screen:selection_bottom_bar"