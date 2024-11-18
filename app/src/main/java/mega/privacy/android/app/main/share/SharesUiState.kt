package mega.privacy.android.app.main.share

import mega.privacy.android.app.presentation.manager.model.SharesTab

/**
 * UI state for shares screen
 * @property currentTab the current tab of shares screen
 */
data class SharesUiState(
    val currentTab: SharesTab = SharesTab.INCOMING_TAB,
)