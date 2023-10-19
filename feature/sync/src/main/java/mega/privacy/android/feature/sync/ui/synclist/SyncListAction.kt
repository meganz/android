package mega.privacy.android.feature.sync.ui.synclist

import mega.privacy.android.feature.sync.ui.model.StalledIssueResolutionAction
import mega.privacy.android.feature.sync.ui.model.StalledIssueUiItem
import mega.privacy.android.feature.sync.ui.model.SyncUiItem

internal sealed interface SyncListAction {

    data class ResolveStalledIssue(
        val uiItem: StalledIssueUiItem,
        val selectedResolution: StalledIssueResolutionAction,
    ) : SyncListAction
}
