package mega.privacy.android.feature.sync.ui.synclist.folders

import mega.privacy.android.feature.sync.ui.model.StopBackupOption
import mega.privacy.android.feature.sync.ui.model.SyncUiItem

internal sealed interface SyncFoldersAction {

    data class CardExpanded(val syncUiItem: SyncUiItem, val expanded: Boolean) : SyncFoldersAction

    data class PauseRunClicked(val syncUiItem: SyncUiItem) : SyncFoldersAction

    data class RemoveFolderClicked(val syncUiItem: SyncUiItem) : SyncFoldersAction

    data object OnRemoveSyncFolderDialogConfirmed : SyncFoldersAction

    data class OnRemoveBackupFolderDialogConfirmed(val stopBackupOption: StopBackupOption) :
        SyncFoldersAction

    data object OnRemoveFolderDialogDismissed : SyncFoldersAction

    data object OnSyncsPausedErrorDialogDismissed : SyncFoldersAction

    data object SnackBarShown : SyncFoldersAction
}
