@file:Suppress("ComposeUnstableCollections")

package mega.privacy.android.feature.photos.presentation.albums.importlink

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.button.SecondaryFilledButton
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.dialogs.BasicInputDialog
import mega.android.core.ui.components.dialogs.MegaDialogProperties
import mega.android.core.ui.components.divider.SubtleDivider
import mega.android.core.ui.components.indicators.LargeInfiniteSpinnerIndicator
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.components.surface.RowSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.photos.R
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.feature.photos.presentation.albums.view.AlbumDynamicContentGrid
import mega.privacy.android.feature.photos.presentation.albums.view.AlbumDynamicContentGridSkeleton
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.contract.queue.snackbar.rememberSnackBarQueue
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.shared.resources.R as sharedResR
import mega.privacy.mobile.analytics.event.AlbumImportInputDecryptionKeyDialogEvent
import mega.privacy.mobile.analytics.event.AlbumImportSaveToCloudDriveButtonEvent
import mega.privacy.mobile.analytics.event.AlbumImportSaveToDeviceButtonEvent
import mega.privacy.mobile.analytics.event.AlbumImportScreenEvent
import mega.privacy.mobile.analytics.event.AlbumImportStorageOverQuotaDialogEvent
import mega.privacy.mobile.analytics.event.AlbumsStorageOverQuotaUpgradeAccountButtonEvent
import mega.privacy.mobile.analytics.event.ImportAlbumContentLoadedEvent
import mega.privacy.mobile.analytics.event.PhotoItemSelected
import mega.privacy.mobile.analytics.event.PhotoItemSelectedEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumImportScreen(
    onShareLink: (String) -> Unit,
    onPreviewPhoto: (PhotoUiState) -> Unit,
    onNavigateFileExplorer: () -> Unit,
    onUpgradeAccount: () -> Unit,
    onBack: () -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    modifier: Modifier = Modifier,
    albumImportViewModel: AlbumImportViewModel =
        hiltViewModel<AlbumImportViewModel, AlbumImportViewModel.Factory> {
            it.create(null)
        },
    showOverDiskQuotaPaywallWarning: () -> Unit = {},
) {
    val state by albumImportViewModel.stateFlow.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarEventQueue = rememberSnackBarQueue()

    LifecycleResumeEffect(Unit) {
        Analytics.tracker.trackEvent(AlbumImportScreenEvent)
        onPauseOrDispose {}
    }

    LaunchedEffect(state.importAlbumMessage) {
        val message = state.importAlbumMessage

        if (message != null) {
            snackbarEventQueue.queueMessage(message)
            albumImportViewModel.clearImportAlbumMessage()
        }
    }

    if (state.showInputDecryptionKeyDialog) {
        InputDecryptionKeyDialog(
            onDismiss = onBack,
            onDecrypt = { key ->
                albumImportViewModel.closeInputDecryptionKeyDialog()
                albumImportViewModel.decryptLink(key)
            }
        )
    }

    if (state.showErrorAccessDialog) {
        BasicDialog(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            onDismiss = onBack,
            title = stringResource(id = sharedR.string.album_import_error_access_dialog_title),
            description = stringResource(id = sharedR.string.album_import_error_access_dialog_description),
            positiveButtonText = stringResource(id = sharedResR.string.general_ok),
            onPositiveButtonClicked = onBack,
        )
    }

    if (state.showRenameAlbumDialog) {
        RenameAlbumDialog(
            album = state.album,
            errorMessage = state.renameAlbumErrorMessage,
            onClearErrorMessage = albumImportViewModel::clearRenameAlbumErrorMessage,
            onDismiss = albumImportViewModel::closeRenameAlbumDialog,
            onRename = albumImportViewModel::validateAlbumName,
        )
    }

    if (state.showStorageExceededDialog) {
        StorageExceededDialog(
            onDismiss = albumImportViewModel::closeStorageExceededDialog,
            onUpgradeAccount = {
                onUpgradeAccount()
                albumImportViewModel.closeStorageExceededDialog()
            },
        )
    }

    if (state.showImportAlbumDialog) {
        ImportAlbumDialog()
    }

    if (state.isRenameAlbumValid) {
        onNavigateFileExplorer()
        albumImportViewModel.clearRenameAlbumValid()
    }

    if (state.isImportConstraintValid) {
        onNavigateFileExplorer()
        albumImportViewModel.clearImportConstraintValid()
    }

    EventEffect(
        event = state.openFileNodeEvent,
        onConsumed = albumImportViewModel::resetOpenFileNodeEvent
    ) { photo ->
        onPreviewPhoto(photo)
    }

    EventEffect(
        event = state.downloadEvent,
        onConsumed = albumImportViewModel::consumeDownloadEvent,
        action = onTransfer
    )

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier.systemBarsPadding(),
        topBar = {
            AlbumImportTopBar(
                album = state.album,
                photos = state.photos,
                selectedPhotos = state.selectedPhotos,
                onShareLink = { onShareLink(state.link.orEmpty()) },
                onClearSelection = albumImportViewModel::clearSelection,
                onBack = {
                    if (state.selectedPhotos.isNotEmpty()) {
                        albumImportViewModel.clearSelection()
                    } else {
                        onBack()
                    }
                },
            )
        },
        bottomBar = {
            if (state.isLocalAlbumsLoaded && state.isAvailableStorageCollected && state.album != null && state.photos.isNotEmpty()) {
                AlbumImportBottomBar(
                    isLogin = state.isLogin,
                    onImport = {
                        if (state.isNetworkConnected) {
                            if (state.storageState == StorageState.PayWall) {
                                showOverDiskQuotaPaywallWarning()
                            } else {
                                val photos = state.selectedPhotos.ifEmpty { state.photos }
                                albumImportViewModel.validateImportConstraint(
                                    album = state.album,
                                    photos = photos,
                                )
                                albumImportViewModel.clearSelection()
                            }
                        } else {
                            coroutineScope.launch {
                                snackbarEventQueue.queueMessage(
                                    message = resources.getString(sharedR.string.photos_network_error_message),
                                )
                            }
                        }
                    },
                    onSaveToDevice = {
                        albumImportViewModel.startDownload()
                    }
                )
            }
        },
        content = { innerPaddings ->
            AlbumImportContent(
                modifier = Modifier.padding(innerPaddings),
                isLocalAlbumsLoaded = state.isLocalAlbumsLoaded,
                isAvailableStorageCollected = state.isAvailableStorageCollected,
                album = state.album,
                photos = state.photos,
                selectedPhotos = state.selectedPhotos,
                onClickPhoto = { photo ->
                    if (state.selectedPhotos.isEmpty()) {
                        Analytics.tracker.trackEvent(
                            PhotoItemSelectedEvent(selectionType = PhotoItemSelected.SelectionType.Single)
                        )
                        if (state.isNetworkConnected) {
                            onPreviewPhoto(photo)
                        } else {
                            coroutineScope.launch {
                                snackbarEventQueue.queueMessage(
                                    message = resources.getString(sharedR.string.photos_network_error_message),
                                )
                            }
                        }
                    } else if (photo in state.selectedPhotos) {
                        Analytics.tracker.trackEvent(
                            PhotoItemSelectedEvent(selectionType = PhotoItemSelected.SelectionType.MultiRemove)
                        )
                        albumImportViewModel.unselectPhoto(photo)
                    } else {
                        Analytics.tracker.trackEvent(
                            PhotoItemSelectedEvent(selectionType = PhotoItemSelected.SelectionType.MultiAdd)
                        )
                        albumImportViewModel.selectPhoto(photo)
                    }
                },
                onPhotoSelection = { photo ->
                    if (photo in state.selectedPhotos) {
                        albumImportViewModel.unselectPhoto(photo)
                    } else {
                        albumImportViewModel.selectPhoto(photo)
                    }
                },
                onAlbumLoaded = {
                    Analytics.tracker.trackEvent(ImportAlbumContentLoadedEvent)
                }
            )
        },
    )
}

@Composable
private fun AlbumImportTopBar(
    album: Album.UserAlbum?,
    photos: List<PhotoUiState>,
    selectedPhotos: Set<PhotoUiState>,
    onShareLink: () -> Unit,
    onClearSelection: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasSelectedPhotos = selectedPhotos.isNotEmpty()

    if (hasSelectedPhotos) {
        MegaTopAppBar(
            modifier = modifier,
            title = selectedPhotos.size.toString(),
            navigationType = AppBarNavigationType.Close(onClearSelection),
        )
    } else {
        MegaTopAppBar(
            modifier = modifier,
            title = album?.title
                ?: ("${stringResource(id = sharedR.string.photos_empty_screen_brand_name_text)} - " +
                        stringResource(id = sharedR.string.photos_loading_indicator_text)),
            subtitle = album?.let { stringResource(id = sharedR.string.album_import_screen_subtitle_album_link_text) },
            navigationType = AppBarNavigationType.Back(onBack),
            actions = buildList {
                if (album != null) {
                    add(
                        MenuActionWithClick(
                            menuAction = object : MenuActionWithIcon {
                                override val testTag: String = "album_import_top_bar:share"

                                @Composable
                                override fun getDescription(): String =
                                    stringResource(sharedR.string.general_share)

                                @Composable
                                override fun getIconPainter(): Painter =
                                    rememberVectorPainter(IconPack.Medium.Thin.Outline.ShareNetwork)
                            },
                            onClick = onShareLink
                        )
                    )
                }
            }
        )
    }
}

@Composable
private fun AlbumImportBottomBar(
    isLogin: Boolean,
    onImport: () -> Unit,
    onSaveToDevice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        content = {
            SubtleDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(
                    space = 8.dp,
                    alignment = Alignment.End
                ),
                verticalAlignment = Alignment.CenterVertically,
                content = {
                    SecondaryFilledButton(
                        modifier = Modifier.wrapContentSize(),
                        text = stringResource(sharedResR.string.general_save_to_device),
                        onClick = {
                            Analytics.tracker.trackEvent(AlbumImportSaveToDeviceButtonEvent)
                            onSaveToDevice()
                        }
                    )

                    if (isLogin) {
                        PrimaryFilledButton(
                            modifier = Modifier.wrapContentSize(),
                            text = stringResource(sharedR.string.photos_save_to_cloud_drive_button_text),
                            onClick = {
                                Analytics.tracker.trackEvent(
                                    AlbumImportSaveToCloudDriveButtonEvent
                                )
                                onImport()
                            }
                        )
                    }
                },
            )
        },
    )
}

@Composable
private fun AlbumImportContent(
    isLocalAlbumsLoaded: Boolean,
    isAvailableStorageCollected: Boolean,
    album: Album.UserAlbum?,
    photos: List<PhotoUiState>,
    selectedPhotos: Set<PhotoUiState>,
    onClickPhoto: (PhotoUiState) -> Unit,
    onPhotoSelection: (PhotoUiState) -> Unit,
    onAlbumLoaded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()

    LaunchedEffect(album != null && isLocalAlbumsLoaded) {
        if (album != null && isLocalAlbumsLoaded) {
            onAlbumLoaded()
        }
    }

    when {
        album == null -> {
            AlbumDynamicContentGridSkeleton(
                modifier = modifier.fillMaxSize(),
            )
        }

        photos.isEmpty() -> {
            EmptyStateView(
                modifier = modifier,
                illustration = R.drawable.il_album_image,
                description = SpannableText(
                    text = stringResource(sharedResR.string.album_content_empty_album_title)
                )
            )
        }

        isLocalAlbumsLoaded && isAvailableStorageCollected -> {
            AlbumDynamicContentGrid(
                modifier = modifier.fillMaxSize(),
                lazyListState = lazyListState,
                photos = photos.toImmutableList(),
                selectedPhotos = selectedPhotos.toImmutableSet(),
                onClick = { photoUiState ->
                    if (selectedPhotos.isEmpty()) {
                        onClickPhoto(photoUiState)
                    } else {
                        onPhotoSelection(photoUiState)
                    }
                },
                onLongPress = onPhotoSelection,
                isPublicAlbumPhoto = true
            )
        }
    }
}

@Composable
private fun InputDecryptionKeyDialog(
    onDismiss: () -> Unit,
    onDecrypt: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var inputText by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue("", TextRange(0)))
    }

    LaunchedEffect(Unit) {
        Analytics.tracker.trackEvent(AlbumImportInputDecryptionKeyDialogEvent)
    }

    BasicInputDialog(
        modifier = modifier,
        dialogProperties = MegaDialogProperties.default.copy(
            dismissOnBackPress = false,
            isPositiveButtonEnabled = inputText.text.isNotBlank()
        ),
        onDismiss = onDismiss,
        title = stringResource(id = sharedR.string.album_import_input_decryption_key_dialog_title),
        inputValue = inputText,
        onValueChange = { inputText = it },
        positiveButtonText = stringResource(id = sharedResR.string.general_decrypt),
        onPositiveButtonClicked = { onDecrypt(inputText.text.trim()) },
        negativeButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
        onNegativeButtonClicked = onDismiss
    )
}

@Composable
private fun RenameAlbumDialog(
    album: Album.UserAlbum?,
    errorMessage: String?,
    onClearErrorMessage: () -> Unit,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var inputText by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue("", TextRange(0)))
    }

    BasicInputDialog(
        modifier = modifier,
        dialogProperties = MegaDialogProperties.default.copy(
            dismissOnBackPress = true,
            isPositiveButtonEnabled = inputText.text.isNotBlank()
        ),
        onDismiss = onDismiss,
        title = stringResource(id = sharedR.string.album_import_rename_album_dialog_title),
        description = stringResource(
            id = sharedR.string.album_import_rename_album_dialog_description,
            album?.title.orEmpty(),
        ),
        inputValue = inputText,
        onValueChange = {
            onClearErrorMessage()
            inputText = it
        },
        errorText = errorMessage,
        positiveButtonText = stringResource(id = sharedR.string.context_rename),
        onPositiveButtonClicked = { onRename(inputText.text.trim()) },
        negativeButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
        onNegativeButtonClicked = onDismiss
    )
}

@Composable
private fun ImportAlbumDialog() {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        content = {
            RowSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape = RoundedCornerShape(4.dp))
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                surfaceColor = SurfaceColor.PageBackground,
                content = {
                    LargeInfiniteSpinnerIndicator()

                    Spacer(modifier = Modifier.width(16.dp))

                    MegaText(
                        text = stringResource(id = sharedR.string.album_import_saving_dialog_progress_text),
                        style = AppTheme.typography.bodyMedium,
                        textColor = TextColor.Secondary
                    )
                },
            )
        },
    )
}

@Composable
private fun StorageExceededDialog(
    onDismiss: () -> Unit,
    onUpgradeAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        Analytics.tracker.trackEvent(AlbumImportStorageOverQuotaDialogEvent)
    }

    BasicDialog(
        modifier = modifier,
        title = stringResource(id = sharedR.string.album_import_storage_exceeded_dialog_title),
        description = stringResource(id = sharedR.string.album_import_storage_exceeded_dialog_description),
        positiveButtonText = stringResource(id = sharedR.string.general_upgrade_button),
        onPositiveButtonClicked = {
            Analytics.tracker.trackEvent(AlbumsStorageOverQuotaUpgradeAccountButtonEvent)
            onUpgradeAccount()
        },
        negativeButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
        onNegativeButtonClicked = onDismiss,
        onDismiss = onDismiss,
        dismissOnBackPress = true,
        dismissOnClickOutside = false,
    )
}
