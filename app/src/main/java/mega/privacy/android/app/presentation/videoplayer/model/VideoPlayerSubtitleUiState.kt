package mega.privacy.android.app.presentation.videoplayer.model

import androidx.compose.runtime.Stable
import mega.privacy.android.app.mediaplayer.model.SubtitleFileInfoItem
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo

/**
 * UI state for the video player subtitle selection screen.
 *
 * @property isLoading true while waiting for initial data from upstream flows
 * @property items the list of subtitle file items to display
 * @property hiddenNodesEnabled true when the account is eligible for hidden-node features
 * @property query the active search query, or null when search is inactive
 * @property selectedSubtitleFileInfo the currently selected subtitle file, or null
 */
@Stable
data class VideoPlayerSubtitleUiState(
    val isLoading: Boolean = true,
    val items: List<SubtitleFileInfoItem> = emptyList(),
    val hiddenNodesEnabled: Boolean = false,
    val query: String? = null,
    val selectedSubtitleFileInfo: SubtitleFileInfo? = null,
)
