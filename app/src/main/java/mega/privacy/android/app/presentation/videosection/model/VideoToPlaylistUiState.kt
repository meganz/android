package mega.privacy.android.app.presentation.videosection.model

import mega.privacy.android.legacy.core.ui.model.SearchWidgetState

/**
 * The ui state for adding video to video playlist
 *
 * @property items the video playlist sets
 * @property searchState SearchWidgetState
 * @property query search query
 * @property selectedItemIds selected item ids
 */
data class VideoToPlaylistUiState(
    val items: List<VideoPlaylistSetUiEntity> = emptyList(),
    val searchState: SearchWidgetState = SearchWidgetState.COLLAPSED,
    val query: String = "",
    val selectedItemIds: List<Long> = emptyList()
)