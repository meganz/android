package mega.privacy.android.feature.sync.ui.synclist.folders

import android.net.Uri
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.ui.model.StopBackupOption
import mega.privacy.android.feature.sync.ui.model.SyncUiItem

internal sealed interface SyncFoldersAction {

    data class CardExpanded(val syncUiItem: SyncUiItem, val expanded: Boolean) : SyncFoldersAction

    data class PauseRunClicked(val syncUiItem: SyncUiItem) : SyncFoldersAction

    data class RemoveFolderClicked(val syncUiItem: SyncUiItem) : SyncFoldersAction

    data class LocalFolderSelected(val syncUiItem: SyncUiItem, val uri: Uri) : SyncFoldersAction

    data object OnRemoveSyncFolderDialogConfirmed : SyncFoldersAction

    data class OnRemoveBackupFolderDialogConfirmed(
        val stopBackupOption: StopBackupOption,
        val selectedFolder: RemoteFolder?,
    ) : SyncFoldersAction

    data object OnRemoveFolderDialogDismissed : SyncFoldersAction

    /**
     * Dispatched when the user picks "Move folder to Cloud drive" on the stop-backup dialog.
     * Hides the confirmation dialog immediately (so a second quick tap cannot re-open the
     * destination picker) while keeping the folder to remove for the move that completes once a
     * destination is selected.
     */
    data object OnStopBackupMoveDestinationSelectionStarted : SyncFoldersAction

    data object SnackBarShown : SyncFoldersAction
}
