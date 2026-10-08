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

    /**
     * Whether the screen shows a spinner of its own, which it does only while the launch is still
     * being resolved. Once [Data] is reached the play/pause button carries the spinner through
     * [Data.isLoading], so answering `true` here as well would put two on screen at once. A
     * rejected launch shows nothing — that screen is already closing.
     */
    val showsLoadingIndicator: Boolean
        get() = this is Loading

    /** The launch source has not been resolved yet. */
    data object Loading : VideoPlayerUiState

    /**
     * @property currentPlayingHandle MEGA node handle of the video to play.
     * @property currentPlayingItemName File name of the video. Never null: a launch without one is
 * rejected rather than resolved.
     * @property launchSourceType Where the player was launched from, which decides how the queue
     * around [currentPlayingHandle] is built.
     * @property videoWidth Width of the video as the player reports it, `0` until a frame has been
     * decoded. Together with [videoHeight] it is what the picture is laid out to.
     * @property videoHeight Height of the video as the player reports it, `0` until a frame has
     * been decoded.
     * @property isLoading Whether the player is working towards a picture, so the screen shows a
     * spinner rather than the black it would otherwise sit on.
     * @property playWhenReady Whether the user has playback running. Intent, not motion: it stays
     * `true` across a rebuffer, which is what the play/pause button has to show, because the
     * button says what the next tap does. Buffering is told apart by [isLoading].
     */
    data class Data(
        val currentPlayingHandle: Long,
        val currentPlayingItemName: String,
        val launchSourceType: VideoPlayerLaunchSourceType,
        val videoWidth: Int,
        val videoHeight: Int,
        val isLoading: Boolean,
        val playWhenReady: Boolean,
    ) : VideoPlayerUiState

    /**
     * No usable launch source reached the route, so there is nothing to play and the player closes.
     *
     * A terminal state rather than a one-shot event: the player never recovers from it, and a
     * state the screen can see is simpler to reason about than an event it has to consume.
     */
    data object InvalidLaunchSource : VideoPlayerUiState
}
