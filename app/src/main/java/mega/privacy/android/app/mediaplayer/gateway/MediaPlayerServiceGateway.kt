package mega.privacy.android.app.mediaplayer.gateway

import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView

/**
 * The media player service gateway
 */
interface MediaPlayerServiceGateway {

    /**
     * Add the listener for player
     *
     * @param listener Player.Listener
     */
    fun addPlayerListener(listener: Player.Listener)

    /**
     * Get the playback state
     *
     * @return playback state
     */
    fun getPlaybackState(): Int

    /**
     * Get the current media item
     *
     * @return MediaItem
     */
    fun getCurrentMediaItem(): MediaItem?

    /**
     * Setup player view
     *
     * @param playerView PlayerView
     * @param useController useController
     * @param controllerShowTimeoutMs controllerShowTimeoutMs
     * @param repeatToggleModes repeatToggleModes
     * @param controllerHideOnTouch controllerHideOnTouch
     * @param showShuffleButton showShuffleButton
     * @param visibilityCallback visibilityCallback
     * @param clickedCallback clickedCallback
     */
    fun setupPlayerView(
        playerView: PlayerView,
        useController: Boolean = true,
        controllerShowTimeoutMs: Int = 0,
        controllerHideOnTouch: Boolean = false,
        repeatToggleModes: Int? = null,
        showShuffleButton: Boolean? = null,
        visibilityCallback: ((visibility: Int) -> Unit)? = null,
        clickedCallback: (() -> Unit)? = null,
    )
}