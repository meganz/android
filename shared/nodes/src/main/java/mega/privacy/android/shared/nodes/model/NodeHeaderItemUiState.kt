package mega.privacy.android.shared.nodes.model

import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.preference.ViewType

/**
 * UI state for the node explorer header (view type and sort configuration).
 */
@Stable
sealed interface NodeHeaderItemUiState {
    /**
     * Initial loading state.
     */
    data object Loading : NodeHeaderItemUiState

    /**
     * Data state containing view type and sort configuration.
     *
     * @param viewType Current list/grid view type.
     * @param nodeSortConfiguration Current sort option and direction.
     */
    data class Data(
        val viewType: ViewType,
        val nodeSortConfiguration: NodeSortConfiguration,
    ) : NodeHeaderItemUiState

    val isLoading: Boolean
        get() = this is Loading
}
