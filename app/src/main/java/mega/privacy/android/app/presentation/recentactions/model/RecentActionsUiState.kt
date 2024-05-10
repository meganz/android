package mega.privacy.android.app.presentation.recentactions.model

import mega.privacy.android.domain.entity.AccountType

/**
 * Recent actions UI state
 *
 * @param groupedRecentActionItems map of recent action bucket UI entity, grouped by timestamp for sticky header
 * @param hideRecentActivity true if recent activity should be hidden
 * @param isLoading true if loading
 * @param isConnected true if connected to internet
 * @param accountType
 * @param showHiddenItems
 */
data class RecentActionsUiState(
    val groupedRecentActionItems: Map<String, List<RecentActionBucketUiEntity>> = emptyMap(),
    val hideRecentActivity: Boolean = false,
    val isLoading: Boolean = true,
    val isConnected: Boolean = false,
    val accountType: AccountType? = null,
    val showHiddenItems: Boolean = false,
)
