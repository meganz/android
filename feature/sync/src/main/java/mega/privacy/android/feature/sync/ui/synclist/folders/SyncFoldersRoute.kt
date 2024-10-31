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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.core.net.toUri
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.OnRemoveFolderDialogConfirmed
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.OnRemoveFolderDialogDismissed
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.OnSyncsPausedErrorDialogDismissed
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.PauseRunClicked
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.RemoveFolderClicked
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.SnackBarShown
import mega.privacy.android.feature.sync.ui.views.SyncTypePreviewProvider
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.android.shared.resources.R as sharedResR

@Composable
internal fun SyncFoldersRoute(
    addFolderClicked: () -> Unit,
    upgradeAccountClicked: () -> Unit,
    issuesInfoClicked: () -> Unit,
    viewModel: SyncFoldersViewModel,
    state: SyncFoldersState,
    snackBarHostState: SnackbarHostState,
    deviceName: String,
    isBackupForAndroidEnabled: Boolean,
) {
    val context = LocalContext.current

    SyncFoldersScreen(
        syncUiItems = state.syncUiItems,
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
        isLowBatteryLevel = state.isLowBatteryLevel,
        isFreeAccount = state.isFreeAccount,
        isLoading = state.isLoading,
        showSyncsPausedErrorDialog = state.showSyncsPausedErrorDialog,
        onShowSyncsPausedErrorDialogDismissed = {
            viewModel.handleAction(OnSyncsPausedErrorDialogDismissed)
        },
        deviceName = deviceName,
        isBackupForAndroidEnabled = isBackupForAndroidEnabled,
    )

    state.syncUiItemToRemove?.let { syncUiItemToRemove ->
        if (state.showConfirmRemoveSyncFolderDialog) {
            RemoveSyncFolderConfirmDialog(
                syncType = syncUiItemToRemove.syncType,
                onConfirm = {
                    viewModel.handleAction(OnRemoveFolderDialogConfirmed)
                },
                onDismiss = {
                    viewModel.handleAction(OnRemoveFolderDialogDismissed)
                },
            )
        }
    }

    val message = state.snackbarMessage?.let { stringResource(id = it) }
    LaunchedEffect(key1 = state.snackbarMessage) {
        message?.let {
            snackBarHostState.showAutoDurationSnackbar(it)
            viewModel.handleAction(SnackBarShown)
        }
    }
}

@Composable
internal fun RemoveSyncFolderConfirmDialog(
    syncType: SyncType,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (syncType) {
        SyncType.TYPE_BACKUP -> {
            ConfirmationDialog(
                title = stringResource(id = sharedResR.string.sync_stop_backup_confirm_dialog_title),
                text = stringResource(id = sharedResR.string.sync_stop_backup_confirm_dialog_message),
                confirmButtonText = stringResource(id = sharedResR.string.sync_stop_backup_button),
                cancelButtonText = stringResource(id = sharedResR.string.general_dialog_cancel_button),
                onConfirm = onConfirm,
                onDismiss = onDismiss,
                modifier = Modifier.testTag(REMOVE_BACKUP_FOLDER_CONFIRM_DIALOG_TEST_TAG),
            )
        }

        else -> {
            ConfirmationDialog(
                title = stringResource(id = sharedResR.string.sync_stop_sync_confirm_dialog_title),
                text = stringResource(id = sharedResR.string.sync_stop_sync_confirm_dialog_message),
                confirmButtonText = stringResource(id = sharedResR.string.sync_stop_sync_button),
                cancelButtonText = stringResource(id = sharedResR.string.general_dialog_cancel_button),
                onConfirm = onConfirm,
                onDismiss = onDismiss,
                modifier = Modifier.testTag(REMOVE_SYNC_FOLDER_CONFIRM_DIALOG_TEST_TAG),
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun RemoveSyncFolderConfirmDialogPreview(
    @PreviewParameter(SyncTypePreviewProvider::class) syncType: SyncType
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        RemoveSyncFolderConfirmDialog(
            syncType = syncType,
            onConfirm = {},
            onDismiss = {},
        )
    }
}

internal const val REMOVE_SYNC_FOLDER_CONFIRM_DIALOG_TEST_TAG =
    "sync:remove_sync_folder:confirm_dialog"
internal const val REMOVE_BACKUP_FOLDER_CONFIRM_DIALOG_TEST_TAG =
    "sync:remove_backup_folder:confirm_dialog"
