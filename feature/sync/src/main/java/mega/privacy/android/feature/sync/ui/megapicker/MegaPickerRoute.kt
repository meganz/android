package mega.privacy.android.feature.sync.ui.megapicker

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.feature.sync.ui.megapicker.MegaPickerAction.FolderClicked
import mega.privacy.android.feature.sync.ui.megapicker.MegaPickerAction.CurrentFolderSelected
import nz.mega.sdk.MegaApiJava

@Composable
internal fun MegaPickerRoute(
    viewModel: MegaPickerViewModel,
    folderSelected: () -> Unit,
    backClicked: () -> Unit,
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    state.value.nodes?.let { nodes ->
        MegaPickerScreen(
            currentFolder = state.value.currentFolder,
            nodes = nodes,
            folderClicked = { viewModel.handleAction(FolderClicked(it)) },
            currentFolderSelected = {
                viewModel.handleAction(CurrentFolderSelected)
                folderSelected()
            },
        )
    }

    val onBack = {
        if (state.value.currentFolder?.parentId?.longValue != MegaApiJava.INVALID_HANDLE) {
            viewModel.handleAction(MegaPickerAction.BackClicked)
        } else {
            backClicked()
        }
    }

    BackHandler(onBack = onBack)
}

