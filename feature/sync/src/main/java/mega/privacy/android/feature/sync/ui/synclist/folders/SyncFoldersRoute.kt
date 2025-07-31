package mega.privacy.android.feature.sync.ui.synclist.folders

import android.content.Intent
import android.provider.DocumentsContract
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.ui.stopbackup.StopBackupConfirmationDialog
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.OnRemoveFolderDialogDismissed
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.OnRemoveSyncFolderDialogConfirmed
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.PauseRunClicked
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.RemoveFolderClicked
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.SnackBarShown
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.android.shared.resources.R as sharedResR

@Composable
internal fun SyncFoldersRoute(
    onAddNewSyncClicked: () -> Unit,
    onAddNewBackupClicked: () -> Unit,
    onSelectStopBackupDestinationClicked: (String?) -> Unit,
    issuesInfoClicked: () -> Unit,
    onOpenMegaFolderClicked: (handle: Long) -> Unit,
    onCameraUploadsSettingsClicked: () -> Unit,
    viewModel: SyncFoldersViewModel,
    uiState: SyncFoldersUiState,
    snackBarHostState: SnackbarHostState,
    deviceName: String,
) {
    val context = LocalContext.current

    SyncFoldersScreen(
        syncUiItems = uiState.syncUiItems,
        cardExpanded = viewModel::handleAction,
        pauseRunClicked = {
            viewModel.handleAction(PauseRunClicked(it))
        },
        removeFolderClicked = {
            viewModel.handleAction(RemoveFolderClicked(it))
        },
        onAddNewSyncClicked = onAddNewSyncClicked,
        onAddNewBackupClicked = onAddNewBackupClicked,
        issuesInfoClicked = issuesInfoClicked,
        onOpenDeviceFolderClicked = { deviceStoragePath ->
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(
                        deviceStoragePath.toUri(),
                        DocumentsContract.Document.MIME_TYPE_DIR,
                    )
                }
            )
        },
        onOpenMegaFolderClicked = { syncUiItem ->
            onOpenMegaFolderClicked(syncUiItem.megaStorageNodeId.longValue)
        },
        onCameraUploadsSettingsClicked = onCameraUploadsSettingsClicked,
        isLowBatteryLevel = uiState.isLowBatteryLevel,
        isStorageOverQuota = uiState.isStorageOverQuota,
        isLoading = uiState.isLoading,
        deviceName = deviceName,
        onLocalFolderSelected = { sync, uri ->
            viewModel.handleAction(
                SyncFoldersAction.LocalFolderSelected(
                    syncUiItem = sync,
                    uri = uri,
                )
            )
        },
    )

    uiState.syncUiItemToRemove?.let { syncUiItemToRemove ->
        if (uiState.showConfirmRemoveSyncFolderDialog) {
            when (syncUiItemToRemove.syncType) {
                SyncType.TYPE_BACKUP -> {
                    StopBackupConfirmationDialog(
                        onConfirm = { selectedOption, selectedFolder ->
                            viewModel.handleAction(
                                SyncFoldersAction.OnRemoveBackupFolderDialogConfirmed(
                                    stopBackupOption = selectedOption,
                                    selectedFolder = selectedFolder,
                                )
                            )
                        },
                        onDismiss = {
                            viewModel.handleAction(OnRemoveFolderDialogDismissed)
                        },
                        onSelectStopBackupDestinationClicked = onSelectStopBackupDestinationClicked,
                        folderName = syncUiItemToRemove.folderPairName,
                    )
                }

                else -> {
                    StopSyncConfirmDialog(
                        onConfirm = {
                            viewModel.handleAction(OnRemoveSyncFolderDialogConfirmed)
                        },
                        onDismiss = {
                            viewModel.handleAction(OnRemoveFolderDialogDismissed)
                        },
                    )
                }
            }
        }
    }

    LaunchedEffect(key1 = uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { resId ->
            val message =
                uiState.snackbarMessage.let { context.getString(resId, uiState.movedFolderName) }
            snackBarHostState.showAutoDurationSnackbar(message)
            viewModel.handleAction(SnackBarShown)
        }
    }
}

@Composable
internal fun StopSyncConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        title = stringResource(id = sharedResR.string.sync_stop_sync_confirm_dialog_title),
        text = stringResource(id = sharedResR.string.sync_stop_sync_confirm_dialog_message),
        confirmButtonText = stringResource(id = sharedResR.string.sync_stop_sync_button),
        cancelButtonText = stringResource(id = sharedResR.string.general_dialog_cancel_button),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = Modifier.testTag(STOP_SYNC_CONFIRM_DIALOG_TEST_TAG),
    )
}

@CombinedThemePreviews
@Composable
private fun RemoveSyncFolderConfirmDialogPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        StopSyncConfirmDialog(
            onConfirm = {},
            onDismiss = {},
        )
    }
}

internal const val STOP_SYNC_CONFIRM_DIALOG_TEST_TAG =
    "sync:stop_sync:confirm_dialog"
