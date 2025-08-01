package mega.privacy.android.feature.sync.ui.model

import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionAction

internal sealed interface SyncModalSheetContent {

    data class IssueResolutions(val stalledIssueUiItem: StalledIssueUiItem) : SyncModalSheetContent

    data class ApplyToAllDialog(
        val stalledIssueUiItem: StalledIssueUiItem,
        val selectedAction: StalledIssueResolutionAction
    ) : SyncModalSheetContent
}
