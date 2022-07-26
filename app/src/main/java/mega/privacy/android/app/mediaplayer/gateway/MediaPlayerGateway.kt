package mega.privacy.android.app.mediaplayer.gateway

import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ShuffleOrder
import mega.privacy.android.app.mediaplayer.MediaMegaPlayer
import mega.privacy.android.app.mediaplayer.model.MediaPlaySources
import mega.privacy.android.app.mediaplayer.model.PlayerNotificationCreatedParams
import mega.privacy.android.app.mediaplayer.service.MediaPlayerCallback

/**
 * The media player gateway
 */
interface MediaPlayerGateway {

    /**
     * Create player
     *
     * @param shuffleEnabled true is shuffle enable, otherwise is false
     * @param shuffleOrder ShuffleOrder
     * @param repeatMode repeat mode
     * @param nameChangeCallback the callback when name is changed
     * @param mediaPlayerCallback MediaPlayerCallback
     */
    fun createPlayer(
        shuffleEnabled: Boolean,
        shuffleOrder: ShuffleOrder,
        repeatMode: Int,
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
     * Get MediaMegaPlayer
     *
     * @return MediaMegaPlayer
     */
    fun getMediaMegaPlayer(): MediaMegaPlayer

    /**
     * Get the current media item
     *
     * @return MediaItem
     */
    fun getCurrentMediaItem(): MediaItem?

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
    fun getCurrentPosition(): Long

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
    fun getPlaybackState(): Int

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
     * Player seek to
     */
    fun playerSeekTo(index: Int)

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
}