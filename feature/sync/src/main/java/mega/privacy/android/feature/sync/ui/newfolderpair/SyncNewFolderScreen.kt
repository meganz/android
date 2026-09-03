package mega.privacy.android.feature.sync.ui.newfolderpair

import android.Manifest
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.banner.TopWarningBanner
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.dialogs.BasicDialogButton
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.model.SpanStyleWithAnnotation
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.ui.extension.showAutoDurationSnackbar
import mega.privacy.android.feature.sync.ui.megapicker.AllFilesAccessDialog
import mega.privacy.android.feature.sync.ui.preview.CombinedThemePhoneLandscapePreviews
import mega.privacy.android.feature.sync.ui.renamebackup.model.RenameAndCreateBackupDialog
import mega.privacy.android.feature.sync.ui.views.InputSyncInformationView
import mega.privacy.android.feature.sync.ui.views.SyncStorageQuotaExceedWarning
import mega.privacy.android.feature.sync.ui.views.SyncTypePreviewProvider
import mega.privacy.android.shared.original.core.ui.theme.extensions.conditional
import mega.privacy.android.shared.resources.R as sharedResR
import mega.privacy.android.shared.sync.ui.permissions.SyncPermissionsManager
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
internal fun SyncNewFolderScreenScaffold(
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
    val syncType = state.syncType
    var isWarningBannerDisplayed by rememberSaveable { mutableStateOf(false) }
    val snackBarHostState = LocalSnackBarHostState.current

    MegaScaffold(
        topBar = {
            MegaTopAppBar(
                modifier = Modifier.testTag(TAG_SYNC_NEW_FOLDER_SCREEN_TOOLBAR),
                title = when (syncType) {
                    SyncType.TYPE_BACKUP -> stringResource(id = sharedResR.string.sync_add_new_backup_toolbar_title)
                    else -> stringResource(R.string.sync_toolbar_title)
                },
                navigationType = AppBarNavigationType.Back {
                    if (state.isLoading.not()) {
                        onBackClicked()
                    }
                },
                // M3 draws the separator from scroll state rather than a fixed elevation.
                drawBottomLineOnScrolledContent = isWarningBannerDisplayed,
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
                    snackBarHostState = snackBarHostState,
                    isStorageOverQuota = state.isStorageOverQuota
                )

                val context = LocalContext.current
                EventEffect(
                    event = state.showSnackbar,
                    onConsumed = { onShowSnackbarConsumed() },
                ) { message ->
                    message?.let {
                        snackBarHostState?.showAutoDurationSnackbar(it.get(context))
                    }
                }
            }
        },
    )
}

@Composable
internal fun SyncNewFolderScreenContent(
    syncType: SyncType,
    deviceName: String,
    isStorageOverQuota: Boolean,
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
    snackBarHostState: SnackbarHostState?,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current
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
                        snackBarHostState?.showAutoDurationSnackbar(resources.getString(sharedResR.string.general_no_picker_warning))
                    }
                }
            } else {
                showSyncPermissionBanner = true
                onShowSyncPermissionBannerValueChanged(true)
            }
        }

        if (isStorageOverQuota) {
            SyncStorageQuotaExceedWarning(onUpgradeClick = onOpenUpgradeAccount)
        } else {
            AnimatedVisibility(showSyncPermissionBanner) {
                TopWarningBanner(
                    body = when (syncType) {
                        SyncType.TYPE_BACKUP -> SpannableText(
                            text = stringResource(id = sharedResR.string.sync_add_new_backup_storage_permission_banner),
                            annotations = mapOf(
                                SpanIndicator('U') to SpanStyleWithAnnotation(
                                    megaSpanStyle = MegaSpanStyle.TextColorStyle(
                                        spanStyle = SpanStyle(textDecoration = TextDecoration.Underline),
                                        textColor = TextColor.Primary,
                                    ),
                                    annotation = "Tap to grant access",
                                )
                            ),
                            onAnnotationClick = {
                                syncPermissionsManager.launchAppSettingFileStorageAccess()
                                showSyncPermissionBanner = false
                                onShowSyncPermissionBannerValueChanged(false)
                            },
                        )

                        else -> SpannableText(
                            text = stringResource(id = R.string.sync_storage_permission_banner),
                        )
                    },
                    showCancelButton = false,
                    modifier = Modifier.clickable {
                        syncPermissionsManager.launchAppSettingFileStorageAccess()
                        showSyncPermissionBanner = false
                        onShowSyncPermissionBannerValueChanged(false)
                    },
                )
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
        }

        if (showStorageOverQuota) {
            BasicDialog(
                title = stringResource(sharedResR.string.sync_error_dialog_insufficient_storage_title),
                description = stringResource(sharedResR.string.sync_error_dialog_insufficient_storage_body),
                buttons = persistentListOf(
                    BasicDialogButton(
                        text = stringResource(sharedResR.string.sync_error_dialog_insufficient_storage_cancel_button),
                        onClick = { onDismissStorageOverQuota() },
                    ),
                    BasicDialogButton(
                        text = stringResource(sharedResR.string.sync_error_dialog_insufficient_storage_confirm_button),
                        onClick = {
                            onDismissStorageOverQuota()
                            onOpenUpgradeAccount()
                        },
                    ),
                ),
                onDismissRequest = { onDismissStorageOverQuota() },
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
                style = AppTheme.typography.titleSmall
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
                                snackBarHostState?.showAutoDurationSnackbar(
                                    resources.getString(
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

                PrimaryFilledButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(
                        when (syncType) {
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
                        }
                    ),
                    onClick = {
                        proceedButtonClicked = true
                        syncClicked()
                    },
                    enabled = buttonEnabled && proceedButtonClicked.not() && isStorageOverQuota.not()
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
    @PreviewParameter(SyncTypePreviewProvider::class) syncType: SyncType,
) {
    AndroidThemeForPreviews {
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
    @PreviewParameter(SyncTypePreviewProvider::class) syncType: SyncType,
) {
    AndroidThemeForPreviews {
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
            snackBarHostState = null,
            isStorageOverQuota = true
        )
    }
}
