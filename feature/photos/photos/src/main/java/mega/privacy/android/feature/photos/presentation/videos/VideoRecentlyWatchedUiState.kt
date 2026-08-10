package mega.privacy.android.feature.photos.presentation.videos

import mega.privacy.android.feature.photos.presentation.videos.model.VideoUiEntity

internal sealed interface VideoRecentlyWatchedUiState {
    data object Loading : VideoRecentlyWatchedUiState

    /**
     * The state for the video recently watched
     *
     * @property groupedVideoRecentlyWatchedItems map of video recently watched items, grouped by timestamp for sticky header
     * @property showHiddenItems Whether hidden items are shown
     *
     */
    data class Data(
        val groupedVideoRecentlyWatchedItems: Map<Long, List<VideoUiEntity>> = emptyMap(),
        val showHiddenItems: Boolean = false,
    ) : VideoRecentlyWatchedUiState
}