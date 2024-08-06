package mega.privacy.android.feature.sync.ui.newfolderpair

import mega.privacy.android.shared.resources.R as sharedResR
import android.Manifest
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.ui.megapicker.AllFilesAccessDialog
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.feature.sync.ui.views.InputSyncInformationView
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.banners.WarningBanner
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.navigation.launchFolderPicker
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar

@Composable
internal fun SyncNewFolderScreen(
    selectedLocalFolder: String,
    selectedMegaFolder: RemoteFolder?,
    localFolderSelected: (Uri) -> Unit,
    selectMegaFolderClicked: () -> Unit,
    syncClicked: () -> Unit,
    syncPermissionsManager: SyncPermissionsManager,
    onBackClicked: () -> Unit,
    showStorageOverQuota: Boolean,
    onDismissStorageOverQuota: () -> Unit,
    onOpenUpgradeAccount: () -> Unit,
    viewModel: SyncNewFolderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val scaffoldState = rememberScaffoldState()

    MegaScaffold(
        scaffoldState = scaffoldState,
        topBar = {
            MegaAppBar(
                modifier = Modifier.testTag(TAG_SYNC_NEW_FOLDER_SCREEN_TOOLBAR),
                appBarType = AppBarType.BACK_NAVIGATION,
                title = stringResource(R.string.sync_toolbar_title),
                onNavigationPressed = { onBackClicked() },
                elevation = 0.dp
            )
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                SyncNewFolderScreenContent(
                    localFolderSelected = localFolderSelected,
                    selectMegaFolderClicked = selectMegaFolderClicked,
                    selectedLocalFolder = selectedLocalFolder,
                    selectedMegaFolder = selectedMegaFolder,
                    syncClicked = syncClicked,
                    syncPermissionsManager = syncPermissionsManager,
                    showStorageOverQuota = showStorageOverQuota,
                    onDismissStorageOverQuota = onDismissStorageOverQuota,
                    onOpenUpgradeAccount = onOpenUpgradeAccount,
                    snackBarHostState = scaffoldState.snackbarHostState,
                )

                val context = LocalContext.current
                EventEffect(
                    event = uiState.showSnackbar,
                    onConsumed = { viewModel.onShowSnackbarConsumed() },
                ) { stringId ->
                    stringId?.let {
                        scaffoldState.snackbarHostState.showAutoDurationSnackbar(context.getString(stringId))
                    }
                }
            }
        },
    )
}

@Composable
private fun SyncNewFolderScreenContent(
    localFolderSelected: (Uri) -> Unit,
    selectMegaFolderClicked: () -> Unit,
    selectedLocalFolder: String,
    selectedMegaFolder: RemoteFolder?,
    syncClicked: () -> Unit,
    syncPermissionsManager: SyncPermissionsManager,
    showStorageOverQuota: Boolean,
    onDismissStorageOverQuota: () -> Unit,
    onOpenUpgradeAccount: () -> Unit,
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
    Column(
        modifier.verticalScroll(scrollState)
    ) {
        val folderPicker = launchFolderPicker(
            Uri.parse(ROOT_FOLDER_URI_STRING)
        ) {
            localFolderSelected(it)
        }
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                runCatching {
                    folderPicker.launch(null)
                }.onFailure {
                    coroutineScope.launch {
                        snackBarHostState.showAutoDurationSnackbar(context.getString(sharedResR.string.general_no_picker_warning))
                    }
                }
            } else {
                showSyncPermissionBanner = true
            }
        }

        AnimatedVisibility(showSyncPermissionBanner) {
            WarningBanner(textString = stringResource(id = R.string.sync_storage_permission_banner),
                onCloseClick = null,
                modifier = Modifier.clickable {
                    syncPermissionsManager.launchAppSettingFileStorageAccess()
                    showSyncPermissionBanner = false
                })
        }

        if (showAllowAppAccessDialog) {
            AllFilesAccessDialog(onConfirm = {
                syncPermissionsManager.launchAppSettingFileStorageAccess()
                showAllowAppAccessDialog = false
            }, onDismiss = {
                showSyncPermissionBanner = true
                showAllowAppAccessDialog = false
            })
        }

        MegaText(
            text = stringResource(id = sharedResR.string.sync_add_new_sync_folder_header_text),
            textColor = TextColor.Primary,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp),
            style = MaterialTheme.typography.subtitle2
        )

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

        InputSyncInformationView(
            selectDeviceFolderClicked = {
                if (syncPermissionsManager.isManageExternalStoragePermissionGranted()) {
                    runCatching {
                        folderPicker.launch(null)
                    }.onFailure {
                        coroutineScope.launch {
                            snackBarHostState.showAutoDurationSnackbar(context.getString(sharedResR.string.general_no_picker_warning))
                        }
                    }
                } else {
                    if (showSyncPermissionBanner) {
                        syncPermissionsManager.launchAppSettingFileStorageAccess()
                        showSyncPermissionBanner = false
                    } else {
                        if (syncPermissionsManager.isSDKAboveOrEqualToR()) {
                            showAllowAppAccessDialog = true
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
            val buttonEnabled =
                selectedLocalFolder.isNotBlank() && selectedMegaFolder != null && syncPermissionsManager.isManageExternalStoragePermissionGranted()

            RaisedDefaultMegaButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                textId = R.string.sync_button_label,
                onClick = syncClicked,
                enabled = buttonEnabled
            )
        }
    }
}

private const val ROOT_FOLDER_URI_STRING =
    "content://com.android.externalstorage.documents/root/primary"
internal const val TAG_SYNC_NEW_FOLDER_SCREEN_TOOLBAR = "sync_new_folder_screen_toolbar_test_tag"
internal const val TAG_SYNC_NEW_FOLDER_SCREEN_SYNC_BUTTON =
    "sync_new_folder_screen_sync_button_test_tag"

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSyncNewFolderScreen() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        SyncNewFolderScreen(
            selectedLocalFolder = "",
            selectedMegaFolder = null,
            localFolderSelected = {},
            selectMegaFolderClicked = {},
            syncClicked = {},
            syncPermissionsManager = SyncPermissionsManager(LocalContext.current),
            showStorageOverQuota = false,
            onDismissStorageOverQuota = {},
            onOpenUpgradeAccount = {},
            onBackClicked = {},
        )
    }
}

