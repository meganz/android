package mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.node.FolderNode

@Composable
fun NodesExplorerScreen(
    viewModel: NodesExplorerViewModel,
    onFolderDestinationSelected: (FolderNode) -> Unit,
) {
    val uiState by viewModel.nodesExplorerUiState.collectAsStateWithLifecycle()
    val uiStateShared by viewModel.nodeExplorerSharedUiState.collectAsStateWithLifecycle()
    NodesExplorerScreen(uiState, onFolderDestinationSelected)
}

@Composable
internal fun NodesExplorerScreen(
    uiState: NodesExplorerUiState,
    onFolderDestinationSelected: (FolderNode) -> Unit,
) {

}

@Composable
@CombinedThemePreviews
fun NodesExplorerScreenPreview() {
    AndroidThemeForPreviews {
        NodesExplorerScreen(
            NodesExplorerUiState(), {})
    }
}