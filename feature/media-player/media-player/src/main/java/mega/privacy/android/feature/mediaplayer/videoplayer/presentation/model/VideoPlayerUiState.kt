package mega.privacy.android.feature.mediaplayer.videoplayer.presentation.model

import androidx.compose.runtime.Stable

/**
 * UI state of the revamped video player.
 *
 * The player is being rebuilt feature by feature against `.ref/video-player-feature-inventory.md`,
 * so [Data] carries only what the features built so far have resolved. Fields arrive with the
 * feature that needs them and take no default values, so a field nobody wired up fails to compile
 * rather than reading as empty.
 */
@Stable
sealed interface VideoPlayerUiState {

    /** The launch source has not been resolved yet. */
    data object Loading : VideoPlayerUiState

    /**
     * @property currentPlayingHandle MEGA node handle of the video to play.
     * @property currentPlayingItemName File name of the video. Never null: a launch without one is
 * rejected rather than resolved.
     * @property launchSourceType Where the player was launched from, which decides how the queue
     * around [currentPlayingHandle] is built.
     */
    data class Data(
        val currentPlayingHandle: Long,
        val currentPlayingItemName: String,
        val launchSourceType: VideoPlayerLaunchSourceType,
    ) : VideoPlayerUiState

    /**
     * No usable launch source reached the route, so there is nothing to play and the player closes.
     *
     * A terminal state rather than a one-shot event: the player never recovers from it, and a
     * state the screen can see is simpler to reason about than an event it has to consume.
     */
    data object InvalidLaunchSource : VideoPlayerUiState
}
