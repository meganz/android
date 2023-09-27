package mega.privacy.android.feature.sync.ui.synclist.folders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.PauseRunClicked
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.RemoveFolderClicked
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.CardExpanded

@Composable
internal fun SyncFoldersRoute(
    viewModel: SyncFoldersViewModel = hiltViewModel(),
    addFolderClicked: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SyncFoldersScreen(
        syncUiItems = state.syncUiItems,
        cardExpanded = viewModel::handleAction,
        pauseRunClicked = {
            viewModel.handleAction(PauseRunClicked(it))
        },
        removeFolderClicked = {
            viewModel.handleAction(RemoveFolderClicked(it))
        },
        addFolderClicked = addFolderClicked,
    )
}
