package mega.privacy.android.feature.photos.presentation.videos

import androidx.compose.runtime.Stable
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.feature.photos.presentation.videos.model.VideoUiEntity


@Stable
sealed interface VideosTabUiState {
    data object Loading : VideosTabUiState

    /**
     * The state is for the videos section
     *
     * @property allVideoEntities the all video items
     * @property query the search query
     * @property selectedSortConfiguration the selected sort configuration
     * @property selectedTypedNodes the selected typed node list
     * @property showHiddenItems whether to show hidden items
     */
    data class Data(
        val allVideoEntities: List<VideoUiEntity> = emptyList(),
        val query: String? = null,
        val selectedSortConfiguration: NodeSortConfiguration = NodeSortConfiguration.default,
        val selectedTypedNodes: List<TypedNode> = emptyList(),
        val showHiddenItems: Boolean = false
    ) : VideosTabUiState {
        /**
         * The highlight text for search by tags or description
         */
        val highlightText get() = query.orEmpty()
    }
}
