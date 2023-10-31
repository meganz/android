package mega.privacy.android.feature.sync.ui.synclist

import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionAction
import mega.privacy.android.feature.sync.ui.model.StalledIssueUiItem

internal sealed interface SyncListAction {

    data class ResolveStalledIssue(
        val uiItem: StalledIssueUiItem,
        val selectedResolution: StalledIssueResolutionAction,
    ) : SyncListAction

    object SnackBarShown : SyncListAction
}
