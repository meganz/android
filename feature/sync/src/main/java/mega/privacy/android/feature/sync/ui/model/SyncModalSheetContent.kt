package mega.privacy.android.feature.sync.ui.model

internal sealed interface SyncModalSheetContent {

    data class DetailedInfo(val stalledIssueUiItem: StalledIssueUiItem) : SyncModalSheetContent

    data class IssueResolutions(val stalledIssueUiItem: StalledIssueUiItem) : SyncModalSheetContent
}
