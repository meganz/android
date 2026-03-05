package mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer

import mega.android.core.ui.model.LocalizedText

/**
 * UI state for [NodesExplorerViewModel].
 */
data class NodesExplorerUiState(
    val folderName: LocalizedText = LocalizedText.Literal(""),
)