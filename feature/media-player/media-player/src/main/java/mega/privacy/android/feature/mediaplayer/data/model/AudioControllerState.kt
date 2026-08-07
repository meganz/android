package mega.privacy.android.feature.mediaplayer.data.model

import androidx.media3.common.Player

/**
 * Raw player state emitted by [mega.privacy.android.feature.mediaplayer.data.gateway.AudioMediaControllerGateway].
 *
 * Represents the current snapshot of the Media3 MediaController state. The ViewModel maps this
 * into AudioPlayerUiState and applies domain-level side effects (analytics, persistence,
 * node name fetching) on top.
 */
data class AudioControllerState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffleEnabled: Boolean = false,
    val mediaItemCount: Int = 0,
    val isBuffering: Boolean = false,
    /** True when [androidx.media3.common.Player.STATE_IDLE] is active, which indicates the player
     *  has stopped and is no longer holding any playback session. */
    val isIdle: Boolean = false,
    val title: String? = null,
    val artist: String? = null,
    val artworkUri: String? = null,
    /** Opaque UUID assigned to the current [androidx.media3.common.MediaItem]. */
    val currentMediaItemId: String? = null,
    /** Current playback speed multiplier; `1.0` = normal speed. */
    val playbackSpeed: Float = 1f,
    /** MEGA node handle for the current media item, resolved via [mega.privacy.android.feature.mediaplayer.data.MediaHandleStore]. */
    val currentMediaItemHandle: Long? = null,
)
