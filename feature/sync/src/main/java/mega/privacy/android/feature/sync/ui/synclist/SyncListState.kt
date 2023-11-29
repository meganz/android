package mega.privacy.android.feature.sync.ui.synclist

import mega.privacy.android.feature.sync.ui.model.SyncOption

internal data class SyncListState(
    val stalledIssuesCount: Int = 0,
    val snackbarMessage: String? = null,
    val shouldShowSyncOptionsMenuItem: Boolean = true,
    val shouldShowCleanSolvedIssueMenuItem: Boolean = false,
    val selectedSyncOption: SyncOption = SyncOption.WI_FI_OR_MOBILE_DATA,
)