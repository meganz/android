package mega.privacy.android.feature.mediaplayer.data.gateway

import android.content.Intent
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.mediaplayer.data.model.AudioControllerState

/**
 * Gateway that abstracts all Media3 [androidx.media3.session.MediaController] and
 * [mega.privacy.android.app.mediaplayer.service.AudioPlayerService] interactions.
 *
 * Consumers (e.g. [mega.privacy.android.app.mediaplayer.AudioPlayerViewModel]) depend only on
 * this interface, allowing full unit-test coverage via mock implementations.
 */
interface AudioMediaControllerGateway {

    /**
     * Emits a new [AudioControllerState] snapshot on every meaningful player-state change,
     * including periodic position/duration updates. Replays the latest value to new collectors.
     */
    val playerState: Flow<AudioControllerState>

    /**
     * Starts [mega.privacy.android.app.mediaplayer.service.AudioPlayerService] as a foreground
     * service, forwarding [intent] extras and data URI.
     */
    fun startService(intent: Intent)

    /** Resumes or starts playback. */
    fun play()

    /** Pauses playback. */
    fun pause()

    /** Seeks to [positionMs] in the current media item. */
    fun seekTo(positionMs: Long)

    /** Skips to the next media item in the queue. */
    fun skipToNext()

    /** Skips to the previous media item in the queue. */
    fun skipToPrevious()

    /** Enables or disables shuffle mode. */
    fun setShuffleEnabled(enabled: Boolean)

    /** Sets the repeat mode to one of [androidx.media3.common.Player] REPEAT_MODE_* constants. */
    fun setRepeatMode(mode: Int)

    /** Stops playback without releasing the player. */
    fun stop()

    /** Sets the playback speed. A value of `1.0` is normal speed. */
    fun setPlaybackSpeed(speed: Float)

    /** Seeks to the media item at [index] in the queue and starts from its default position. No-op if [index] is out of range. */
    fun seekToMediaItem(index: Int)

    /** Moves the media item at [fromIndex] to [toIndex] in the queue. No-op if either index is out of range. */
    fun moveMediaItem(fromIndex: Int, toIndex: Int)

    /** Releases the MediaController connection and all associated resources. */
    fun release()
}
