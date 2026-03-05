package mega.privacy.android.feature.cloudexplorer.presentation.clouddriveexplorer

import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.node.NodesLoadingState

data class CloudDriveExplorerInternalUiState(
    val folderName: LocalizedText = LocalizedText.Literal(""),
    val nodesLoadingState: NodesLoadingState = NodesLoadingState.Loading,
    val isHiddenNodeSettingsLoading: Boolean = true,
    val showHiddenNodes: Boolean = false,
    val isHiddenNodesEnabled: Boolean = false,
)
