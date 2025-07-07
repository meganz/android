package mega.privacy.android.feature.sync.ui.synclist

import androidx.annotation.StringRes
import mega.privacy.android.feature.sync.ui.model.SyncConnectionType

internal data class SyncListState(
    val stalledIssuesCount: Int = 0,
    @StringRes val snackbarMessage: Int? = null,
    val shouldShowCleanSolvedIssueMenuItem: Boolean = false,
    val selectedSyncConnectionType: SyncConnectionType = SyncConnectionType.WiFiOrMobileData,
    val deviceName: String = "",
)
