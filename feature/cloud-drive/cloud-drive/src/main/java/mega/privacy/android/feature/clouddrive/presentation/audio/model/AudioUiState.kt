package mega.privacy.android.feature.clouddrive.presentation.audio.model

import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.SortOrder
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

    /**
     * Loading
     */
    data object Loading : AudioUiState

    /**
     * Loaded state for the cloud drive audio list.
     *
     * @property items Audio nodes to display.
     * @property currentViewType List or grid presentation.
     * @property openedFileNode File opened for playback / actions, if any.
     * @property selectedSortOrder Domain sort order for opened-file handling.
     * @property selectedSortConfiguration Sort UI state for the nodes header.
     */
    data class Data(
        val items: List<NodeViewItem<TypedNode>>,
        val currentViewType: ViewType,
        val openedFileNode: TypedFileNode?,
        val selectedSortOrder: SortOrder,
        val selectedSortConfiguration: NodeSortConfiguration,
    ) : AudioUiState
}
