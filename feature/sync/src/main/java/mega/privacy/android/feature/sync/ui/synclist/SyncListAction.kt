package mega.privacy.android.feature.sync.ui.synclist

import mega.privacy.android.feature.sync.ui.model.SyncUiItem

internal sealed interface SyncListAction {

    data class CardExpanded(val syncUiItem: SyncUiItem, val expanded: Boolean) : SyncListAction

    data class PauseRunClicked(val syncUiItem: SyncUiItem) : SyncListAction

    data class RemoveFolderClicked(val folderPairId: Long) : SyncListAction
}
