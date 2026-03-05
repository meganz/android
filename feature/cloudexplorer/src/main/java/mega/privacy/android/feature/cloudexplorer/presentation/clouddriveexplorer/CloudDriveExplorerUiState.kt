package mega.privacy.android.feature.cloudexplorer.presentation.clouddriveexplorer

import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.feature.cloudexplorer.presentation.nodeexplorer.NodeExplorerUiState

data class CloudDriveExplorerUiState(
    val cloudDriveExplorerInternalUIState: CloudDriveExplorerInternalUiState,
    val nodeExplorerUiState: NodeExplorerUiState,
) {
    val folderName = cloudDriveExplorerInternalUIState.folderName
    val nodesLoadingState = cloudDriveExplorerInternalUIState.nodesLoadingState
    val showHiddenNodes = cloudDriveExplorerInternalUIState.showHiddenNodes
    val isHiddenNodesEnabled = cloudDriveExplorerInternalUIState.isHiddenNodesEnabled
    val isLoading = cloudDriveExplorerInternalUIState.nodesLoadingState == NodesLoadingState.Loading
            || cloudDriveExplorerInternalUIState.isHiddenNodeSettingsLoading

    val items = nodeExplorerUiState.items
    val viewType = nodeExplorerUiState.viewType
    val navigateBack = nodeExplorerUiState.navigateBack
    val sortOrder = nodeExplorerUiState.sortOrder
    val nodeSortConfiguration = nodeExplorerUiState.nodeSortConfiguration
    val isStorageOverQuota = nodeExplorerUiState.isStorageOverQuota
    val isSelectionModeEnabled = nodeExplorerUiState.isSelectionModeEnabled
}