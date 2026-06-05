package mega.privacy.android.feature.cloudexplorer.presentation.importnodes

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.ExplorerScreen
import mega.privacy.android.navigation.destination.ImportNavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImportScreen(
    uiState: ImportUiState,
    startNavKey: ImportNavKey,
    onNavigateBack: () -> Unit,
    onNavigate: (NavKey) -> Unit,
    onSelectFolder: (NodeId) -> Unit,
) {
    if (uiState is ImportUiState.Data) {
        var isProcessingAction by rememberSaveable { mutableStateOf(false) }

        ExplorerScreen(
            explorerMode = ExplorerMode.Import,
            startNavKey = startNavKey,
            isInnerNavigation = false,
            nodeExplorerId = uiState.rootNodeId,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            onCloseExplorerScreen = onNavigateBack,
            onNavigateBack = onNavigateBack,
            onNavigate = onNavigate,
            isProcessingAction = isProcessingAction,
            onFolderPicked = { nodeId ->
                isProcessingAction = true
                onSelectFolder(nodeId)
                onNavigateBack()
            },
        )
    }
}
