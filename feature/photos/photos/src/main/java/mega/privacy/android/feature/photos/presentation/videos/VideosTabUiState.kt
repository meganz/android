package mega.privacy.android.feature.photos.presentation.videos

import androidx.compose.runtime.Stable
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.feature.photos.presentation.videos.model.VideoUiEntity


@Stable
sealed interface VideosTabUiState {
    data object Loading : VideosTabUiState

    /**
     * The state is for the videos section
     *
     * @property allVideos the all video items
     * @property sortOrder the sort order
     * @property query the search query
     * @property selectedSortConfiguration the selected sort configuration
     */
    data class Data(
        val allVideos: List<VideoUiEntity> = emptyList(),
        val sortOrder: SortOrder = SortOrder.ORDER_NONE,
        val query: String? = null,
        val selectedSortConfiguration: NodeSortConfiguration = NodeSortConfiguration.default,
    ) : VideosTabUiState {
        /**
         * The highlight text for search by tags or description
         */
        val highlightText get() = query.orEmpty()
    }
}
