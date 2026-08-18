package mega.privacy.android.feature.sync.ui.synclist.folders

import android.content.Intent
import android.provider.DocumentsContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.ui.stopbackup.StopBackupConfirmationDialog
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.OnRemoveFolderDialogDismissed
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.OnRemoveSyncFolderDialogConfirmed
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.PauseRunClicked
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.RemoveFolderClicked
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.SnackBarShown
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.sync.ui.extension.showAutoDurationSnackbar
import mega.privacy.android.shared.resources.R as sharedResR

@Composable
internal fun SyncFoldersRoute(
    onAddNewSyncClicked: () -> Unit,
    onAddNewBackupClicked: () -> Unit,
    onSelectStopBackupDestinationClicked: (String?) -> Unit,
    issuesInfoClicked: () -> Unit,
    onOpenMegaFolderClicked: (handle: Long) -> Unit,
    onCameraUploadsSettingsClicked: () -> Unit,
    onAction: (SyncFoldersAction) -> Unit,
    uiState: SyncFoldersUiState,
    deviceName: String,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbarScope = rememberCoroutineScope()
    val snackBarHostState = LocalSnackBarHostState.current

    SyncFoldersScreen(
        syncUiItems = uiState.syncUiItems,
        cardExpanded = onAction,
        pauseRunClicked = {
            onAction(PauseRunClicked(it))
        },
        removeFolderClicked = {
            onAction(RemoveFolderClicked(it))
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
        syncPauseReason = uiState.syncPauseReason,
        onLocalFolderSelected = { sync, uri ->
            onAction(
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
                            onAction(
                                SyncFoldersAction.OnRemoveBackupFolderDialogConfirmed(
                                    stopBackupOption = selectedOption,
                                    selectedFolder = selectedFolder,
                                )
                            )
                        },
                        onDismiss = {
                            onAction(OnRemoveFolderDialogDismissed)
                        },
                        onSelectStopBackupDestinationClicked = { folderName ->
                            // Hide the dialog before opening the picker so a second quick tap on
                            // "Move folder to Cloud drive" cannot re-open it. See AND-22622.
                            onAction(
                                SyncFoldersAction.OnStopBackupMoveDestinationSelectionStarted
                            )
                            onSelectStopBackupDestinationClicked(folderName)
                        },
                        folderName = syncUiItemToRemove.folderPairName,
                    )
                }

                else -> {
                    StopSyncConfirmDialog(
                        onConfirm = {
                            onAction(OnRemoveSyncFolderDialogConfirmed)
                        },
                        onDismiss = {
                            onAction(OnRemoveFolderDialogDismissed)
                        },
                    )
                }
            }
        }
    }

    LaunchedEffect(key1 = uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { resId ->
            val message = resources.getString(resId, uiState.movedFolderName)
            onAction(SnackBarShown)
            snackbarScope.launch {
                snackBarHostState?.showAutoDurationSnackbar(message)
            }
        }
    }
}

@Composable
internal fun StopSyncConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    BasicDialog(
        modifier = Modifier.testTag(STOP_SYNC_CONFIRM_DIALOG_TEST_TAG),
        title = stringResource(id = sharedResR.string.sync_stop_sync_confirm_dialog_title),
        description = stringResource(id = sharedResR.string.sync_stop_sync_confirm_dialog_message),
        positiveButtonText = stringResource(id = sharedResR.string.sync_stop_sync_button),
        onPositiveButtonClicked = onConfirm,
        negativeButtonText = stringResource(id = sharedResR.string.general_dialog_cancel_button),
        onNegativeButtonClicked = onDismiss,
        onDismiss = onDismiss,
    )
}

@CombinedThemePreviews
@Composable
private fun RemoveSyncFolderConfirmDialogPreview() {
    AndroidThemeForPreviews {
        StopSyncConfirmDialog(
            onConfirm = {},
            onDismiss = {},
        )
    }
}

internal const val STOP_SYNC_CONFIRM_DIALOG_TEST_TAG =
    "sync:stop_sync:confirm_dialog"
