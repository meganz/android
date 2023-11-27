package mega.privacy.android.feature.sync.ui.synclist

internal data class SyncListState(
    val stalledIssuesCount: Int = 0,
    val snackbarMessage: String? = null,
    val shouldShowSyncOptionsMenuItem: Boolean = true,
    val shouldShowCleanSolvedIssueMenuItem: Boolean = false
)