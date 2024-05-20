package mega.privacy.android.app.mediaplayer.queue.model

/**
 * Video queue UI state
 *
 * @property items list of media queue item UI entities
 * @property currentPlayingPosition the current playing position of the video
 * @property indexOfCurrentPlayingItem the index of the current playing item
 * @property selectedItemHandles the selected item handles
 * @property isSearchMode whether the search mode is activated
 */
data class VideoQueueUiState(
    val items: List<MediaQueueItemUiEntity> = emptyList(),
    val currentPlayingPosition: String = "00:00",
    val indexOfCurrentPlayingItem: Int = 0,
    val selectedItemHandles: List<Long> = emptyList(),
    val isSearchMode: Boolean = false
)
