package mega.privacy.android.feature.sync.ui.newfolderpair

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderAction.LocalFolderSelected
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderAction.FolderNameChanged
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderAction.NextClicked

@Composable
internal fun SyncNewFolderScreenRoute(
    viewModel: SyncNewFolderViewModel,
    openSelectMegaFolderScreen: () -> Unit,
    openNextScreen: (SyncNewFolderState) -> Unit,
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    SyncNewFolderScreen(
        folderPairName = state.value.folderPairName,
        selectedLocalFolder = state.value.selectedLocalFolder,
        selectedMegaFolder = state.value.selectedMegaFolder,
        localFolderSelected = { viewModel.handleAction(LocalFolderSelected(it)) },
        folderNameChanged = { viewModel.handleAction(FolderNameChanged(it)) },
        selectMegaFolderClicked = openSelectMegaFolderScreen,
        showPermissionBanner = false,
        permissionAllowButtonClicked = {
            // implemented in next MR
        },
        permissionLearnMoreButtonClicked = {
            // implemented in next MR
        },
        syncClicked = {
            viewModel.handleAction(NextClicked)
            openNextScreen(state.value)
        }
    )
}