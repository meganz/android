package mega.privacy.android.app.mediaplayer.gateway

import android.content.Intent
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.mediaplayer.model.AudioControllerState

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

    /** Releases the MediaController connection and all associated resources. */
    fun release()
}
