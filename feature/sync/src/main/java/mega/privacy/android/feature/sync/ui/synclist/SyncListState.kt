package mega.privacy.android.feature.sync.ui.synclist

import androidx.annotation.StringRes

internal data class SyncListState(
    val stalledIssuesCount: Int = 0,
    @StringRes val snackbarMessage: Int? = null,
    val shouldShowCleanSolvedIssueMenuItem: Boolean = false,
    val deviceName: String = "",
)
