package mega.privacy.android.feature.sync.ui.synclist.stalledissues

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.feature.sync.ui.model.StalledIssueDetailedInfo
import mega.privacy.android.feature.sync.ui.model.StalledIssueUiItem

internal data class SyncStalledIssuesState(
    val stalledIssues: List<StalledIssueUiItem>,
    val stalledIssueDetailedInfo: StalledIssueDetailedInfo? = null,
    val snackbarMessageContent: StateEventWithContent<Int> = consumed(),
)
