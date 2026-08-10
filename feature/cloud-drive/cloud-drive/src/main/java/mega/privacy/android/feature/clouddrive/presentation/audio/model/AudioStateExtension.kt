package mega.privacy.android.feature.clouddrive.presentation.audio.model

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Get selected items
 *
 * @param selectedIds
 * @return selected nodes
 */
fun AudioUiState.getSelectedItems(selectedIds: Set<NodeId>): List<TypedNode> =
    when (this) {
        is AudioUiState.Data -> {
            items.filter { it.id in selectedIds }
                .map { it.node }
        }

        is AudioUiState.Loading -> emptyList()
    }