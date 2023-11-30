package mega.privacy.android.feature.sync.ui.newfolderpair

import android.Manifest
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.appbar.AppBarType
import mega.privacy.android.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.core.ui.controls.banners.WarningBanner
import mega.privacy.android.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.core.ui.navigation.launchFolderPicker
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.ui.megapicker.AllFilesAccessDialog
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.feature.sync.ui.views.InputSyncInformationView

@Composable
internal fun SyncNewFolderScreen(
    folderPairName: String,
    selectedLocalFolder: String,
    selectedMegaFolder: RemoteFolder?,
    localFolderSelected: (Uri) -> Unit,
    folderNameChanged: (String) -> Unit,
    selectMegaFolderClicked: () -> Unit,
    syncClicked: () -> Unit,
    syncPermissionsManager: SyncPermissionsManager,
) {
    val onBackPressedDispatcher =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    Scaffold(
        topBar = {
            MegaAppBar(
                modifier = Modifier.testTag(TAG_SYNC_NEW_FOLDER_SCREEN_TOOLBAR),
                appBarType = AppBarType.BACK_NAVIGATION,
                title = stringResource(R.string.sync_toolbar_title),
                subtitle = "Choose folders",
                onNavigationPressed = { onBackPressedDispatcher?.onBackPressed() },
                elevation = 0.dp
            )
        }, content = { paddingValues ->
            SyncNewFolderScreenContent(
                modifier = Modifier.padding(paddingValues),
                localFolderSelected = localFolderSelected,
                selectMegaFolderClicked = selectMegaFolderClicked,
                folderNameChanged = folderNameChanged,
                folderPairName = folderPairName,
                selectedLocalFolder = selectedLocalFolder,
                selectedMegaFolder = selectedMegaFolder,
                syncClicked = syncClicked,
                syncPermissionsManager = syncPermissionsManager
            )
        }
    )
}

@Composable
private fun SyncNewFolderScreenContent(
    modifier: Modifier = Modifier,
    localFolderSelected: (Uri) -> Unit,
    selectMegaFolderClicked: () -> Unit,
    folderNameChanged: (String) -> Unit,
    folderPairName: String,
    selectedLocalFolder: String,
    selectedMegaFolder: RemoteFolder?,
    syncClicked: () -> Unit,
    syncPermissionsManager: SyncPermissionsManager,
) {
    var showSyncPermissionBanner by rememberSaveable {
        mutableStateOf(false)
    }
    var showAllowAppAccessDialog by rememberSaveable {
        mutableStateOf(false)
    }

    Column(modifier) {
        val folderPicker = launchFolderPicker {
            localFolderSelected(it)
        }

        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                folderPicker.launch(null)
            } else {
                showSyncPermissionBanner = true
            }
        }

        AnimatedVisibility(showSyncPermissionBanner) {
            WarningBanner(
                textString = stringResource(id = R.string.sync_storage_permission_banner),
                onCloseClick = null,
                modifier = Modifier.clickable {
                    syncPermissionsManager.launchAppSettingFileStorageAccess()
                    showSyncPermissionBanner = false
                }
            )
        }

        if (showAllowAppAccessDialog) {
            AllFilesAccessDialog(
                onConfirm = {
                    syncPermissionsManager.launchAppSettingFileStorageAccess()
                    showAllowAppAccessDialog = false
                },
                onDismiss = {
                    showSyncPermissionBanner = true
                    showAllowAppAccessDialog = false
                }
            )
        }

        InputSyncInformationView(
            Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 48.dp),
            selectDeviceFolderClicked = {
                if (syncPermissionsManager.isManageExternalStoragePermissionGranted()) {
                    folderPicker.launch(null)
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
            selectMEGAFolderClicked = {
                selectMegaFolderClicked()
            },
            onFolderPairNameChanged = { folderPairName ->
                folderNameChanged(folderPairName)
            },
            folderPairName,
            selectedLocalFolder,
            selectedMegaFolder?.name ?: ""
        )

        Box(
            Modifier
                .fillMaxWidth()
                .testTag(TAG_SYNC_NEW_FOLDER_SCREEN_SYNC_BUTTON),
            contentAlignment = Alignment.Center
        ) {
            val buttonEnabled =
                selectedLocalFolder.isNotBlank()
                        && selectedMegaFolder != null
                        && syncPermissionsManager.isManageExternalStoragePermissionGranted()

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

internal const val TAG_SYNC_NEW_FOLDER_SCREEN_TOOLBAR =
    "sync_new_folder_screen_toolbar_test_tag"
internal const val TAG_SYNC_NEW_FOLDER_SCREEN_SYNC_BUTTON =
    "sync_new_folder_screen_sync_button_test_tag"

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSyncNewFolderScreen() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        SyncNewFolderScreen(
            folderPairName = "",
            selectedLocalFolder = "",
            selectedMegaFolder = null,
            localFolderSelected = {},
            folderNameChanged = {},
            selectMegaFolderClicked = {},
            syncClicked = {},
            syncPermissionsManager = SyncPermissionsManager(LocalContext.current)
        )
    }
}

