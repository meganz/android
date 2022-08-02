package mega.privacy.android.app.mediaplayer.facade

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.STATE_ENDED
import com.google.android.exoplayer2.Player.STATE_IDLE
import com.google.android.exoplayer2.source.ShuffleOrder
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.util.EventLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.MediaMegaPlayer
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerServiceGateway
import mega.privacy.android.app.mediaplayer.model.MediaPlaySources
import mega.privacy.android.app.mediaplayer.service.MediaPlayerCallback
import mega.privacy.android.app.mediaplayer.service.MetadataExtractor
import mega.privacy.android.app.mediaplayer.model.PlayerNotificationCreatedParams
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import timber.log.Timber

/**
 * The implementation of MediaPlayerGateway
 */
class MediaPlayerFacade constructor(
    @ApplicationContext private val context: Context,
) : MediaPlayerGateway, MediaPlayerServiceGateway {

    private lateinit var exoPlayer: ExoPlayer
    private lateinit var player: MediaMegaPlayer
    private var playerNotificationManager: PlayerNotificationManager? = null
    private var notificationDismissed = false

    override fun createPlayer(
        shuffleEnabled: Boolean,
        shuffleOrder: ShuffleOrder,
        repeatMode: Int,
        nameChangeCallback: (title: String?, artist: String?, album: String?) -> Unit,
        mediaPlayerCallback: MediaPlayerCallback,
    ) {
        val trackSelector = DefaultTrackSelector(context)
        val renderersFactory = DefaultRenderersFactory(context).setExtensionRendererMode(
            DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        exoPlayer = ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setSeekBackIncrementMs(INCREMENT_TIME_IN_MS)
            .build().apply {
                addListener(MetadataExtractor { title, artist, album ->
                    nameChangeCallback(title, artist, album)
                })
                addListener(object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        mediaPlayerCallback.onMediaItemTransitionCallback(
                            handle = mediaItem?.mediaId,
                            isUpdateName = reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        super.onIsPlayingChanged(isPlaying)
                        mediaPlayerCallback.onIsPlayingChangedCallback(isPlaying)
                    }

                    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                        mediaPlayerCallback.onShuffleModeEnabledChangedCallback(shuffleEnabled)
                    }

                    override fun onRepeatModeChanged(repeatMode: Int) {
                        mediaPlayerCallback.onRepeatModeChangedCallback(repeatMode)
                    }

                    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                        mediaPlayerCallback.onPlayWhenReadyChangedCallback(playWhenReady)
                        if (playWhenReady && notificationDismissed) {
                            playerNotificationManager?.setPlayer(player)
                            notificationDismissed = false
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        mediaPlayerCallback.onPlaybackStateChangedCallback(playbackState)
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        mediaPlayerCallback.onPlayerErrorCallback()
                    }
                })
                addAnalyticsListener(object :
                    EventLogger(trackSelector, "MediaPlayer") {
                    override fun logd(msg: String) {
                        Timber.d(msg)
                    }

                    override fun loge(msg: String) {
                        Timber.e(msg)
                    }
                })
                shuffleModeEnabled = shuffleEnabled
                this.repeatMode = repeatMode
                setShuffleOrder(shuffleOrder)
            }
        player = MediaMegaPlayer(exoPlayer)
    }

    override fun createPlayerControlNotification(playerNotificationParams: PlayerNotificationCreatedParams) {
        with(playerNotificationParams) {
            playerNotificationManager = PlayerNotificationManager.Builder(
                context,
                notificationId,
                channelId
            ).setChannelNameResourceId(channelNameResourceId)
                .setChannelDescriptionResourceId(channelDescriptionResourceId)
                .setMediaDescriptionAdapter(object :
                    PlayerNotificationManager.MediaDescriptionAdapter {
                    override fun getCurrentContentTitle(player: Player): String {
                        return metadata.value?.title ?: metadata.value?.nodeName ?: ""
                    }

                    override fun createCurrentContentIntent(player: Player): PendingIntent? {
                        return pendingIntent
                    }

                    override fun getCurrentContentText(player: Player): String {
                        return metadata.value?.artist ?: ""
                    }

                    override fun getCurrentLargeIcon(
                        player: Player,
                        callback: PlayerNotificationManager.BitmapCallback,
                    ): Bitmap? {
                        val thumbnail = thumbnail.value
                        if (thumbnail == null || !thumbnail.exists()) {
                            return ContextCompat.getDrawable(context,
                                R.drawable.ic_default_audio_cover)?.toBitmap()
                        }
                        return BitmapFactory.decodeFile(thumbnail.absolutePath,
                            BitmapFactory.Options())
                    }
                })
                .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                    override fun onNotificationPosted(
                        notificationId: Int,
                        notification: Notification,
                        ongoing: Boolean,
                    ) {
                        onNotificationPostedCallback(notificationId, notification, ongoing)
                    }

                    override fun onNotificationCancelled(
                        notificationId: Int,
                        dismissedByUser: Boolean,
                    ) {
                        if (dismissedByUser) {
                            playerNotificationManager?.setPlayer(null)
                            notificationDismissed = true
                        }
                    }
                })
                .build().apply {
                    setSmallIcon(smallIcon)
                    setUseChronometer(useChronometer)
                    setUseNextActionInCompactView(useNextActionInCompactView)
                    setUsePreviousActionInCompactView(usePreviousActionInCompactView)
                    setPlayer(player)
                }
        }
    }

    override fun getMediaMegaPlayer(): MediaMegaPlayer = player

    override fun getCurrentMediaItem(): MediaItem? = player.currentMediaItem

    override fun mediaPlayerIsPlaying(): Boolean =
        player.playWhenReady && player.playbackState != STATE_ENDED

    override fun mediaPlayerRetry(isRetry: Boolean) {
        if (isRetry && player.playbackState == STATE_IDLE) {
            player.prepare()
        }
    }

    override fun setShuffleOrder(newShuffleOrder: ShuffleOrder) {
        exoPlayer.setShuffleOrder(newShuffleOrder)
    }

    override fun getCurrentPosition(): Long = exoPlayer.currentPosition

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        player.playWhenReady = playWhenReady
    }

    override fun getPlayWhenReady(): Boolean = player.playWhenReady

    override fun mediaItemRemoved(index: Int): String? {
        if (index < player.mediaItemCount) {
            val nextIndex = player.nextMediaItemIndex
            player.removeMediaItem(index)
            if (nextIndex != C.INDEX_UNSET) {
                return player.getMediaItemAt(nextIndex).mediaId
            }
        }
        return null
    }

    override fun playerPrepare() {
        player.prepare()
    }

    override fun playerStop() {
        player.stop()
    }

    override fun playerRelease() {
        player.release()
    }

    override fun playerSeekTo(index: Int) {
        player.seekTo(index, 0)
    }

    override fun buildPlaySources(mediaPlaySources: MediaPlaySources) {
        with(mediaPlaySources) {
            if (newIndexForCurrentItem == INVALID_VALUE) {
                player.setMediaItems(mediaItems)
            } else {
                val oldIndexForCurrentItem = player.currentMediaItemIndex
                val oldItemsCount = player.mediaItemCount
                if (oldIndexForCurrentItem != oldItemsCount - 1) {
                    player.removeMediaItems(oldIndexForCurrentItem + 1, oldItemsCount)
                }
                if (oldIndexForCurrentItem != 0) {
                    player.removeMediaItems(0, oldIndexForCurrentItem)
                }

                if (newIndexForCurrentItem != 0) {
                    player.addMediaItems(0, mediaItems.subList(0, newIndexForCurrentItem))
                }
                if (newIndexForCurrentItem != mediaItems.size - 1) {
                    player.addMediaItems(
                        mediaItems.subList(newIndexForCurrentItem + 1, mediaItems.size)
                    )
                }
            }
        }
    }

    override fun removeListener(listener: Player.Listener) {
        player.removeListener(listener)
    }

    override fun invalidatePlayerNotification() {
        playerNotificationManager?.invalidate()
    }

    override fun setPlayerForNotification() {
        playerNotificationManager?.setPlayer(player)
    }

    override fun clearPlayerForNotification() {
        playerNotificationManager?.setPlayer(null)
    }

    override fun addPlayerListener(listener: Player.Listener) {
        player.wrappedPlayer.addListener(listener)
    }

    override fun getPlaybackState() = player.playbackState

    override fun setupPlayerView(
        playerView: PlayerView,
        useController: Boolean,
        controllerShowTimeoutMs: Int,
        controllerHideOnTouch: Boolean,
        repeatToggleModes: Int?,
        showShuffleButton: Boolean?,
        visibilityCallback: ((visibility: Int) -> Unit)?,
        clickedCallback: (() -> Unit)?,
    ) {
        with(playerView) {
            player = this@MediaPlayerFacade.player
            this.useController = useController
            this.controllerShowTimeoutMs = controllerShowTimeoutMs
            this.controllerHideOnTouch = controllerHideOnTouch
            repeatToggleModes?.run {
                setRepeatToggleModes(this)
            }
            showShuffleButton?.run {
                setShowShuffleButton(this)
            }
            visibilityCallback?.run {
                setControllerVisibilityListener { visibility ->
                    this(visibility)
                }
            }
            clickedCallback?.run {
                setOnClickListener {
                    this()
                }
            }
            showController()
        }
    }

    companion object {
        private const val INCREMENT_TIME_IN_MS = 15000L
    }
}