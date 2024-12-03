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
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.OnRemoveSyncFolderDialogConfirmed
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.OnRemoveFolderDialogDismissed
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.OnSyncsPausedErrorDialogDismissed
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.PauseRunClicked
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.RemoveFolderClicked
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.SnackBarShown
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.android.shared.resources.R as sharedResR
import mega.privacy.android.feature.sync.ui.stopbackup.StopBackupConfirmationDialog

@Composable
internal fun SyncFoldersRoute(
    addFolderClicked: () -> Unit,
    onSelectStopBackupDestinationClicked: () -> Unit,
    upgradeAccountClicked: () -> Unit,
    issuesInfoClicked: () -> Unit,
    viewModel: SyncFoldersViewModel,
    uiState: SyncFoldersUiState,
    snackBarHostState: SnackbarHostState,
    deviceName: String,
    isBackupForAndroidEnabled: Boolean,
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
        addFolderClicked = addFolderClicked,
        upgradeAccountClicked = upgradeAccountClicked,
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
            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = "https://mega.nz/opensync#${syncUiItem.megaStorageNodeId.longValue}".toUri()
            })
        },
        isLowBatteryLevel = uiState.isLowBatteryLevel,
        isFreeAccount = uiState.isFreeAccount,
        isLoading = uiState.isLoading,
        showSyncsPausedErrorDialog = uiState.showSyncsPausedErrorDialog,
        onShowSyncsPausedErrorDialogDismissed = {
            viewModel.handleAction(OnSyncsPausedErrorDialogDismissed)
        },
        deviceName = deviceName,
        isBackupForAndroidEnabled = isBackupForAndroidEnabled,
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

    val message = uiState.snackbarMessage?.let { stringResource(id = it) }
    LaunchedEffect(key1 = uiState.snackbarMessage) {
        message?.let {
            snackBarHostState.showAutoDurationSnackbar(it)
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
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        StopSyncConfirmDialog(
            onConfirm = {},
            onDismiss = {},
        )
    }
}

internal const val STOP_SYNC_CONFIRM_DIALOG_TEST_TAG =
    "sync:stop_sync:confirm_dialog"
