package mega.privacy.android.feature.cloudexplorer.presentation.favouritesexplorer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.node.FolderNode

@Composable
fun FavouritesExplorerScreen(
    viewModel: FavouritesExplorerViewModel,
    onFolderDestinationSelected: (FolderNode) -> Unit,
) {
    val uiState by viewModel.favouritesExplorerUiState.collectAsStateWithLifecycle()
    FavouritesExplorerScreen(uiState, onFolderDestinationSelected)
}

@Composable
internal fun FavouritesExplorerScreen(
    uiState: FavouritesExplorerUiState,
    onFolderDestinationSelected: (FolderNode) -> Unit,
) {

}

@Composable
@CombinedThemePreviews
fun FavouritesExplorerScreenPreview() {
    AndroidThemeForPreviews {
        FavouritesExplorerScreen(
            FavouritesExplorerUiState(), {})
    }
}