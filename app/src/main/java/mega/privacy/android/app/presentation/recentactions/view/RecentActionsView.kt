package mega.privacy.android.app.presentation.recentactions.view

import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.recentactions.model.RecentActionsUiState
import mega.privacy.android.domain.entity.RecentActionBucket

/**
 * Composable for the recent actions screen
 */
@Composable
fun RecentActionsView(
    uiState: RecentActionsUiState,
    onItemClick: (RecentActionBucket) -> Unit,
    onMenuClick: (RecentActionBucket) -> Unit,
    onShowActivityActionClick: () -> Unit,
) {
    if (uiState.isLoading) {
        RecentLoadingView()
    } else if (uiState.hideRecentActivity) {
        RecentActionsHiddenView(
            onShowActivityActionClick = onShowActivityActionClick
        )
    } else {
        if (uiState.groupedRecentActionItems.isEmpty()) {
            RecentActionsEmptyView()
        } else {
            RecentActionsListView(
                groupedRecentActions = uiState.groupedRecentActionItems,
                onItemClick = onItemClick,
                onMenuClick = onMenuClick
            )
        }
    }
}