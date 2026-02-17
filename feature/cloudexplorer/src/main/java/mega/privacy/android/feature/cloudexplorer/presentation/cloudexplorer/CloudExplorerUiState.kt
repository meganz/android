package mega.privacy.android.feature.cloudexplorer.presentation.cloudexplorer

import mega.privacy.android.domain.entity.node.FolderNode

data class CloudExplorerUiState(
    val currentFolder: FolderNode? = null
)