package mega.privacy.android.feature.mediaplayer.data.gateway

import android.net.Uri
import android.view.TextureView
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.mediaplayer.data.model.VideoControllerState

/**
 * Gateway that abstracts the player interactions of the revamped video player.
 *
 * Consumers depend only on this interface, so they can be unit tested against a mock and, once the
 * player moves into this module, need no `:app` types.
 *
 * Mirrors [AudioMediaControllerGateway] in shape: state flows out through [playerState] and nothing
 * is read back synchronously, so a caller can never mix values sampled at different instants.
 *
 * **Carries only what the player calls today.** Every member here has a consumer. A method without
 * one cannot be exercised on a device, and a test for it can do no more than restate it, so the
 * audio gateway's fuller shape is not a template to copy. Playback controls, the queue and
 * subtitles each add their own methods when the feature driving them arrives — written against
 * that caller and verified on a device.
 *
 * Unlike the SDK-facing gateways in `:data`, this one is a player-control seam and is meant to be
 * injected straight into the player ViewModel, exactly as [AudioMediaControllerGateway] is. It has
 * to be: [attachSurface] takes a `TextureView`, so only the UI layer can drive it, and `:domain`
 * is a pure JVM module that cannot host a Media3 type for a use case to wrap.
 */
interface VideoMediaControllerGateway {

    /**
     * Emits a new [VideoControllerState] on every meaningful player change, including periodic
     * position updates while playing. Replays the latest value to new collectors.
     */
    val playerState: Flow<VideoControllerState>

    /**
     * Replaces whatever the player holds with the single item at [uri], identified by [mediaId],
     * and prepares it. Does not start playback — the caller decides that with [play].
     *
     * Takes one item rather than a list because nothing builds a queue yet.
     *
     * @param mediaId identifies the item in player callbacks, which is how a transition is matched
     * back to the node it belongs to.
     */
    fun setPlaySource(uri: Uri, mediaId: String)

    /** Starts playback. */
    fun play()

    /** Pauses playback, keeping the item and the position it reached. */
    fun pause()

    /**
     * Moves playback to [positionMs] within the current item.
     *
     * The caller is responsible for keeping [positionMs] within the item: the player clamps out
     * of range values silently, which hides a caller that got the duration wrong.
     */
    fun seekTo(positionMs: Long)

    /**
     * Moves playback back by the increment the player was built with.
     *
     * The player's own command rather than arithmetic on the position the UI holds: that one is
     * sampled on a timer and can be half a second behind, which would make the landing point
     * drift from one press to the next. Clamping at the start of the item is the player's too.
     */
    fun seekBack()

    /** Moves playback forward by the increment the player was built with, clamped at the end. */
    fun seekForward()

    /** Skips to the next item in the queue. No-op when there is none. */
    fun skipToNext()

    /**
     * Skips to the previous item in the queue, or restarts the current one when playback is past
     * its first few seconds — Media3 `seekToPrevious` semantics. Until the queue is built, every
     * press takes the second path.
     */
    fun skipToPrevious()

    /**
     * Binds the player's video output to [textureView].
     *
     * A `TextureView` rather than a `SurfaceView` because the player screen needs to read frames
     * back for screenshots, and because the Compose zoom gesture applies a `graphicsLayer`
     * transform that a `SurfaceView`'s independent surface would silently ignore.
     */
    fun attachSurface(textureView: TextureView)

    /** Detaches the video output, leaving playback running. */
    fun clearSurface()

    /**
     * Releases the player and all associated resources. The owner calls this from its own cleanup.
     *
     * Call on the main thread, as with [attachSurface] and [clearSurface]: the player is created
     * there and only accepts calls from the thread that owns it.
     */
    fun release()
}
