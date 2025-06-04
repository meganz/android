package mega.privacy.android.feature.sync.ui.newfolderpair

import android.Manifest
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.ui.megapicker.AllFilesAccessDialog
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.feature.sync.ui.renamebackup.model.RenameAndCreateBackupDialog
import mega.privacy.android.feature.sync.ui.views.InputSyncInformationView
import mega.privacy.android.feature.sync.ui.views.SyncTypePreviewProvider
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.banners.WarningBanner
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.text.MegaSpannedClickableText
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyle
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyleWithAnnotation
import mega.privacy.android.shared.original.core.ui.model.SpanIndicator
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePhoneLandscapePreviews
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.conditional
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.android.shared.resources.R as sharedResR
import mega.privacy.mobile.analytics.event.AndroidSyncAllFilesAccessDialogDisplayedEvent
import mega.privacy.mobile.analytics.event.AndroidSyncSelectDeviceFolderButtonPressedEvent

@Composable
internal fun SyncNewFolderScreen(
    selectedLocalFolder: String,
    selectedLocalFolderUri: String,
    selectedMegaFolder: RemoteFolder?,
    onSelectFolder: () -> Unit,
    selectMegaFolderClicked: () -> Unit,
    syncClicked: () -> Unit,
    syncPermissionsManager: SyncPermissionsManager,
    onBackClicked: () -> Unit,
    showStorageOverQuota: Boolean,
    onDismissStorageOverQuota: () -> Unit,
    onOpenUpgradeAccount: () -> Unit,
    viewModel: SyncNewFolderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SyncNewFolderScreenScaffold(
        state = state,
        selectedLocalFolder = selectedLocalFolder,
        selectedLocalFolderUri = selectedLocalFolderUri,
        selectedMegaFolder = selectedMegaFolder,
        onSelectFolder = onSelectFolder,
        selectMegaFolderClicked = selectMegaFolderClicked,
        syncClicked = syncClicked,
        syncPermissionsManager = syncPermissionsManager,
        onBackClicked = onBackClicked,
        showStorageOverQuota = showStorageOverQuota,
        onDismissStorageOverQuota = onDismissStorageOverQuota,
        onDismissRenameAndCreateBackupDialog = { viewModel.onShowRenameAndCreateBackupDialogConsumed() },
        onRenameAndCreateBackupSucceeded = { viewModel.openSyncListScreen() },
        onOpenUpgradeAccount = onOpenUpgradeAccount,
        onShowSnackbarConsumed = { viewModel.onShowSnackbarConsumed() },
    )
}

@Composable
private fun SyncNewFolderScreenScaffold(
    state: SyncNewFolderState,
    selectedLocalFolder: String,
    selectedLocalFolderUri: String,
    selectedMegaFolder: RemoteFolder?,
    onSelectFolder: () -> Unit,
    selectMegaFolderClicked: () -> Unit,
    syncClicked: () -> Unit,
    syncPermissionsManager: SyncPermissionsManager,
    onBackClicked: () -> Unit,
    showStorageOverQuota: Boolean,
    onDismissStorageOverQuota: () -> Unit,
    onDismissRenameAndCreateBackupDialog: () -> Unit,
    onRenameAndCreateBackupSucceeded: () -> Unit,
    onOpenUpgradeAccount: () -> Unit,
    onShowSnackbarConsumed: () -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val syncType = state.syncType
    var isWarningBannerDisplayed by rememberSaveable { mutableStateOf(false) }

    MegaScaffold(
        scaffoldState = scaffoldState,
        shouldAddSnackBarPadding = false,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            MegaAppBar(
                modifier = Modifier.testTag(TAG_SYNC_NEW_FOLDER_SCREEN_TOOLBAR),
                appBarType = AppBarType.BACK_NAVIGATION,
                title = when (syncType) {
                    SyncType.TYPE_BACKUP -> stringResource(id = sharedResR.string.sync_add_new_backup_toolbar_title)
                    else -> stringResource(R.string.sync_toolbar_title)
                },
                onNavigationPressed = { onBackClicked() },
                windowInsets = WindowInsets(0.dp),
                elevation = if (isWarningBannerDisplayed) AppBarDefaults.TopAppBarElevation else 0.dp,
            )
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                SyncNewFolderScreenContent(
                    syncType = syncType,
                    deviceName = state.deviceName,
                    onSelectFolder = onSelectFolder,
                    selectMegaFolderClicked = selectMegaFolderClicked,
                    selectedLocalFolder = selectedLocalFolder,
                    selectedLocalFolderUri = selectedLocalFolderUri,
                    selectedMegaFolder = selectedMegaFolder,
                    syncClicked = syncClicked,
                    syncPermissionsManager = syncPermissionsManager,
                    showStorageOverQuota = showStorageOverQuota,
                    onDismissStorageOverQuota = onDismissStorageOverQuota,
                    showRenameAndCreateBackupDialog = state.showRenameAndCreateBackupDialog,
                    onDismissRenameAndCreateBackupDialog = onDismissRenameAndCreateBackupDialog,
                    onRenameAndCreateBackupSucceeded = onRenameAndCreateBackupSucceeded,
                    onOpenUpgradeAccount = onOpenUpgradeAccount,
                    onShowSyncPermissionBannerValueChanged = { value ->
                        isWarningBannerDisplayed = value
                    },
                    snackBarHostState = scaffoldState.snackbarHostState,
                )

                val context = LocalContext.current
                EventEffect(
                    event = state.showSnackbar,
                    onConsumed = { onShowSnackbarConsumed() },
                ) { stringId ->
                    stringId?.let {
                        scaffoldState.snackbarHostState.showAutoDurationSnackbar(
                            context.getString(stringId)
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun SyncNewFolderScreenContent(
    syncType: SyncType,
    deviceName: String,
    onSelectFolder: () -> Unit,
    selectMegaFolderClicked: () -> Unit,
    selectedLocalFolder: String,
    selectedLocalFolderUri: String,
    selectedMegaFolder: RemoteFolder?,
    syncClicked: () -> Unit,
    syncPermissionsManager: SyncPermissionsManager,
    showStorageOverQuota: Boolean,
    onDismissStorageOverQuota: () -> Unit,
    showRenameAndCreateBackupDialog: String?,
    onDismissRenameAndCreateBackupDialog: () -> Unit,
    onRenameAndCreateBackupSucceeded: () -> Unit,
    onOpenUpgradeAccount: () -> Unit,
    onShowSyncPermissionBannerValueChanged: (Boolean) -> Unit,
    snackBarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showSyncPermissionBanner by rememberSaveable {
        mutableStateOf(false)
    }
    var showAllowAppAccessDialog by rememberSaveable {
        mutableStateOf(false)
    }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var proceedButtonClicked by remember { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
    ) {
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                runCatching {
                    onSelectFolder()
                }.onFailure {
                    coroutineScope.launch {
                        snackBarHostState.showAutoDurationSnackbar(context.getString(sharedResR.string.general_no_picker_warning))
                    }
                }
            } else {
                showSyncPermissionBanner = true
                onShowSyncPermissionBannerValueChanged(true)
            }
        }

        AnimatedVisibility(showSyncPermissionBanner) {
            WarningBanner(
                textComponent = {
                    when (syncType) {
                        SyncType.TYPE_BACKUP -> {
                            MegaSpannedClickableText(
                                value = stringResource(id = sharedResR.string.sync_add_new_backup_storage_permission_banner),
                                styles = mapOf(
                                    SpanIndicator('U') to MegaSpanStyleWithAnnotation(
                                        megaSpanStyle = MegaSpanStyle(
                                            spanStyle = SpanStyle(
                                                textDecoration = TextDecoration.Underline
                                            )
                                        ),
                                        annotation = "Tap to grant access",
                                    )
                                ),
                                color = TextColor.Primary,
                                onAnnotationClick = {
                                    syncPermissionsManager.launchAppSettingFileStorageAccess()
                                    showSyncPermissionBanner = false
                                    onShowSyncPermissionBannerValueChanged(false)
                                },
                            )
                        }

                        else -> {
                            MegaText(
                                text = stringResource(id = R.string.sync_storage_permission_banner),
                                textColor = TextColor.Primary,
                            )
                        }
                    }

                },
                onCloseClick = null,
                modifier = Modifier.clickable {
                    syncPermissionsManager.launchAppSettingFileStorageAccess()
                    showSyncPermissionBanner = false
                    onShowSyncPermissionBannerValueChanged(false)
                })
        }

        if (showAllowAppAccessDialog) {
            AllFilesAccessDialog(onConfirm = {
                syncPermissionsManager.launchAppSettingFileStorageAccess()
                showAllowAppAccessDialog = false
            }, onDismiss = {
                showSyncPermissionBanner = true
                onShowSyncPermissionBannerValueChanged(true)
                showAllowAppAccessDialog = false
            })
        }

        if (showStorageOverQuota) {
            MegaAlertDialog(
                title = stringResource(sharedResR.string.sync_error_dialog_insufficient_storage_title),
                text = stringResource(sharedResR.string.sync_error_dialog_insufficient_storage_body),
                confirmButtonText = stringResource(sharedResR.string.sync_error_dialog_insufficient_storage_confirm_button),
                cancelButtonText = stringResource(sharedResR.string.sync_error_dialog_insufficient_storage_cancel_button),
                onConfirm = {
                    onDismissStorageOverQuota()
                    onOpenUpgradeAccount()
                },
                onDismiss = {
                    onDismissStorageOverQuota()
                }
            )
        }

        showRenameAndCreateBackupDialog?.let { folderPairName ->
            RenameAndCreateBackupDialog(
                backupName = folderPairName,
                localPath = selectedLocalFolderUri,
                onSuccess = {
                    onDismissRenameAndCreateBackupDialog()
                    onRenameAndCreateBackupSucceeded()
                },
                onCancel = {
                    proceedButtonClicked = false
                    onDismissRenameAndCreateBackupDialog()
                },
            )
        }

        val isLandscape =
            LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        Column(modifier = Modifier.conditional(isLandscape) { fillMaxWidth(0.45f).align(Alignment.CenterHorizontally) }) {
            MegaText(
                text = when (syncType) {
                    SyncType.TYPE_BACKUP -> stringResource(id = sharedResR.string.sync_add_new_backup_header_text)
                    else -> stringResource(id = sharedResR.string.sync_add_new_sync_folder_header_text)
                },
                textColor = TextColor.Primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp),
                style = MaterialTheme.typography.subtitle2
            )

            InputSyncInformationView(
                syncType = syncType,
                deviceName = deviceName,
                selectDeviceFolderClicked = {
                    Analytics.tracker.trackEvent(AndroidSyncSelectDeviceFolderButtonPressedEvent)
                    if (syncPermissionsManager.isManageExternalStoragePermissionGranted()) {
                        runCatching {
                            onSelectFolder()
                        }.onFailure {
                            coroutineScope.launch {
                                snackBarHostState.showAutoDurationSnackbar(
                                    context.getString(
                                        sharedResR.string.general_no_picker_warning
                                    )
                                )
                            }
                        }
                    } else {
                        if (showSyncPermissionBanner) {
                            syncPermissionsManager.launchAppSettingFileStorageAccess()
                            showSyncPermissionBanner = false
                            onShowSyncPermissionBannerValueChanged(false)
                        } else {
                            if (syncPermissionsManager.isSDKAboveOrEqualToR()) {
                                showAllowAppAccessDialog = true
                                Analytics.tracker.trackEvent(
                                    AndroidSyncAllFilesAccessDialogDisplayedEvent
                                )
                            } else {
                                launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            }
                        }
                    }
                },
                selectMegaFolderClicked = {
                    selectMegaFolderClicked()
                },
                selectedDeviceFolder = selectedLocalFolder,
                selectedMegaFolder = selectedMegaFolder?.name ?: ""
            )

            Box(
                Modifier
                    .fillMaxWidth()
                    .testTag(TAG_SYNC_NEW_FOLDER_SCREEN_SYNC_BUTTON),
                contentAlignment = Alignment.Center
            ) {
                val buttonEnabled = when (syncType) {
                    SyncType.TYPE_BACKUP -> selectedLocalFolder.isNotBlank() && syncPermissionsManager.isManageExternalStoragePermissionGranted()
                    else -> selectedLocalFolder.isNotBlank() && selectedMegaFolder != null && syncPermissionsManager.isManageExternalStoragePermissionGranted()
                }

                RaisedDefaultMegaButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                    textId = when (syncType) {
                        SyncType.TYPE_BACKUP -> {
                            if (proceedButtonClicked) {
                                sharedResR.string.sync_list_sync_state_updating
                            } else {
                                sharedResR.string.sync_add_new_backup_proceed_button_label
                            }
                        }

                        else -> {
                            if (proceedButtonClicked) {
                                R.string.sync_list_sync_state_syncing
                            } else {
                                R.string.sync_button_label
                            }
                        }
                    },
                    onClick = {
                        proceedButtonClicked = true
                        syncClicked()
                    },
                    enabled = buttonEnabled && proceedButtonClicked.not()
                )
            }
        }
    }
}

internal const val TAG_SYNC_NEW_FOLDER_SCREEN_TOOLBAR = "sync_new_folder_screen_toolbar_test_tag"
internal const val TAG_SYNC_NEW_FOLDER_SCREEN_SYNC_BUTTON =
    "sync_new_folder_screen_sync_button_test_tag"

@CombinedThemePreviews
@CombinedThemePhoneLandscapePreviews
@Composable
private fun SyncNewFolderScreenPreview(
    @PreviewParameter(SyncTypePreviewProvider::class) syncType: SyncType
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SyncNewFolderScreenScaffold(
            state = SyncNewFolderState(
                syncType = syncType,
                deviceName = "Device Name",
            ),
            selectedLocalFolder = "",
            selectedLocalFolderUri = "",
            selectedMegaFolder = null,
            onSelectFolder = {},
            selectMegaFolderClicked = {},
            syncClicked = {},
            syncPermissionsManager = SyncPermissionsManager(LocalContext.current),
            showStorageOverQuota = false,
            onDismissStorageOverQuota = {},
            onDismissRenameAndCreateBackupDialog = {},
            onRenameAndCreateBackupSucceeded = {},
            onOpenUpgradeAccount = {},
            onBackClicked = {},
            onShowSnackbarConsumed = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SyncNewFolderScreenContentPreview(
    @PreviewParameter(SyncTypePreviewProvider::class) syncType: SyncType
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SyncNewFolderScreenContent(
            syncType = syncType,
            deviceName = "Device Name",
            selectedLocalFolder = "",
            selectedLocalFolderUri = "",
            selectedMegaFolder = null,
            onSelectFolder = {},
            selectMegaFolderClicked = {},
            syncClicked = {},
            syncPermissionsManager = SyncPermissionsManager(LocalContext.current),
            showStorageOverQuota = false,
            onDismissStorageOverQuota = {},
            showRenameAndCreateBackupDialog = null,
            onDismissRenameAndCreateBackupDialog = {},
            onRenameAndCreateBackupSucceeded = {},
            onOpenUpgradeAccount = {},
            onShowSyncPermissionBannerValueChanged = {},
            snackBarHostState = rememberScaffoldState().snackbarHostState,
        )
    }
}
