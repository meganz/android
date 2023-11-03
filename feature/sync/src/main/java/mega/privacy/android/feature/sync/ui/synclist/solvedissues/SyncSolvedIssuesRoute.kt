package mega.privacy.android.feature.sync.ui.synclist.solvedissues

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun SyncSolvedIssuesRoute(
    viewModel: SyncSolvedIssuesViewModel = hiltViewModel(),
) {

    val state by viewModel.state.collectAsStateWithLifecycle()

    SyncSolvedIssuesScreen(
        state.solvedIssues
    )
}
