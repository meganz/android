package mega.privacy.android.feature.sync.ui.synclist

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import mega.privacy.android.feature.sync.ui.synclist.SyncListAction.CardExpanded
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun SyncListRoute(
    viewModel: SyncListViewModel,
    addFolderClicked: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SyncListScreen(
        syncUiItems = state.syncUiItems,
        cardExpanded = { syncUiItem, expanded ->
            viewModel.handleAction(CardExpanded(syncUiItem, expanded))
        },
        removeFolderClicked = {
            viewModel.handleAction(SyncListAction.RemoveFolderClicked(it))
        },
        addFolderClicked = addFolderClicked
    )
}
