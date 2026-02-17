package mega.privacy.android.feature.cloudexplorer.presentation.cloudexplorer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.MegaOutlinedButton
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
    MegaScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            MegaText(text = "Cloud Explorer Screen")
            MegaOutlinedButton(modifier = Modifier, "Choose root node", onClick = {
                uiState.currentFolder?.let { onFolderDestinationSelected(it) }
            })
        }
    }
}

@Composable
@CombinedThemePreviews
fun CloudExplorerScreenPreview() {
    AndroidThemeForPreviews {
        CloudExplorerScreen(CloudExplorerUiState(), {})
    }
}