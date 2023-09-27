package mega.privacy.android.feature.sync.ui.synclist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun SyncListRoute(
    viewModel: SyncListViewModel,
    addFolderClicked: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SyncListScreen(
        stalledIssuesCount = state.stalledIssuesCount,
        addFolderClicked = addFolderClicked
    )
}
