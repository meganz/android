package mega.privacy.android.feature.photos.presentation.videos

import androidx.compose.runtime.Stable
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
     */
    data class Data(
        val allVideos: List<VideoUiEntity> = emptyList(),
        val sortOrder: SortOrder = SortOrder.ORDER_NONE,
        val query: String? = null,
    ) : VideosTabUiState
}
