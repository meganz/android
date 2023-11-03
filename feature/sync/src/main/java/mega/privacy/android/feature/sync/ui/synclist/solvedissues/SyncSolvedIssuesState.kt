package mega.privacy.android.feature.sync.ui.synclist.solvedissues

import mega.privacy.android.feature.sync.ui.model.SolvedIssueUiItem

internal data class SyncSolvedIssuesState(
    val solvedIssues: List<SolvedIssueUiItem> = emptyList(),
)