package mega.privacy.android.feature.sync.ui.synclist.stalledissues

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun SyncStalledIssuesRoute(
    modifier: Modifier = Modifier,
    viewModel: SyncStalledIssuesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val stalledIssues = state.stalledIssues

    StalledIssuesScreen(modifier, stalledIssues)
}
