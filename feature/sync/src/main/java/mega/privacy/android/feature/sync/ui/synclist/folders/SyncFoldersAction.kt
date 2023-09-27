package mega.privacy.android.feature.sync.ui.synclist.folders

import mega.privacy.android.feature.sync.ui.model.SyncUiItem

internal sealed interface SyncFoldersAction {

    data class CardExpanded(val syncUiItem: SyncUiItem, val expanded: Boolean) : SyncFoldersAction

    data class PauseRunClicked(val syncUiItem: SyncUiItem) : SyncFoldersAction

    data class RemoveFolderClicked(val folderPairId: Long) : SyncFoldersAction
}
