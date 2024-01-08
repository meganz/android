package mega.privacy.android.app.mediaplayer.gateway

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.mediaplayer.model.MediaPlaySources
import mega.privacy.android.app.mediaplayer.model.PlayerNotificationCreatedParams
import mega.privacy.android.app.mediaplayer.service.MediaPlayerCallback
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode

/**
 * The media player gateway
 */
interface MediaPlayerGateway {

    /**
     * Create player
     *
     * @param shuffleEnabled true is shuffle enable, otherwise is false
     * @param shuffleOrder ShuffleOrder
     * @param repeatToggleMode RepeatToggleMode
     * @param nameChangeCallback the callback when name is changed
     * @param mediaPlayerCallback MediaPlayerCallback
     */
    fun createPlayer(
        shuffleEnabled: Boolean? = null,
        shuffleOrder: ShuffleOrder? = null,
        repeatToggleMode: RepeatToggleMode,
        nameChangeCallback: (title: String?, artist: String?, album: String?) -> Unit,
        mediaPlayerCallback: MediaPlayerCallback,
    )

    /**
     * create player control notification
     *
     * @param playerNotificationParams PlayerNotificationParams
     */
    fun createPlayerControlNotification(playerNotificationParams: PlayerNotificationCreatedParams)

    /**
     * Get the current media item
     *
     * @return MediaItem
     */
    fun getCurrentMediaItem(): MediaItem?

    /**
     * Get the duration of current playing item
     *
     * @return of current playing item
     */
    fun getCurrentItemDuration(): Long

    /**
     * Set the new shuffle order
     *
     * @param newShuffleOrder new shuffle order
     */
    fun setShuffleOrder(newShuffleOrder: ShuffleOrder)

    /**
     * Get the current position of playing item
     *
     * @return current position of playing item
     */
    fun getCurrentPlayingPosition(): Long

    /**
     * Set play when ready for player
     *
     * @param playWhenReady true is play when ready, otherwise is false
     */
    fun setPlayWhenReady(playWhenReady: Boolean)

    /**
     * Get play when ready
     *
     * @return true is play when ready, otherwise is false
     */
    fun getPlayWhenReady(): Boolean

    /**
     * Remove the item from player
     *
     * @param index index of the item removed
     * @return the id of will be playing media item
     */
    fun mediaItemRemoved(index: Int): String?

    /**
     * Get the playback state
     *
     * @return playback state
     */
    fun getPlaybackState(): Int?

    /**
     * Player prepare
     */
    fun playerPrepare()

    /**
     * Stop player
     */
    fun playerStop()

    /**
     * Release player
     */
    fun playerRelease()

    /**
     * Player seek to other item
     *
     * @param index the index of item that will be played
     */
    fun playerSeekTo(index: Int)

    /**
     * Player seed to playing position
     *
     * @param positionInMs the position ms
     */
    fun playerSeekToPositionInMs(positionInMs: Long)

    /**
     * Build play sources for player
     *
     * @param mediaPlaySources MediaPlaySources
     */
    fun buildPlaySources(mediaPlaySources: MediaPlaySources)

    /**
     * Remove the listener from player
     *
     * @param listener removed listener
     */
    fun removeListener(listener: Player.Listener)

    /**
     * Return the media player whether is playing
     *
     * @return true is playing, otherwise is false.
     */
    fun mediaPlayerIsPlaying(): Boolean

    /**
     * Media player retry
     *
     * @param isRetry true is retry, otherwise is false.
     */
    fun mediaPlayerRetry(isRetry: Boolean)

    /**
     * Player notification invalidate
     */
    fun invalidatePlayerNotification()

    /**
     * Player notification set player
     */
    fun setPlayerForNotification()

    /**
     * Clear player of player notification
     */
    fun clearPlayerForNotification()

    /**
     * Set repeat mode
     *
     * @param repeatToggleMode RepeatToggleMode
     */
    fun setRepeatToggleMode(repeatToggleMode: RepeatToggleMode)

    /**
     * Add the listener for player
     *
     * @param listener Player.Listener
     */
    fun addPlayerListener(listener: Player.Listener)

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
        playerView: PlayerView,
        useController: Boolean = true,
        controllerShowTimeoutMs: Int = 0,
        controllerHideOnTouch: Boolean = false,
        isAudioPlayer: Boolean = true,
        showShuffleButton: Boolean? = null,
    )

    /**
     * Add subtitle for video
     *
     * @param subtitleFileUrl the subtitle file link
     * @return true is success, otherwise is failed
     */
    fun addSubtitle(subtitleFileUrl: String): Boolean

    /**
     * For show subtitle after the subtitle file has been set
     */
    fun showSubtitle()

    /**
     * For hide subtitle after the subtitle file has been set
     */
    fun hideSubtitle()

    /**
     * Update the playback speed
     *
     * @param speed playback speed
     */
    fun updatePlaybackSpeed(speed: Float)

    /**
     * Update the mediaNotAllowPlayState
     *
     * @param value true is not allow play, otherwise is false
     */
    fun updateMediaNotAllowPlayState(value: Boolean)

    /**
     * Monitor the mediaNotAllowPlayState
     *
     * @return mediaNotAllowPlayState
     */
    fun monitorMediaNotAllowPlayState(): Flow<Boolean>
}