package mega.privacy.android.feature.clouddrive.presentation.audio.model

import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeViewItem

/**
 * UI state for the Audio screen.
 */
@Stable
sealed interface AudioUiState {

    data object Loading : AudioUiState

    /**
     * Loaded state for the cloud drive audio list.
     *
     * @property items Audio nodes to display.
     * @property currentViewType List or grid presentation.
     * @property openedFileNode File opened for playback / actions, if any.
     * @property selectedSortOrder Domain sort order for opened-file handling.
     * @property selectedSortConfiguration Sort UI state for the nodes header.
     * @property isSearchRevampEnabled Which search entry (revamp vs legacy) to use from the app bar.
     * @property isHiddenNodesEnabled Whether the hidden nodes feature is enabled for this account.
     * @property showHiddenNodes Whether the user has opted to show hidden/sensitive nodes.
     */
    data class Data(
        val items: List<NodeViewItem<TypedNode>>,
        val currentViewType: ViewType,
        val openedFileNode: TypedFileNode?,
        val selectedSortOrder: SortOrder,
        val selectedSortConfiguration: NodeSortConfiguration,
        val isSearchRevampEnabled: Boolean,
        val isHiddenNodesEnabled: Boolean = false,
        val showHiddenNodes: Boolean = false,
    ) : AudioUiState {

        val visibleItemsCount: Int
            get() = if (showHiddenNodes || !isHiddenNodesEnabled) {
                items.size
            } else {
                items.count { !it.isSensitive }
            }

        val isEmpty: Boolean
            get() = visibleItemsCount == 0

        fun computeSelectedItemsCount(selectedIds: Set<NodeId>): Int {
            if (showHiddenNodes || !isHiddenNodesEnabled) {
                return items.count { it.node.id in selectedIds }
            }
            return items.count { !it.isSensitive && it.node.id in selectedIds }
        }
    }
}
