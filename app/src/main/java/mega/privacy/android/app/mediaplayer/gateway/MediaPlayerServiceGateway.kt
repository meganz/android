package mega.privacy.android.app.mediaplayer.gateway

import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.mediaplayer.model.RepeatToggleMode
import mega.privacy.android.app.mediaplayer.service.Metadata

/**
 * The media player service gateway
 */
interface MediaPlayerServiceGateway {
    /**
     *  Metadata update
     *
     *  @return Flow<Metadata>
     */
    fun metadataUpdate(): Flow<Metadata>

    /**
     * video size update
     *
     * @return Flow<Pair<Int, Int>> first is video width, second is video height
     */
    fun videoSizeUpdate(): Flow<Pair<Int, Int>>

    /**
     * Stop audio player
     */
    fun stopAudioPlayer()

    /**
     * Close video player UI
     */
    fun mainPlayerUIClosed()

    /**
     * Judge the player whether is playing
     *
     * @return true is playing, otherwise is false.
     */
    fun playing(): Boolean

    /**
     * Seek to the index
     *
     * @param index the index that is sought to
     */
    fun seekTo(index: Int)

    /**
     * Set playWhenReady
     *
     * @param playWhenReady playWhenReady
     */
    fun setPlayWhenReady(playWhenReady: Boolean)

    /**
     * Remove the listener from player
     *
     * @param listener removed listener
     */
    fun removeListener(listener: Player.Listener)

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
     * Set repeat mode for video
     *
     * @param repeatToggleMode RepeatToggleMode
     */
    fun setRepeatModeForVideo(repeatToggleMode: RepeatToggleMode)

    /**
     * Setup player view
     *
     * @param playerView PlayerView
     * @param useController useController
     * @param controllerShowTimeoutMs controllerShowTimeoutMs
     * @param isAudioPlayer true is audio player, otherwise is false
     * @param controllerHideOnTouch controllerHideOnTouch
     * @param showShuffleButton showShuffleButton
     */
    fun setupPlayerView(
        playerView: StyledPlayerView,
        useController: Boolean = true,
        controllerShowTimeoutMs: Int = 0,
        controllerHideOnTouch: Boolean = false,
        isAudioPlayer: Boolean = true,
        showShuffleButton: Boolean? = null,
    )
}