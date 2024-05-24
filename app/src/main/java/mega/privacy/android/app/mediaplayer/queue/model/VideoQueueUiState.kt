package mega.privacy.android.app.mediaplayer.queue.model

import mega.privacy.android.legacy.core.ui.model.SearchWidgetState

/**
 * Video queue UI state
 *
 * @property items list of media queue item UI entities
 * @property currentPlayingPosition the current playing position of the video
 * @property indexOfCurrentPlayingItem the index of the current playing item
 * @property selectedItemHandles the selected item handles
 * @property isActionMode whether the action mode is activated
 * @property searchState SearchWidgetState
 * @property query search query
 */
data class VideoQueueUiState(
    val items: List<MediaQueueItemUiEntity> = emptyList(),
    val currentPlayingPosition: String = "00:00",
    val indexOfCurrentPlayingItem: Int = 0,
    val selectedItemHandles: List<Long> = emptyList(),
    val isActionMode: Boolean = false,
    val searchState: SearchWidgetState = SearchWidgetState.COLLAPSED,
    val query: String? = null,
)
