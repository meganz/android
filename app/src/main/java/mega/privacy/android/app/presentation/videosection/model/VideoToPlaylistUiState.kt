package mega.privacy.android.app.presentation.videosection.model

import mega.privacy.android.legacy.core.ui.model.SearchWidgetState

/**
 * The ui state for adding video to video playlist
 *
 * @property items the video playlist sets
 * @property isLoading whether is in loading state
 * @property searchState SearchWidgetState
 * @property query search query
 * @property selectedPlaylistIds selected video playlist ids
 * @property isInputTitleValid true if the input title is valid
 * @property createVideoPlaylistPlaceholderTitle the create video playlist placeholder title
 * @property shouldCreateVideoPlaylist true if there is a need to create a video playlist
 * @property createDialogErrorMessage the create dialog error message
 * @property isVideoPlaylistCreatedSuccessfully the video playlist created successfully state
 */
data class VideoToPlaylistUiState(
    val items: List<VideoPlaylistSetUiEntity> = emptyList(),
    val isLoading: Boolean = true,
    val searchState: SearchWidgetState = SearchWidgetState.COLLAPSED,
    val query: String = "",
    val selectedPlaylistIds: List<Long> = emptyList(),
    val isInputTitleValid: Boolean = true,
    val createVideoPlaylistPlaceholderTitle: String = "",
    val shouldCreateVideoPlaylist: Boolean = false,
    val createDialogErrorMessage: Int? = null,
    val isVideoPlaylistCreatedSuccessfully: Boolean = false,
)