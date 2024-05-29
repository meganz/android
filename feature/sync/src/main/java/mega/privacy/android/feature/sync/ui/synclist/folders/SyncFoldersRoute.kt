package mega.privacy.android.feature.sync.ui.synclist.folders

import androidx.compose.runtime.Composable
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.PauseRunClicked
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction.RemoveFolderClicked

@Composable
internal fun SyncFoldersRoute(
    addFolderClicked: () -> Unit,
    upgradeAccountClicked: () -> Unit,
    issuesInfoClicked: () -> Unit,
    viewModel: SyncFoldersViewModel,
    state: SyncFoldersState
) {
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
        upgradeAccountClicked = upgradeAccountClicked,
        issuesInfoClicked = issuesInfoClicked,
        isLowBatteryLevel = state.isLowBatteryLevel,
        isFreeAccount = state.isFreeAccount,
    )
}
