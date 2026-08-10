package mega.privacy.android.feature.clouddrive.presentation.clouddrive.model

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Get selected items
 *
 * @param selectedIds
 * @return selected nodes
 */
fun CloudDriveUiState.getSelectedItems(selectedIds: Set<NodeId>): List<TypedNode> =
    when (this) {
        is CloudDriveUiState.Data -> {
            items.filter { it.id in selectedIds }
                .map { it.node }
        }

        is CloudDriveUiState.Loading -> emptyList()
    }
