package mega.privacy.android.app.presentation.recentactions.model

import mega.privacy.android.domain.entity.RecentActionBucket

/**
 * Recent actions UI state
 *
 * @param groupedRecentActionItems map of recent action buckets, grouped by timestamp for sticky header
 * @param hideRecentActivity true if recent activity should be hidden
 * @param isLoading true if loading
 */
data class RecentActionsUiState(
    val groupedRecentActionItems: Map<Long, List<RecentActionBucket>> = emptyMap(),
    val hideRecentActivity: Boolean = false,
    val isLoading: Boolean = true
)
