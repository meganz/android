package mega.privacy.android.feature.sync.ui.synclist.stalledissues

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.feature.sync.ui.model.StalledIssueUiItem

@Composable
internal fun SyncStalledIssuesRoute(
    stalledIssueDetailsClicked: (StalledIssueUiItem) -> Unit,
    moreClicked: (StalledIssueUiItem) -> Unit,
    viewModel: SyncStalledIssuesViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val stalledIssues = state.stalledIssues

    StalledIssuesScreen(
        stalledIssues = stalledIssues,
        issueDetailsClicked = stalledIssueDetailsClicked,
        moreClicked = moreClicked,
        modifier = modifier
    )
}
