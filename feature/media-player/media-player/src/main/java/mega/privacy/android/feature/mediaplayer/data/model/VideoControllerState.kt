package mega.privacy.android.feature.mediaplayer.data.model

import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode

/**
 * Raw player state emitted by
 * [mega.privacy.android.feature.mediaplayer.data.gateway.VideoMediaControllerGateway].
 *
 * A snapshot of the video player, carrying only primitives and domain types so that this module
 * stays free of `:app`. The ViewModel maps it into the UI state and applies the domain-level side
 * effects (analytics, persistence, node name lookups) on top.
 *
 * @property playWhenReady The user's playback intent, as set through
 * [mega.privacy.android.feature.mediaplayer.data.gateway.VideoMediaControllerGateway.setPlayWhenReady].
 * Distinct from [isPlaying]: while buffering, this is `true` and [isPlaying] is `false`. Control
 * flow that asks "should this resume?" belongs on this property, not on [isPlaying].
 * @property pausedByUserRequest True when the most recent pause came from the user rather than from
 * audio focus loss, a track change or buffering. Derived from Media3's
 * `PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST`, and used to stop an automatic resume from
 * overriding an explicit pause.
 * @property isBuffering True while the player is in `Player.STATE_BUFFERING`.
 * @property isEnded True once the current item reached `Player.STATE_ENDED`.
 * @property isReady True while the player is in `Player.STATE_READY`.
 * @property currentMediaHandle MEGA node handle of the item in the player; `null` before the first item loads.
 * @property transitionUpdatesName True when the most recent item transition moved to a different
 * item, so the displayed name and subtitle track have to be refreshed. False for a repeat of the
 * same item.
 * @property repeatMode Current repeat mode.
 * @property playbackSpeed Current playback speed multiplier; `1.0` is normal speed.
 * @property currentPositionMs Playback position of the current item, in milliseconds.
 * @property durationMs Total duration of the current item in milliseconds; `0` while unknown.
 * @property title Title of the current item from its Media3 metadata.
 * @property artist Artist of the current item from its Media3 metadata.
 * @property album Album of the current item from its Media3 metadata.
 * @property videoWidth Width of the decoded video in pixels; `0` before the first frame is sized.
 * @property videoHeight Height of the decoded video in pixels; `0` before the first frame is sized.
 */
data class VideoControllerState(
    val playWhenReady: Boolean = false,
    val pausedByUserRequest: Boolean = false,
    val isBuffering: Boolean = false,
    val isEnded: Boolean = false,
    val isReady: Boolean = false,
    val currentMediaHandle: Long? = null,
    val transitionUpdatesName: Boolean = false,
    val repeatMode: RepeatToggleMode = RepeatToggleMode.REPEAT_NONE,
    val playbackSpeed: Float = 1f,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
) {
    /**
     * True when the player is actively rendering frames, i.e. ready and not paused.
     *
     * Derived rather than stored: it depends on two inputs that arrive in separate callbacks, and
     * a stored copy goes stale whenever only one of them changes.
     */
    val isPlaying: Boolean get() = isReady && playWhenReady
}
