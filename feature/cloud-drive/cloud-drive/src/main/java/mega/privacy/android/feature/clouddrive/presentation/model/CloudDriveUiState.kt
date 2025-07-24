package mega.privacy.android.feature.clouddrive.presentation.model

import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode

data class CloudDriveUiState(
    val isLoading: Boolean = true,
    val currentFolderId: NodeId = NodeId(-1L),
    val items: List<NodeUiItem<TypedNode>> = emptyList(),
    val selectedItems: Set<Long> = emptySet(),
    val isInSelectionMode: Boolean = false,
)