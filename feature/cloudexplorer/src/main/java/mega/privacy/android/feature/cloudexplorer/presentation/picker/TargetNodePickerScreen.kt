package mega.privacy.android.feature.cloudexplorer.presentation.picker

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
import mega.privacy.android.navigation.destination.ExplorerNavKey
import mega.privacy.android.navigation.destination.NodesExplorerNavKey

/**
 * Shared entry screen for the copy and move flows. Resumes at the last picked destination by
 * pushing [TargetNodePickerUiState.Data.targetPath] onto the back stack, then lets the user pick a
 * destination folder via [onSelectFolder]. [disabledTargetId] blocks re-selecting a folder (used by
 * move to forbid the source's current parent).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TargetNodePickerScreen(
    uiState: TargetNodePickerUiState,
    startNavKey: ExplorerNavKey,
    explorerMode: ExplorerMode,
    onNavigateBack: () -> Unit,
    onNavigate: (List<NavKey>) -> Unit,
    onSelectFolder: (NodeId) -> Unit,
    disabledTargetId: NodeId? = null,
) {
    if (uiState is TargetNodePickerUiState.Data) {
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
                            explorerMode = explorerMode,
                            startNavKey = startNavKey,
                            shareUris = null,
                        )
                    }
                )
            }
        }

        ExplorerScreen(
            explorerMode = explorerMode,
            startNavKey = startNavKey,
            isInnerNavigation = false,
            nodeExplorerId = uiState.rootNodeId,
            nodeSourceType = uiState.nodeSourceType,
            tabIndex = tabIndex,
            disabledTargetId = disabledTargetId,
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
