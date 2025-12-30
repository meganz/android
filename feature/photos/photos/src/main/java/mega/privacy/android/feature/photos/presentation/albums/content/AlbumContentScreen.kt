package mega.privacy.android.feature.photos.presentation.albums.content

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.triggered
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.indicators.InfiniteProgressBarIndicator
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.components.sheets.SheetActionHeader
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberMultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.components.AddContentFab
import mega.privacy.android.core.nodecomponents.components.selectionmode.SelectionModeBottomBar
import mega.privacy.android.core.nodecomponents.list.NodeActionListTile
import mega.privacy.android.core.nodecomponents.menu.menuaction.DownloadMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.SendToChatMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ShareMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeActionState
import mega.privacy.android.core.nodecomponents.sheet.sort.SortBottomSheet
import mega.privacy.android.core.nodecomponents.sheet.sort.SortBottomSheetResult
import mega.privacy.android.core.sharedcomponents.extension.isDarkMode
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.DownloadPhotoResult
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.photos.R
import mega.privacy.android.feature.photos.extensions.downloadAsStateWithLifecycle
import mega.privacy.android.feature.photos.model.AlbumFlow
import mega.privacy.android.feature.photos.model.AlbumSortConfiguration
import mega.privacy.android.feature.photos.model.AlbumSortOption
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.feature.photos.presentation.albums.content.model.AlbumContentSelectionAction
import mega.privacy.android.feature.photos.presentation.albums.dialog.EnterAlbumNameDialog
import mega.privacy.android.feature.photos.presentation.albums.dialog.RemoveAlbumConfirmationDialog
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumUiState
import mega.privacy.android.feature.photos.presentation.albums.view.AlbumDynamicContentGrid
import mega.privacy.android.feature.photos.presentation.albums.view.AlbumDynamicContentGridSkeleton
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.contract.queue.snackbar.rememberSnackBarQueue
import mega.privacy.android.navigation.destination.AlbumContentPreviewNavKey
import mega.privacy.android.navigation.destination.AlbumGetLinkNavKey
import mega.privacy.android.navigation.destination.LegacyAlbumCoverSelectionNavKey
import mega.privacy.android.navigation.destination.LegacyPhotoSelectionNavKey
import mega.privacy.android.navigation.destination.OverDiskQuotaPaywallWarningNavKey
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.AlbumAddPhotosFABEvent
import mega.privacy.mobile.analytics.event.AlbumContentDeleteAlbumEvent
import mega.privacy.mobile.analytics.event.AlbumContentScreenEvent
import mega.privacy.mobile.analytics.event.AlbumContentShareLinkMenuToolbarEvent
import mega.privacy.mobile.analytics.event.DeleteAlbumCancelButtonPressedEvent
import mega.privacy.mobile.analytics.event.RemoveItemsFromAlbumDialogButtonEvent

@Composable
fun AlbumContentScreen(
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
    viewModel: AlbumContentViewModel = hiltViewModel(),
    actionViewModel: NodeOptionsActionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val actionState by actionViewModel.uiState.collectAsStateWithLifecycle()
    val selectionModeActionHandler = rememberMultiNodeActionHandler(
        viewModel = actionViewModel,
        navigationHandler = navigationHandler
    )

    AlbumContentScreen(
        uiState = uiState,
        actionState = actionState,
        actionHandler = selectionModeActionHandler::invoke,
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
        hidePhotosEvent = { viewModel.hideOrUnhideNodes(true) },
        unhidePhotosEvent = { viewModel.hideOrUnhideNodes(false) },
        removeFavourites = viewModel::removeFavourites,
        removePhotos = viewModel::removePhotosFromAlbum,
        deleteAlbum = viewModel::deleteAlbum,
        resetDeleteAlbumSuccessEvent = viewModel::resetDeleteAlbumSuccess,
        hideDeleteConfirmation = viewModel::resetShowDeleteConfirmation,
        renameAlbum = viewModel::updateAlbumName,
        resetUpdateAlbumNameErrorMessage = viewModel::resetUpdateAlbumNameErrorMessage,
        resetShowUpdateAlbumName = viewModel::resetShowUpdateAlbumName,
        selectAlbumCover = {
            navigationHandler.navigate(
                LegacyAlbumCoverSelectionNavKey(it.id)
            )
        },
        resetSelectAlbumCoverEvent = viewModel::resetSelectAlbumCoverEvent,
        resetManageLink = viewModel::resetManageLink,
        hideRemoveLinkConfirmation = viewModel::resetRemoveLinkConfirmation,
        removeLink = viewModel::disableExportAlbum,
        resetLinkRemovedSuccessEvent = viewModel::resetLinkRemovedSuccess,
        openGetLink = { albumId, hasSensitiveContent ->
            navigationHandler.navigate(
                AlbumGetLinkNavKey(
                    albumId = albumId.id,
                    hasSensitiveContent = hasSensitiveContent
                )
            )
        },
        handleAction = viewModel::handleAction,
        resetPaywallEvent = viewModel::resetPaywallEvent,
        navigateToPaywall = {
            navigationHandler.navigate(OverDiskQuotaPaywallWarningNavKey)
        },
        sortPhotos = viewModel::sortPhotos,
        previewPhoto = viewModel::previewPhoto,
        resetPreviewPhoto = viewModel::resetPreviewPhoto,
        navigateToPhotoPreview = navigationHandler::navigate,
        resetAddMoreItems = viewModel::resetAddMoreItems,
        navigateToLegacyPhotoSelection = navigationHandler::navigate,
        onTransfer = onTransfer,
        consumeDownloadEvent = actionViewModel::markDownloadEventConsumed,
        consumeInfoToShowEvent = actionViewModel::onInfoToShowEventConsumed,
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
    unhidePhotosEvent: () -> Unit,
    removeFavourites: () -> Unit,
    removePhotos: () -> Unit,
    deleteAlbum: () -> Unit,
    resetDeleteAlbumSuccessEvent: () -> Unit,
    hideDeleteConfirmation: () -> Unit,
    renameAlbum: (String) -> Unit,
    resetUpdateAlbumNameErrorMessage: () -> Unit,
    resetShowUpdateAlbumName: () -> Unit,
    selectAlbumCover: (AlbumId) -> Unit,
    resetSelectAlbumCoverEvent: () -> Unit,
    resetManageLink: () -> Unit,
    hideRemoveLinkConfirmation: () -> Unit,
    removeLink: () -> Unit,
    resetLinkRemovedSuccessEvent: () -> Unit,
    openGetLink: (AlbumId, Boolean) -> Unit,
    handleAction: (AlbumContentSelectionAction) -> Unit,
    navigateToPaywall: () -> Unit,
    resetPaywallEvent: () -> Unit,
    sortPhotos: (AlbumSortConfiguration) -> Unit,
    previewPhoto: (PhotoUiState) -> Unit,
    resetPreviewPhoto: () -> Unit,
    navigateToPhotoPreview: (AlbumContentPreviewNavKey) -> Unit,
    resetAddMoreItems: () -> Unit,
    navigateToLegacyPhotoSelection: (LegacyPhotoSelectionNavKey) -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    consumeDownloadEvent: () -> Unit,
    consumeInfoToShowEvent: () -> Unit,
    snackbarQueue: SnackbarEventQueue = rememberSnackBarQueue(),
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val lazyListState = rememberLazyListState()
    val configuration = LocalConfiguration.current
    val smallWidth = remember(configuration) {
        (configuration.screenWidthDp.dp - 1.dp) / 3
    }
    val isUserAlbum = remember(uiState.uiAlbum) {
        uiState.uiAlbum?.mediaAlbum is MediaAlbum.User
    }
    var showDeletePhotosConfirmation by remember { mutableStateOf(false) }
    var isMoreOptionsSheetVisible by rememberSaveable { mutableStateOf(false) }
    var showSortBottomSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Analytics.tracker.trackEvent(AlbumContentScreenEvent)
    }

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
        event = actionState.infoToShowEvent,
        onConsumed = consumeInfoToShowEvent,
        action = { info ->
            snackbarQueue.queueMessage(info.get(context))
        }
    )

    EventEffect(
        event = uiState.deleteAlbumSuccessEvent,
        onConsumed = resetDeleteAlbumSuccessEvent,
        action = onBack
    )

    EventEffect(
        event = uiState.manageLinkEvent,
        onConsumed = resetManageLink,
        action = { event ->
            event?.let {
                openGetLink(event.album.id, event.hasSensitiveContent)
            }
        }
    )

    EventEffect(
        event = uiState.linkRemovedSuccessEvent,
        onConsumed = resetLinkRemovedSuccessEvent,
        action = {
            snackbarQueue.queueMessage(
                resources.getQuantityString(
                    sharedR.plurals.context_link_removal_success,
                    1
                )
            )
        }
    )

    EventEffect(
        event = uiState.selectAlbumCoverEvent,
        onConsumed = resetSelectAlbumCoverEvent,
        action = { albumId ->
            albumId?.let {
                selectAlbumCover(it)
            }
        }
    )

    EventEffect(
        event = uiState.paywallEvent,
        onConsumed = resetPaywallEvent,
        action = navigateToPaywall
    )

    EventEffect(
        event = uiState.previewAlbumContentEvent,
        onConsumed = resetPreviewPhoto,
        action = navigateToPhotoPreview
    )

    EventEffect(
        event = uiState.addMoreItemsEvent,
        onConsumed = resetAddMoreItems,
        action = {
            val album = (uiState.uiAlbum?.mediaAlbum as? MediaAlbum.User) ?: return@EventEffect

            navigateToLegacyPhotoSelection(
                LegacyPhotoSelectionNavKey(
                    albumId = album.id.id,
                    selectionMode = AlbumFlow.Addition.ordinal,
                    captureResult = false
                )
            )
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
                    title = uiState.uiAlbum?.title?.text.orEmpty(),
                    actions = buildList {
                        if (isUserAlbum) {
                            add(
                                MenuActionWithClick(AlbumContentSelectionAction.More) {
                                    isMoreOptionsSheetVisible = true
                                }
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            SelectionModeBottomBar(
                modifier = Modifier
                    .testTag(ALBUM_CONTENT_SCREEN_SELECTION_BOTTOM_BAR),
                visible = uiState.selectedPhotos.isNotEmpty(),
                actions = uiState.visibleBottomBarActions,
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
                            Analytics.tracker.trackEvent(AlbumContentShareLinkMenuToolbarEvent)
                            sharePhotos()
                            deselectAll()
                        }

                        is AlbumContentSelectionAction.Hide -> {
                            hidePhotosEvent()
                            deselectAll()
                        }

                        is AlbumContentSelectionAction.Unhide -> {
                            unhidePhotosEvent()
                            deselectAll()
                        }

                        is AlbumContentSelectionAction.SendToChat -> {
                            sendPhotosToChatEvent()
                            deselectAll()
                        }

                        is AlbumContentSelectionAction.RemoveFavourites -> {
                            removeFavourites()
                            deselectAll()
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            AddContentFab(
                modifier = Modifier.testTag(ALBUM_CONTENT_SCREEN_ADD_CONTENT_FAB),
                visible = uiState.selectedPhotos.isEmpty()
                        && uiState.uiAlbum?.mediaAlbum is MediaAlbum.User
                        && uiState.photos.isNotEmpty(),
                onClick = {
                    Analytics.tracker.trackEvent(AlbumAddPhotosFABEvent)
                    handleAction(AlbumContentSelectionAction.AddItems)
                }
            )
        }
    ) { innerPadding ->
        val isLoadingEmpty =
            uiState.photos.isEmpty() && (uiState.isLoading || uiState.isAddingPhotos)
        val hasPhotos = uiState.photos.isNotEmpty()

        when {
            isLoadingEmpty -> {
                // Show skeleton loading state when album is empty and loading
                AlbumDynamicContentGridSkeleton(
                    modifier = Modifier
                        .testTag(ALBUM_CONTENT_SCREEN_SKELETON)
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding()),
                    size = smallWidth
                )
            }

            hasPhotos -> {
                // Show photo grid with optional progress indicator
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding()),
                ) {
                    if (uiState.isAddingPhotos || uiState.isRemovingPhotos) {
                        InfiniteProgressBarIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(ALBUM_CONTENT_SCREEN_LOADING_PROGRESS)
                        )
                    }

                    AlbumDynamicContentGrid(
                        modifier = Modifier.fillMaxSize(),
                        lazyListState = lazyListState,
                        photos = uiState.photos,
                        smallWidth = smallWidth,
                        onClick = { photo ->
                            if (uiState.selectedPhotos.isEmpty()) {
                                previewPhoto(photo)
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
                        contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
                        onSortOrderClick = {
                            showSortBottomSheet = true
                        },
                        sortConfiguration = uiState.albumSortConfiguration
                    )
                }
            }

            else -> {
                // Show empty state when album has no photos and not loading
                AlbumContentEmptyLayout(
                    modifier = Modifier
                        .testTag(ALBUM_CONTENT_SCREEN_EMPTY_PHOTOS_LAYOUT)
                        .fillMaxSize(),
                    isActionVisible = uiState.uiAlbum?.mediaAlbum is MediaAlbum.User && uiState.photos.isEmpty()
                ) {
                    handleAction(AlbumContentSelectionAction.AddItems)
                }
            }
        }

        RemovePhotosConfirmationDialog(
            isVisible = showDeletePhotosConfirmation,
            onConfirm = {
                Analytics.tracker.trackEvent(RemoveItemsFromAlbumDialogButtonEvent)
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
            isVisible = uiState.showDeleteAlbumConfirmation == triggered,
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

        RemoveLinksDialog(
            isVisible = uiState.showRemoveLinkConfirmation == triggered,
            onConfirm = {
                hideRemoveLinkConfirmation()
                removeLink()
            },
            onDismiss = hideRemoveLinkConfirmation
        )

        AlbumOptionsBottomSheet(
            isVisible = isMoreOptionsSheetVisible && isUserAlbum,
            onDismiss = { isMoreOptionsSheetVisible = false },
            albumUiState = uiState.uiAlbum,
            onAction = handleAction,
            isDarkTheme = uiState.themeMode.isDarkMode()
        )

        if (uiState.showUpdateAlbumName == triggered) {
            EnterAlbumNameDialog(
                modifier = Modifier
                    .testTag(ALBUM_CONTENT_SCREEN_UPDATE_ALBUM_DIALOG),
                onDismiss = resetShowUpdateAlbumName,
                onConfirm = renameAlbum,
                resetErrorMessage = resetUpdateAlbumNameErrorMessage,
                errorText = (uiState.updateAlbumNameErrorMessage as? StateEventWithContentTriggered)?.content,
                name = uiState.uiAlbum?.title?.text.orEmpty(),
                positiveButtonText = stringResource(sharedR.string.context_rename)
            )
        }

        if (showSortBottomSheet) {
            SortBottomSheet(
                modifier = Modifier
                    .testTag(ALBUM_CONTENT_SCREEN_SORT_BOTTOM_SHEET),
                options = AlbumSortOption.entries,
                title = stringResource(sharedR.string.action_sort_by_header),
                sheetState = rememberModalBottomSheetState(),
                selectedSort = SortBottomSheetResult(
                    sortOptionItem = uiState.albumSortConfiguration.sortOption,
                    sortDirection = uiState.albumSortConfiguration.sortDirection
                ),
                onSortOptionSelected = { result ->
                    result?.let {
                        sortPhotos(
                            AlbumSortConfiguration(
                                sortOption = it.sortOptionItem,
                                sortDirection = it.sortDirection
                            )
                        )
                        showSortBottomSheet = false
                    }
                },
                onDismissRequest = {
                    showSortBottomSheet = false
                }
            )
        }
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

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
internal fun RemoveLinksDialog(
    isVisible: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    BasicDialog(
        modifier = Modifier.testTag(ALBUM_CONTENT_SCREEN_REMOVE_LINKS_DIALOG),
        title = stringResource(sharedR.string.album_content_remove_link_dialog_title),
        description = stringResource(sharedR.string.album_content_remove_link_dialog_description),
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
    onAction: (AlbumContentSelectionAction) -> Unit,
    isVisible: Boolean = false,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
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
            val placeholder = if (isDarkTheme) {
                painterResource(R.drawable.ic_album_cover_d)
            } else {
                painterResource(R.drawable.ic_album_cover)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                SheetActionHeader(
                    title = albumUiState?.title?.text,
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
                                .clip(RoundedCornerShape(8.dp)),
                            placeholder = placeholder,
                            error = placeholder
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
                            onAction(action)
                            onDismiss()
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
            unhidePhotosEvent = {},
            removeFavourites = {},
            removePhotos = {},
            deleteAlbum = {},
            resetDeleteAlbumSuccessEvent = {},
            hideDeleteConfirmation = {},
            renameAlbum = {},
            resetUpdateAlbumNameErrorMessage = {},
            resetShowUpdateAlbumName = {},
            selectAlbumCover = {},
            resetSelectAlbumCoverEvent = {},
            resetManageLink = {},
            hideRemoveLinkConfirmation = {},
            removeLink = {},
            resetLinkRemovedSuccessEvent = {},
            openGetLink = { _, _ -> },
            navigateToPaywall = {},
            resetPaywallEvent = {},
            sortPhotos = {},
            previewPhoto = {},
            resetPreviewPhoto = {},
            navigateToPhotoPreview = {},
            resetAddMoreItems = {},
            onTransfer = {},
            consumeDownloadEvent = {},
            consumeInfoToShowEvent = {},
            handleAction = {},
            navigateToLegacyPhotoSelection = {}
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
internal const val ALBUM_CONTENT_SCREEN_UPDATE_ALBUM_DIALOG =
    "album_content_screen:update_album_dialog"
internal const val ALBUM_CONTENT_SCREEN_REMOVE_LINKS_DIALOG =
    "album_content_screen:remove_links_dialog"
internal const val ALBUM_CONTENT_SCREEN_SORT_BOTTOM_SHEET =
    "album_content_screen:sort_bottom_sheet"
internal const val ALBUM_CONTENT_SCREEN_LOADING_PROGRESS =
    "album_content_screen:loading_progress"
internal const val ALBUM_CONTENT_SCREEN_SKELETON =
    "album_content_screen:skeleton"
internal const val ALBUM_CONTENT_SCREEN_EMPTY_PHOTOS_LAYOUT =
    "album_content_screen:empty_photos_layout"
internal const val ALBUM_CONTENT_SCREEN_ADD_CONTENT_FAB =
    "album_content_screen:add_content_fab"