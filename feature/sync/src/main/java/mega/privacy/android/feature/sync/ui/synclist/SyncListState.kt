package mega.privacy.android.feature.sync.ui.synclist

internal data class SyncListState(
    val stalledIssuesCount: Int = 0,
    val shouldShowCleanSolvedIssueMenuItem: Boolean = false,
    val deviceName: String = "",
)
