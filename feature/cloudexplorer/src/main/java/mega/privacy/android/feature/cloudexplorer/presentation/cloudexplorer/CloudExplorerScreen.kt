package mega.privacy.android.feature.cloudexplorer.presentation.cloudexplorer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.node.FolderNode

@Composable
fun CloudExplorerScreen(
    viewModel: CloudExplorerViewModel,
    onFolderDestinationSelected: (FolderNode) -> Unit,
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    CloudExplorerScreen(uiState, onFolderDestinationSelected)
}

@Composable
internal fun CloudExplorerScreen(
    uiState: CloudExplorerUiState,
    onFolderDestinationSelected: (FolderNode) -> Unit,
) {

}

@Composable
@CombinedThemePreviews
fun CloudExplorerScreenPreview() {
    AndroidThemeForPreviews {
        CloudExplorerScreen(CloudExplorerUiState(), {})
    }
}