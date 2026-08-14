package mega.privacy.android.feature.mediaplayer.data.model

import androidx.media3.common.Player

/**
 * Raw player state emitted by [mega.privacy.android.feature.mediaplayer.data.gateway.AudioMediaControllerGateway].
 *
 * Represents the current snapshot of the Media3 MediaController state. The ViewModel maps this
 * into AudioPlayerUiState and applies domain-level side effects (analytics, persistence,
 * node name fetching) on top.
 *
 * @property isPlaying True when the player is actively playing (not paused or buffering).
 * @property currentPositionMs Current playback position in milliseconds.
 * @property durationMs Total duration of the current media item in milliseconds.
 * @property repeatMode Current repeat mode; one of [Player.REPEAT_MODE_OFF], [Player.REPEAT_MODE_ONE], or [Player.REPEAT_MODE_ALL].
 * @property shuffleEnabled True when shuffle mode is enabled.
 * @property mediaItemCount Total number of media items in the queue.
 * @property isBuffering True when the player is buffering (i.e. [Player.STATE_BUFFERING] is active).
 * @property isIdle True when [Player.STATE_IDLE] is active, which indicates the player has stopped and is no longer holding any playback session.
 * @property title Title of the current media item from its [androidx.media3.common.MediaMetadata].
 * @property artist Artist of the current media item from its [androidx.media3.common.MediaMetadata].
 * @property artworkUri Artwork URI of the current media item from its [androidx.media3.common.MediaMetadata].
 * @property currentMediaItemId Opaque UUID assigned to the current [androidx.media3.common.MediaItem].
 * @property playbackSpeed Current playback speed multiplier; `1.0` = normal speed.
 * @property currentMediaItemHandle MEGA node handle for the current media item, resolved via [mega.privacy.android.feature.mediaplayer.data.MediaHandleStore].
 * @property queueItems Snapshot of all items currently in the player's queue. Updated when the timeline changes (items added, removed, or reordered).
 * @property currentQueueIndex Index of the currently playing item within [queueItems]; mapped to `0` when no item is active.
 */
data class AudioControllerState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffleEnabled: Boolean = false,
    val mediaItemCount: Int = 0,
    val isBuffering: Boolean = false,
    val isIdle: Boolean = false,
    val title: String? = null,
    val artist: String? = null,
    val artworkUri: String? = null,
    val currentMediaItemId: String? = null,
    val playbackSpeed: Float = 1f,
    val currentMediaItemHandle: Long? = null,
    val queueItems: List<AudioQueueItem> = emptyList(),
    val currentQueueIndex: Int = 0,
)
