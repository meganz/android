package mega.privacy.android.app.presentation.videosection.model

import mega.privacy.android.domain.entity.set.UserSet

/**
 * The ui state for adding video to video playlist
 *
 * @property items the video playlist sets
 */
data class VideoToPlaylistUiState(
    val items: List<UserSet> = emptyList(),
)