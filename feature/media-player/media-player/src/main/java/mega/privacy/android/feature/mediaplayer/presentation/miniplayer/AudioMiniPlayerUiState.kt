package mega.privacy.android.feature.mediaplayer.presentation.miniplayer

/**
 * State rendered by [AudioMiniPlayer].
 */
internal sealed interface AudioMiniPlayerUiState {

    /**
     * No playback session exists, so the bar is not rendered at all. Also the state before the
     * first snapshot arrives: the mini player has nothing to show until a session is connected.
     */
    data object Hidden : AudioMiniPlayerUiState

    /**
     * A session is playing or paused and the bar is rendered.
     *
     * @property title Title of the current item, already resolved from the Media3 metadata.
     * @property artist Artist of the current item, null or blank when the file carries none.
     * @property isPlaying True while the player is actively playing.
     * @property isLoading True while the player is buffering; the play/pause icon is replaced by
     * a throbber but the button stays tappable, matching the full-screen player.
     * @property progress Playback progress in `0f..1f`, 0 when the duration is not known yet.
     */
    data class Data(
        val title: String,
        val artist: String?,
        val isPlaying: Boolean,
        val isLoading: Boolean,
        val progress: Float,
    ) : AudioMiniPlayerUiState
}
