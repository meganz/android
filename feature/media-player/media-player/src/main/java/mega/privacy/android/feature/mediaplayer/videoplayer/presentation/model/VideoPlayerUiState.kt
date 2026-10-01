package mega.privacy.android.feature.mediaplayer.videoplayer.presentation.model

import androidx.compose.runtime.Stable

/**
 * UI state of the revamped video player.
 *
 * Skeleton: the player is being rebuilt feature by feature against
 * `.ref/video-player-feature-inventory.md`, so [Data] is still empty. Fields arrive with the
 * feature that needs them, and carry no default values — the state is assembled in one place in
 * the ViewModel, where a default would hide a field nobody wired up.
 */
@Stable
sealed interface VideoPlayerUiState {

    /** Nothing has been loaded yet. */
    data object Loading : VideoPlayerUiState

    /** The player is ready to be shown. */
    data object Data : VideoPlayerUiState
}
