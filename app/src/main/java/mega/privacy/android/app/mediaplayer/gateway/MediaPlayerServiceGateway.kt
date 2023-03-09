package mega.privacy.android.app.mediaplayer.gateway

import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.mediaplayer.model.PlaybackPositionState
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
     * Get current playing position
     *
     * @return current playing position
     */
    fun getCurrentPlayingPosition(): Long

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

    /**
     * Update playback position state
     *
     * @return Flow<PlaybackPositionState>
     */
    fun playbackPositionStateUpdate(): Flow<PlaybackPositionState>

    /**
     * Set resume playback position history for playing video
     *
     * @param playbackPosition playback position history
     */
    fun setResumePlaybackPosition(playbackPosition: Long?)

    /**
     * Set resume playback position history for playing video before build sources
     *
     * @param playbackPosition playback position history
     */
    fun setResumePlaybackPositionBeforeBuildSources(playbackPosition: Long?)

    /**
     * Set restart to play video
     */
    fun setRestartPlayVideo()

    /**
     * Set restart to play video before build sources
     */
    fun setRestartPlayVideoBeforeBuildSources()

    /**
     * Cancel playback position dialog
     */
    fun cancelPlaybackPositionDialog()

    /**
     * Cancel playback position dialog before build sources
     */
    fun cancelPlaybackPositionDialogBeforeBuildSources()

    /**
     * Add subtitle for video
     *
     * @param subtitleFileUrl the subtitle file link
     */
    fun addSubtitle(subtitleFileUrl: String)

    /**
     * For show subtitle after the subtitle file has been set
     */
    fun showSubtitle()

    /**
     * For hide subtitle after the subtitle file has been set
     */
    fun hideSubtitle()
}