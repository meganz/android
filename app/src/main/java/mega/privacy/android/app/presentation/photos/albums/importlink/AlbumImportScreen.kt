package mega.privacy.android.app.presentation.photos.albums.importlink

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.getStorageState
import mega.privacy.android.app.presentation.photos.albums.view.DynamicView
import mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponent
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyView
import mega.privacy.android.legacy.core.ui.controls.dialogs.MegaDialog
import mega.privacy.android.shared.original.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.shared.original.core.ui.controls.progressindicator.MegaCircularProgressIndicator
import mega.privacy.android.shared.original.core.ui.controls.textfields.GenericTextField
import mega.privacy.android.shared.original.core.ui.theme.accent_900
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_020_grey_700
import mega.privacy.android.shared.original.core.ui.theme.grey_alpha_012
import mega.privacy.android.shared.original.core.ui.theme.grey_alpha_038
import mega.privacy.android.shared.original.core.ui.theme.grey_alpha_054
import mega.privacy.android.shared.original.core.ui.theme.grey_alpha_087
import mega.privacy.android.shared.original.core.ui.theme.white
import mega.privacy.android.shared.original.core.ui.theme.white_alpha_012
import mega.privacy.android.shared.original.core.ui.theme.white_alpha_038
import mega.privacy.android.shared.original.core.ui.theme.white_alpha_054
import mega.privacy.android.shared.original.core.ui.theme.white_alpha_087
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.AlbumImportInputDecryptionKeyDialogEvent
import mega.privacy.mobile.analytics.event.AlbumImportSaveToCloudDriveButtonEvent
import mega.privacy.mobile.analytics.event.AlbumImportSaveToDeviceButtonEvent
import mega.privacy.mobile.analytics.event.AlbumImportScreenEvent
import mega.privacy.mobile.analytics.event.AlbumImportStorageOverQuotaDialogEvent
import mega.privacy.mobile.analytics.event.AlbumsStorageOverQuotaUpgradeAccountButtonEvent
import mega.privacy.mobile.analytics.event.ImportAlbumContentLoadedEvent
import mega.privacy.mobile.analytics.event.PhotoItemSelected
import mega.privacy.mobile.analytics.event.PhotoItemSelectedEvent
import mega.privacy.android.shared.resources.R as sharedResR

private typealias ImageDownloader = (
    isPreview: Boolean,
    photo: Photo,
    callback: (Boolean) -> Unit,
) -> Unit

@Composable
internal fun AlbumImportScreen(
    albumImportViewModel: AlbumImportViewModel = viewModel(),
    onShareLink: (String) -> Unit,
    onPreviewPhoto: (Photo) -> Unit,
    onNavigateFileExplorer: () -> Unit,
    onUpgradeAccount: () -> Unit,
    onBack: (isBackToHome: Boolean) -> Unit,
    navigateToStorageSettings: () -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight
    val state by albumImportViewModel.stateFlow.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Analytics.tracker.trackEvent(AlbumImportScreenEvent)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(state.importAlbumMessage) {
        val message = state.importAlbumMessage

        if (message != null) {
            scaffoldState.snackbarHostState.showAutoDurationSnackbar(message)
            albumImportViewModel.clearImportAlbumMessage()
        }
    }

    if (state.showInputDecryptionKeyDialog) {
        InputDecryptionKeyDialog(
            onDismiss = { onBack(false) },
            onDecrypt = { key ->
                albumImportViewModel.closeInputDecryptionKeyDialog()
                albumImportViewModel.decryptLink(key)
            },
            onDialogDisplayed = {
                Analytics.tracker.trackEvent(AlbumImportInputDecryptionKeyDialogEvent)
            }
        )
    }

    if (state.showErrorAccessDialog) {
        ErrorAccessDialog(
            onDismiss = { onBack(state.isBackToHome) },
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
        LaunchedEffect(Unit) {
            Analytics.tracker.trackEvent(AlbumImportStorageOverQuotaDialogEvent)
        }
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

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        scaffoldState = scaffoldState,
        topBar = {
            AlbumImportTopBar(
                album = state.album,
                photos = state.photos,
                selectedPhotos = state.selectedPhotos,
                onShareLink = { onShareLink(state.link.orEmpty()) },
                onSelectAllPhotos = albumImportViewModel::selectAllPhotos,
                onClearSelection = albumImportViewModel::clearSelection,
                onBack = {
                    if (state.selectedPhotos.isNotEmpty()) {
                        albumImportViewModel.clearSelection()
                    } else {
                        onBack(false)
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
                            if (getStorageState() == StorageState.PayWall) {
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
                                scaffoldState.snackbarHostState.showAutoDurationSnackbar(
                                    message = context.resources.getString(R.string.error_server_connection_problem),
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
        snackbarHost = { snackbarHostState ->
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { snackbarData ->
                    Snackbar(
                        snackbarData = snackbarData,
                        backgroundColor = grey_alpha_087.takeIf { isLight } ?: white,
                    )
                },
            )
        },
        content = { innerPaddings ->
            AlbumImportContent(
                modifier = Modifier.padding(innerPaddings),
                isLocalAlbumsLoaded = state.isLocalAlbumsLoaded,
                isAvailableStorageCollected = state.isAvailableStorageCollected,
                album = state.album,
                photos = state.photos,
                selectedPhotos = state.selectedPhotos,
                onDownloadImage = albumImportViewModel::downloadImage,
                onClickPhoto = { photo ->
                    if (state.selectedPhotos.isEmpty()) {
                        Analytics.tracker.trackEvent(
                            PhotoItemSelectedEvent(selectionType = PhotoItemSelected.SelectionType.Single)
                        )
                        if (state.isNetworkConnected) {
                            onPreviewPhoto(photo)
                        } else {
                            coroutineScope.launch {
                                scaffoldState.snackbarHostState.showAutoDurationSnackbar(
                                    message = context.resources.getString(R.string.error_server_connection_problem),
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
            StartTransferComponent(
                event = state.downloadEvent,
                onConsumeEvent = albumImportViewModel::consumeDownloadEvent,
                snackBarHostState = scaffoldState.snackbarHostState,
                navigateToStorageSettings = navigateToStorageSettings,
            )
        },
    )
}

@Composable
private fun AlbumImportTopBar(
    modifier: Modifier = Modifier,
    album: Album.UserAlbum?,
    photos: List<Photo>,
    selectedPhotos: Set<Photo>,
    onShareLink: () -> Unit,
    onSelectAllPhotos: () -> Unit,
    onClearSelection: () -> Unit,
    onBack: () -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight
    val hasSelectedPhotos = selectedPhotos.isNotEmpty()

    var showContextMenu by rememberSaveable { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column(
                verticalArrangement = Arrangement.Center,
                content = {
                    if (hasSelectedPhotos) {
                        Text(
                            text = "${selectedPhotos.size}",
                            color = accent_900,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.W500,
                            style = MaterialTheme.typography.subtitle1,
                        )
                    } else {
                        Text(
                            text = album?.title
                                ?: ("${stringResource(id = R.string.title_mega_info_empty_screen)} - " +
                                        stringResource(id = R.string.general_loading)),
                            color = grey_alpha_087.takeIf { isLight } ?: white_alpha_087,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.W500,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            style = MaterialTheme.typography.subtitle1,
                        )

                        if (album != null) {
                            Text(
                                text = stringResource(id = R.string.album_import_link),
                                color = grey_alpha_054.takeIf { isLight } ?: white_alpha_054,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.W400,
                            )
                        }
                    }
                },
            )
        },
        modifier = modifier,
        navigationIcon = {
            IconButton(
                onClick = onBack,
                content = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back_white),
                        contentDescription = null,
                        tint = accent_900.takeIf { hasSelectedPhotos }
                            ?: grey_alpha_087.takeIf { isLight } ?: white_alpha_087,
                    )
                },
            )
        },
        actions = {
            if (hasSelectedPhotos) {
                IconButton(
                    onClick = { showContextMenu = true },
                    content = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_dots_vertical_white),
                            contentDescription = null,
                            tint = accent_900,
                        )
                    },
                )

                DropdownMenu(
                    expanded = showContextMenu,
                    onDismissRequest = { showContextMenu = false },
                    content = {
                        if (selectedPhotos.size != photos.size) {
                            DropdownMenuItem(
                                onClick = {
                                    onSelectAllPhotos()
                                    showContextMenu = false
                                },
                                content = {
                                    Text(
                                        text = stringResource(id = R.string.action_select_all),
                                    )
                                },
                            )
                        }

                        DropdownMenuItem(
                            onClick = {
                                onClearSelection()
                                showContextMenu = false
                            },
                            content = {
                                Text(
                                    text = stringResource(id = R.string.action_unselect_all),
                                )
                            },
                        )
                    },
                )
            } else if (album != null) {
                IconButton(
                    onClick = onShareLink,
                    content = {
                        Icon(
                            painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.ShareNetwork),
                            contentDescription = null,
                            tint = grey_alpha_087.takeIf { isLight } ?: white_alpha_087,
                        )
                    },
                )
            }
        },
        elevation = 0.dp,
    )
}

@Composable
private fun AlbumImportBottomBar(
    modifier: Modifier = Modifier,
    isLogin: Boolean,
    onImport: () -> Unit,
    onSaveToDevice: () -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight

    Column(
        modifier = modifier,
        content = {
            Divider(
                color = grey_alpha_012.takeIf { isLight } ?: white_alpha_012,
                thickness = 1.dp,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.grey_020_grey_700)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = {
                    if (isLogin) {
                        TextMegaButton(
                            textId = R.string.general_save_to_cloud_drive,
                            onClick = {
                                Analytics.tracker.trackEvent(AlbumImportSaveToCloudDriveButtonEvent)
                                onImport()
                            },
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextMegaButton(
                        textId = R.string.general_save_to_device,
                        onClick = {
                            Analytics.tracker.trackEvent(AlbumImportSaveToDeviceButtonEvent)
                            onSaveToDevice()
                        },
                    )
                },
            )
        },
    )
}

@Composable
private fun AlbumImportContent(
    modifier: Modifier = Modifier,
    isLocalAlbumsLoaded: Boolean,
    isAvailableStorageCollected: Boolean,
    album: Album.UserAlbum?,
    photos: List<Photo>,
    selectedPhotos: Set<Photo>,
    onDownloadImage: ImageDownloader,
    onClickPhoto: (Photo) -> Unit,
    onPhotoSelection: (Photo) -> Unit,
    onAlbumLoaded: () -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(album != null && isLocalAlbumsLoaded) {
        if (album != null && isLocalAlbumsLoaded) {
            onAlbumLoaded()
        }
    }

    if (album != null && photos.isEmpty()) {
        LegacyMegaEmptyView(
            modifier = modifier,
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_photos_user_album_empty),
            text = stringResource(id = R.string.photos_user_album_empty_album),
        )
    } else if (isLocalAlbumsLoaded && isAvailableStorageCollected && album != null) {
        AlbumImportList(
            modifier = modifier,
            photos = photos,
            selectedPhotos = selectedPhotos,
            onDownloadImage = onDownloadImage,
            onClickPhoto = onClickPhoto,
            onPhotoSelection = onPhotoSelection,
        )
    }
}

@Composable
private fun AlbumImportList(
    modifier: Modifier = Modifier,
    photos: List<Photo>,
    selectedPhotos: Set<Photo>,
    onDownloadImage: ImageDownloader,
    onClickPhoto: (Photo) -> Unit,
    onPhotoSelection: (Photo) -> Unit,
) {
    val lazyListState = rememberLazyListState()

    val configuration = LocalConfiguration.current
    val smallWidth = remember(configuration) {
        (configuration.screenWidthDp.dp - 1.dp) / 3
    }

    DynamicView(
        lazyListState = lazyListState,
        photos = photos,
        smallWidth = smallWidth,
        photoDownload = onDownloadImage,
        onClick = onClickPhoto,
        onLongPress = onPhotoSelection,
        selectedPhotos = selectedPhotos,
        endSpacing = 64.dp,
    )
}

@Composable
private fun InputDecryptionKeyDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onDecrypt: (String) -> Unit,
    onDialogDisplayed: () -> Unit,
) {
    LaunchedEffect(onDialogDisplayed) {
        onDialogDisplayed()
    }

    val isLight = MaterialTheme.colors.isLight
    var text by rememberSaveable { mutableStateOf("") }

    MegaDialog(
        modifier = modifier,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
        onDismissRequest = onDismiss,
        titleString = stringResource(id = R.string.album_import_input_decryption_key_title),
        fontWeight = FontWeight.W500,
        body = {
            Column(
                content = {
                    Text(
                        text = stringResource(id = R.string.album_import_input_decryption_key_description),
                        color = grey_alpha_054.takeIf { isLight } ?: white_alpha_054,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W400,
                        style = MaterialTheme.typography.subtitle1,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    GenericTextField(
                        placeholder = "",
                        onTextChange = { text = it },
                        imeAction = ImeAction.Done,
                        keyboardActions = KeyboardActions.Default,
                        text = text,
                    )
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onDecrypt(text.trim()) },
                modifier = Modifier.alpha(0.4f.takeIf { text.isBlank() } ?: 1f),
                enabled = text.isNotBlank(),
                content = {
                    Text(
                        text = stringResource(id = R.string.general_decryp),
                        color = accent_900,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W500,
                        style = MaterialTheme.typography.button,
                    )
                },
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                content = {
                    Text(
                        text = stringResource(id = sharedR.string.general_dialog_cancel_button),
                        color = accent_900,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W500,
                        style = MaterialTheme.typography.button,
                    )
                },
            )
        },
    )
}

@Composable
private fun ErrorAccessDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight

    MegaDialog(
        modifier = modifier,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
        onDismissRequest = onDismiss,
        titleString = stringResource(id = R.string.album_import_error_access_title),
        fontWeight = FontWeight.W500,
        body = {
            Text(
                text = stringResource(id = R.string.album_import_error_access_description),
                color = grey_alpha_054.takeIf { isLight } ?: white_alpha_054,
                fontSize = 16.sp,
                fontWeight = FontWeight.W400,
                style = MaterialTheme.typography.subtitle1,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                content = {
                    Text(
                        text = stringResource(id = sharedResR.string.general_ok),
                        color = accent_900,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W500,
                        style = MaterialTheme.typography.button,
                    )
                },
            )
        },
        dismissButton = {},
    )
}

@Composable
private fun RenameAlbumDialog(
    modifier: Modifier = Modifier,
    album: Album.UserAlbum?,
    errorMessage: String?,
    onClearErrorMessage: () -> Unit,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight
    var text by rememberSaveable { mutableStateOf("") }

    MegaDialog(
        modifier = modifier.padding(36.dp),
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
        onDismissRequest = onDismiss,
        titleString = stringResource(id = R.string.album_import_rename_album_title),
        fontWeight = FontWeight.W500,
        body = {
            Column(
                content = {
                    Text(
                        text = stringResource(
                            id = R.string.album_import_rename_album_description,
                            album?.title.orEmpty(),
                        ),
                        color = grey_alpha_054.takeIf { isLight } ?: white_alpha_054,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W400,
                        style = MaterialTheme.typography.subtitle1,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    GenericTextField(
                        placeholder = "",
                        onTextChange = {
                            text = it
                            onClearErrorMessage()
                        },
                        imeAction = ImeAction.Done,
                        keyboardActions = KeyboardActions.Default,
                        text = text,
                        errorText = errorMessage,
                    )
                },
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                content = {
                    Text(
                        text = stringResource(id = sharedR.string.general_dialog_cancel_button),
                        color = accent_900,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W500,
                        style = MaterialTheme.typography.button,
                    )
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onRename(text.trim()) },
                content = {
                    Text(
                        text = stringResource(id = R.string.context_rename),
                        color = accent_900,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W500,
                        style = MaterialTheme.typography.button,
                    )
                },
            )
        },
    )
}

@Composable
private fun ImportAlbumDialog(
    modifier: Modifier = Modifier,
) {
    val isLight = MaterialTheme.colors.isLight

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        content = {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(4.dp),
                elevation = 24.dp,
                content = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        content = {
                            MegaCircularProgressIndicator(
                                modifier = Modifier.size(44.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = stringResource(id = R.string.album_import_saving),
                                color = grey_alpha_038.takeIf { isLight } ?: white_alpha_038,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.W400,
                                style = MaterialTheme.typography.body2,
                            )
                        },
                    )
                },
            )
        },
    )
}

@Composable
private fun StorageExceededDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onUpgradeAccount: () -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight

    MegaDialog(
        modifier = modifier,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
        onDismissRequest = onDismiss,
        titleString = stringResource(id = R.string.album_import_storage_exceeded_title),
        fontWeight = FontWeight.W400,
        body = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
                content = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_storage_full),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                    )
                },
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(id = R.string.album_import_storage_exceeded_description),
                color = grey_alpha_054.takeIf { isLight } ?: white_alpha_054,
                fontSize = 14.sp,
                fontWeight = FontWeight.W400,
                style = MaterialTheme.typography.subtitle2,
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                content = {
                    Text(
                        text = stringResource(id = sharedR.string.general_dialog_cancel_button),
                        color = accent_900,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W500,
                        style = MaterialTheme.typography.button,
                    )
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    Analytics.tracker.trackEvent(AlbumsStorageOverQuotaUpgradeAccountButtonEvent)
                    onUpgradeAccount()
                },
                content = {
                    Text(
                        text = stringResource(id = sharedR.string.general_upgrade_button),
                        color = accent_900,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W500,
                        style = MaterialTheme.typography.button,
                    )
                },
            )
        },
    )
}
