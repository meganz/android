package mega.privacy.android.feature.sync.ui.synclist.stalledissues

import mega.privacy.android.feature.sync.ui.model.StalledIssueDetailedInfo
import mega.privacy.android.feature.sync.ui.model.StalledIssueUiItem

internal data class SyncStalledIssuesState(
    val stalledIssues: List<StalledIssueUiItem>,
    val stalledIssueDetailedInfo: StalledIssueDetailedInfo? = null,
)