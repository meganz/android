package mega.privacy.android.feature.sync.ui.megapicker

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import  mega.privacy.android.feature.sync.ui.megapicker.MegaPickerAction.FolderClicked
import  mega.privacy.android.feature.sync.ui.megapicker.MegaPickerAction.CurrentFolderSelected

@Composable
internal fun MegaPickerRoute(
    viewModel: MegaPickerViewModel,
    folderSelected: () -> Unit,
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    state.value.nodes?.let { nodes ->
        MegaPickerScreen(
            nodes = nodes,
            folderClicked = { viewModel.handleAction(FolderClicked(it)) },
            currentFolderSelected = {
                viewModel.handleAction(CurrentFolderSelected)
                folderSelected()
            },
        )
    }
}

