package mega.privacy.android.feature.mediaplayer.data.gateway

import android.view.TextureView
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.feature.mediaplayer.data.model.VideoControllerState
import mega.privacy.android.feature.mediaplayer.data.model.VideoPlaybackEvent

/**
 * Gateway that abstracts every player interaction of the revamped video player.
 *
 * Consumers depend only on this interface, so they can be unit tested against a mock and, once the
 * player moves into this module, need no `:app` types.
 *
 * Mirrors [AudioMediaControllerGateway]: state flows out through [playerState] and nothing is read
 * back synchronously, so a caller can never mix values sampled at different instants. One-shot
 * signals travel on [events] rather than in the state.
 *
 * Unlike the SDK-facing gateways in `:data`, this one is a player-control seam and is meant to be
 * injected straight into the player ViewModel, exactly as [AudioMediaControllerGateway] is. It has
 * to be: [attachSurface] takes a `TextureView`, so only the UI layer can drive it, and `:domain`
 * is a pure JVM module that cannot host a Media3 type for a use case to wrap.
 *
 * The implementation wraps an ExoPlayer directly for now. It is replaced by a Media3
 * `MediaController` bound to a `VideoPlayerService` in a later phase; because `MediaController`
 * also implements `Player`, every method here keeps its signature through that swap.
 */
interface VideoMediaControllerGateway {

    /**
     * Emits a new [VideoControllerState] on every meaningful player change, including periodic
     * position updates. Replays the latest value to new collectors.
     */
    val playerState: Flow<VideoControllerState>

    /**
     * Emits each [VideoPlaybackEvent] once, in order, with no replay, to a single collector — a
     * second collector would take events away from the first rather than see the same ones.
     *
     * Buffered without bound rather than single-slot: a [VideoPlaybackEvent.ItemLeft] carries the
     * only record of where playback left an item, so no later event and no buffer limit may
     * displace it before the collector has seen it. It lives on the gateway rather than in a separate injectable queue so
     * that each player owns its own stream — one shared queue behind an unscoped gateway would let
     * two players consume each other's events.
     */
    val events: Flow<VideoPlaybackEvent>

    /** Resumes or starts playback. */
    fun play()

    /** Pauses playback. */
    fun pause()

    /**
     * Sets the playback intent without asserting whether the player is already playing.
     *
     * Kept separate from [play] and [pause] because the caller distinguishes an automatic resume
     * from an explicit user action — see [VideoControllerState.pausedByUserRequest].
     */
    fun setPlayWhenReady(playWhenReady: Boolean)

    /** Seeks to [positionMs] within the current item. */
    fun seekTo(positionMs: Long)

    /** Seeks to the item at queue [index] and plays from its default position. */
    fun seekToMediaItem(index: Int)

    /** Sets the repeat mode. */
    fun setRepeatMode(mode: RepeatToggleMode)

    /** Sets the playback speed; `1.0` is normal speed. */
    fun setPlaybackSpeed(speed: Float)

    /** Re-prepares the player after an error, as driven by the retry counter in the ViewModel. */
    fun retry()

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

    /** Adds a subtitle track from [subtitleFileUrl] and returns whether it was accepted. */
    fun addSubtitle(subtitleFileUrl: String): Boolean

    /** Shows the subtitle track added by [addSubtitle]. */
    fun showSubtitle()

    /** Hides the subtitle track without removing it. */
    fun hideSubtitle()

    /**
     * Releases the player and all associated resources. The owner calls this from its own cleanup.
     *
     * Call on the main thread, as with [attachSurface] and [clearSurface]: the player is created
     * there and only accepts calls from the thread that owns it.
     */
    fun release()
}
