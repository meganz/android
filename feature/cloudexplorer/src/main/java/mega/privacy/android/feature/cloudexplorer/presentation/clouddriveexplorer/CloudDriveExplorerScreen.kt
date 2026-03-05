package mega.privacy.android.feature.cloudexplorer.presentation.clouddriveexplorer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.feature.cloudexplorer.presentation.nodeexplorer.NodeExplorerUiState

@Composable
fun CloudDriveExplorerScreen(
    viewModel: CloudDriveExplorerViewModel,
    onFolderDestinationSelected: (FolderNode) -> Unit,
) {
    val uiState by viewModel.cloudDriveExplorerUiState.collectAsStateWithLifecycle()
    CloudDriveExplorerScreen(uiState, onFolderDestinationSelected)
}

@Composable
internal fun CloudDriveExplorerScreen(
    uiState: CloudDriveExplorerUiState,
    onFolderDestinationSelected: (FolderNode) -> Unit,
) {

}

@Composable
@CombinedThemePreviews
fun CloudDriveExplorerScreenPreview() {
    AndroidThemeForPreviews {
        CloudDriveExplorerScreen(
            CloudDriveExplorerUiState(
                CloudDriveExplorerInternalUiState(),
                NodeExplorerUiState()
            ), {})
    }
}