package mega.privacy.android.feature.cloudexplorer.presentation.incomingsharesexplorer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.node.FolderNode

@Composable
fun IncomingSharesExplorerScreen(
    viewModel: IncomingSharesExplorerViewModel,
    onFolderDestinationSelected: (FolderNode) -> Unit,
) {
    val uiState by viewModel.incomingSharesExplorerUiState.collectAsStateWithLifecycle()
    IncomingSharesExplorerScreen(uiState, onFolderDestinationSelected)
}

@Composable
internal fun IncomingSharesExplorerScreen(
    uiState: IncomingSharesExplorerUiState,
    onFolderDestinationSelected: (FolderNode) -> Unit,
) {

}

@Composable
@CombinedThemePreviews
fun IncomingSharesExplorerScreenPreview() {
    AndroidThemeForPreviews {
        IncomingSharesExplorerScreen(
            IncomingSharesExplorerUiState(), {})
    }
}