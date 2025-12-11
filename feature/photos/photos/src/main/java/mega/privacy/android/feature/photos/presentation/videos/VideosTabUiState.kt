package mega.privacy.android.feature.photos.presentation.videos

import androidx.compose.runtime.Stable
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.feature.photos.presentation.videos.model.DurationFilterOption
import mega.privacy.android.feature.photos.presentation.videos.model.LocationFilterOption
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.feature.photos.presentation.videos.model.VideoUiEntity


@Stable
sealed interface VideosTabUiState {
    data object Loading : VideosTabUiState

    /**
     * The state is for the videos section
     *
     * @property allVideoEntities the all video items
     * @property sortOrder the sort order
     * @property query the search query
     * @property locationSelectedFilterOption the selected location filter option
     * @property durationSelectedFilterOption the selected duration filter option
     * @property selectedSortConfiguration the selected sort configuration
     * @property selectedTypedNodes the selected typed node list
     */
    data class Data(
        val allVideoEntities: List<VideoUiEntity> = emptyList(),
        val allVideoNodes: List<TypedVideoNode> = emptyList(),
        val sortOrder: SortOrder = SortOrder.ORDER_NONE,
        val query: String? = null,
        val locationSelectedFilterOption: LocationFilterOption = LocationFilterOption.AllLocations,
        val durationSelectedFilterOption: DurationFilterOption = DurationFilterOption.AllDurations,
        val selectedSortConfiguration: NodeSortConfiguration = NodeSortConfiguration.default,
        val selectedTypedNodes: List<TypedNode> = emptyList()
    ) : VideosTabUiState {
        /**
         * The highlight text for search by tags or description
         */
        val highlightText get() = query.orEmpty()
    }
}
