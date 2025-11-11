package mega.privacy.android.feature.photos.presentation.albums.content

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.triggered
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.components.sheets.SheetActionHeader
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
import mega.privacy.android.core.nodecomponents.list.NodeActionListTile
import mega.privacy.android.core.nodecomponents.menu.menuaction.DownloadMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.HideMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.SendToChatMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ShareMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeActionState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.photos.DownloadPhotoResult
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.photos.extensions.downloadAsStateWithLifecycle
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.feature.photos.presentation.albums.content.model.AlbumContentSelectionAction
import mega.privacy.android.feature.photos.presentation.albums.dialog.RemoveAlbumConfirmationDialog
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumUiState
import mega.privacy.android.feature.photos.presentation.albums.view.AlbumDynamicContentGrid
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.AlbumContentDeleteAlbumEvent
import mega.privacy.mobile.analytics.event.DeleteAlbumCancelButtonPressedEvent

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
        removePhotos = viewModel::removePhotosFromAlbum,
        deleteAlbum = viewModel::deleteAlbum,
        resetDeleteAlbumSuccessEvent = viewModel::resetDeleteAlbumSuccess,
        showDeleteConfirmation = viewModel::showDeleteConfirmation,
        hideDeleteConfirmation = viewModel::resetShowDeleteConfirmation,
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
    removePhotos: () -> Unit,
    deleteAlbum: () -> Unit,
    resetDeleteAlbumSuccessEvent: () -> Unit,
    showDeleteConfirmation: () -> Unit,
    hideDeleteConfirmation: () -> Unit,
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
    var isMoreOptionsSheetVisible by rememberSaveable { mutableStateOf(false) }

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

    EventEffect(
        event = uiState.deleteAlbumSuccessEvent,
        onConsumed = resetDeleteAlbumSuccessEvent,
        action = onBack
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
                    title = uiState.uiAlbum?.title.orEmpty(),
                    actions = listOf(
                        MenuActionWithClick(AlbumContentSelectionAction.More) {
                            isMoreOptionsSheetVisible = true
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

        RemovePhotosConfirmationDialog(
            isVisible = showDeletePhotosConfirmation,
            onConfirm = {
                Analytics.tracker.trackEvent(AlbumContentDeleteAlbumEvent)
                removePhotos()
                deselectAll()
                showDeletePhotosConfirmation = false
            },
            onDismiss = {
                deselectAll()
                showDeletePhotosConfirmation = false
            }
        )

        RemoveAlbumConfirmationDialog(
            modifier = Modifier.testTag(ALBUM_CONTENT_SCREEN_DELETE_ALBUM_DIALOG),
            size = 1,
            isVisible = uiState.showDeleteConfirmation == triggered,
            onConfirm = {
                Analytics.tracker.trackEvent(AlbumContentDeleteAlbumEvent)
                deleteAlbum()
                hideDeleteConfirmation()
            },
            onDismiss = {
                Analytics.tracker.trackEvent(DeleteAlbumCancelButtonPressedEvent)
                hideDeleteConfirmation()
            }
        )

        AlbumOptionsBottomSheet(
            isVisible = isMoreOptionsSheetVisible,
            onDismiss = { isMoreOptionsSheetVisible = false },
            albumUiState = uiState.uiAlbum,
            deleteAlbum = {
                if (uiState.photos.isEmpty()) {
                    Analytics.tracker.trackEvent(AlbumContentDeleteAlbumEvent)
                    deleteAlbum()
                } else {
                    showDeleteConfirmation()
                }
            }
        )
    }
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
internal fun RemovePhotosConfirmationDialog(
    isVisible: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    BasicDialog(
        modifier = Modifier.testTag(ALBUM_CONTENT_SCREEN_DELETE_PHOTOS_DIALOG),
        description = stringResource(sharedR.string.album_content_remove_photos_dialog_description),
        positiveButtonText = stringResource(sharedR.string.general_remove),
        onPositiveButtonClicked = onConfirm,
        negativeButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
        onNegativeButtonClicked = onDismiss,
        onDismiss = onDismiss,
        isVisible = isVisible
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
internal fun AlbumOptionsBottomSheet(
    onDismiss: () -> Unit,
    albumUiState: AlbumUiState?,
    deleteAlbum: () -> Unit,
    isVisible: Boolean = false,
) {
    val sheetState = rememberModalBottomSheetState()
    val cover = albumUiState?.cover?.downloadAsStateWithLifecycle(isPreview = false)
    val downloadedPhoto = when (val result = cover?.value) {
        is DownloadPhotoResult.Success -> result.thumbnailFilePath
        else -> null
    }
    val coroutineScope = rememberCoroutineScope()
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            showBottomSheet = true
        } else {
            coroutineScope.launch {
                sheetState.hide()
            }.invokeOnCompletion {
                if (!sheetState.isVisible) {
                    showBottomSheet = false
                }
            }
        }
    }

    if (showBottomSheet) {
        MegaModalBottomSheet(
            modifier = Modifier.testTag(ALBUM_CONTENT_SCREEN_MORE_OPTIONS_BOTTOM_SHEET),
            sheetState = sheetState,
            bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
            onDismissRequest = onDismiss
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                SheetActionHeader(
                    title = albumUiState?.title.orEmpty(),
                    leadingElement = {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(downloadedPhoto)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(32.dp)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    },
                    onClickListener = null
                )

                AlbumContentSelectionAction.bottomSheetItems.forEach { action ->
                    NodeActionListTile(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        menuAction = action,
                        onActionClicked = {
                            when (action) {
                                is AlbumContentSelectionAction.Rename -> {
                                    // Todo rename album
                                }

                                is AlbumContentSelectionAction.SelectAlbumCover -> {
                                    // Todo select album cover
                                }

                                is AlbumContentSelectionAction.ManageLink -> {
                                    // Todo manage link
                                }

                                is AlbumContentSelectionAction.RemoveLink -> {
                                    // Todo share album
                                }

                                is AlbumContentSelectionAction.Delete -> {
                                    deleteAlbum()
                                    onDismiss()
                                }

                                else -> {}
                            }
                        },
                        isDestructive = action.highlightIcon
                    )
                }
            }
        }
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
            removePhotos = {},
            deleteAlbum = {},
            resetDeleteAlbumSuccessEvent = {},
            showDeleteConfirmation = {},
            hideDeleteConfirmation = {},
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
internal const val ALBUM_CONTENT_SCREEN_DELETE_PHOTOS_DIALOG =
    "album_content_screen:delete_photos_dialog"
internal const val ALBUM_CONTENT_SCREEN_MORE_OPTIONS_BOTTOM_SHEET =
    "album_content_screen:more_options_bottom_sheet"
internal const val ALBUM_CONTENT_SCREEN_DELETE_ALBUM_DIALOG =
    "album_content_screen:delete_album_dialog"