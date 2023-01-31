package mega.privacy.android.app.presentation.recentactions.model

/**
 * Recent actions UI state
 *
 * @param recentActionItems list of recent action items
 * @param hideRecentActivity true if recent activity should be hidden
 */
data class RecentActionsState(
    val recentActionItems: List<RecentActionItemType> = emptyList(),
    val hideRecentActivity: Boolean = false,
)
