package mega.privacy.android.feature.sync.ui.model

import androidx.annotation.StringRes

internal data class StalledIssueDetailedInfo(
    @StringRes val title: Int,
    @StringRes val explanation: Int
)
