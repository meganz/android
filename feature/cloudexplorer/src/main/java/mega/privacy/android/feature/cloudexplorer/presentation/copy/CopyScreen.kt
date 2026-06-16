package mega.privacy.android.feature.cloudexplorer.presentation.copy

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import mega.android.core.ui.extensions.LaunchedOnceEffect
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.CLOUD_TAB_INDEX
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.ExplorerScreen
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.INCOMING_TAB_INDEX
import mega.privacy.android.navigation.destination.CopyNavKey
import mega.privacy.android.navigation.destination.NodesExplorerNavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CopyScreen(
    uiState: CopyUiState,
    startNavKey: CopyNavKey,
    onNavigateBack: () -> Unit,
    onNavigate: (List<NavKey>) -> Unit,
    onSelectFolder: (NodeId) -> Unit,
) {
    if (uiState is CopyUiState.Data) {
        val tabIndex = if (uiState.nodeSourceType == NodeSourceType.INCOMING_SHARES) {
            INCOMING_TAB_INDEX
        } else {
            CLOUD_TAB_INDEX
        }
        var isProcessingAction by rememberSaveable { mutableStateOf(false) }

        if (uiState.targetPath.isNotEmpty()) {
            LaunchedOnceEffect {
                onNavigate(
                    uiState.targetPath.map { nodeId ->
                        NodesExplorerNavKey(
                            nodeId = nodeId,
                            nodeSourceType = uiState.nodeSourceType,
                            explorerMode = ExplorerMode.Copy,
                            startNavKey = startNavKey,
                            shareUris = null,
                        )
                    }
                )
            }
        }

        ExplorerScreen(
            explorerMode = ExplorerMode.Copy,
            startNavKey = startNavKey,
            isInnerNavigation = false,
            nodeExplorerId = uiState.rootNodeId,
            nodeSourceType = uiState.nodeSourceType,
            tabIndex = tabIndex,
            onCloseExplorerScreen = onNavigateBack,
            onNavigateBack = onNavigateBack,
            onNavigate = { onNavigate(listOf(it)) },
            isProcessingAction = isProcessingAction,
            onFolderPicked = { nodeId ->
                isProcessingAction = true
                onSelectFolder(nodeId)
            },
        )
    }
}
