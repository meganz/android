package mega.privacy.android.app.mediaplayer.facade

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.util.EventLogger
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL
import com.google.android.exoplayer2.util.RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE
import com.google.android.exoplayer2.video.VideoSize
import com.google.common.collect.ImmutableList
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.MediaMegaPlayer
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.mapper.RepeatModeMapper
import mega.privacy.android.app.mediaplayer.mapper.RepeatToggleModeMapper
import mega.privacy.android.app.mediaplayer.model.MediaPlaySources
import mega.privacy.android.app.mediaplayer.model.PlayerNotificationCreatedParams
import mega.privacy.android.app.mediaplayer.model.RepeatToggleMode
import mega.privacy.android.app.mediaplayer.service.MediaPlayerCallback
import mega.privacy.android.app.mediaplayer.service.MetadataExtractor
import mega.privacy.android.app.middlelayer.reporter.CrashReporter
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import timber.log.Timber
import javax.inject.Inject

/**
 * The implementation of MediaPlayerGateway
 */
class MediaPlayerFacade @Inject constructor(
    @ApplicationContext private val context: Context,
    private val crashReporter: CrashReporter,
    private val repeatModeMapper: RepeatModeMapper,
    private val repeatToggleModeMapper: RepeatToggleModeMapper,
) : MediaPlayerGateway {

    private lateinit var exoPlayer: ExoPlayer
    private lateinit var player: MediaMegaPlayer
    private lateinit var trackSelector: DefaultTrackSelector
    private var playerNotificationManager: PlayerNotificationManager? = null
    private var notificationDismissed = false

    override fun createPlayer(
        shuffleEnabled: Boolean?,
        shuffleOrder: ShuffleOrder?,
        repeatToggleMode: RepeatToggleMode,
        nameChangeCallback: (title: String?, artist: String?, album: String?) -> Unit,
        mediaPlayerCallback: MediaPlayerCallback,
    ) {
        trackSelector = DefaultTrackSelector(context)
        val renderersFactory = DefaultRenderersFactory(context).setExtensionRendererMode(
            DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
        )
        exoPlayer = ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setSeekBackIncrementMs(INCREMENT_TIME_IN_MS)
            .build().apply {
                addListener(MetadataExtractor { title, artist, album ->
                    crashReporter.log("playing item: ${player.currentMediaItem?.mediaId}, title: $title")
                    nameChangeCallback(title, artist, album)
                })
                addListener(object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        mediaPlayerCallback.onMediaItemTransitionCallback(
                            handle = mediaItem?.mediaId,
                            isUpdateName = reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED
                        )
                    }

                    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                        mediaPlayerCallback.onShuffleModeEnabledChangedCallback(shuffleModeEnabled)
                    }

                    override fun onRepeatModeChanged(repeatMode: Int) {
                        mediaPlayerCallback.onRepeatModeChangedCallback(repeatModeMapper(repeatMode))
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

                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        super.onVideoSizeChanged(videoSize)
                        mediaPlayerCallback.onVideoSizeCallback(videoSize.width, videoSize.height)
                    }
                })
                addAnalyticsListener(object :
                    EventLogger("MediaPlayer") {
                    override fun logd(msg: String) {
                        Timber.d(msg)
                    }

                    override fun loge(msg: String) {
                        Timber.e(msg)
                    }
                })
                this.repeatMode = convertToRepeatMode(repeatToggleMode)
            }
        shuffleEnabled?.let {
            exoPlayer.shuffleModeEnabled = it
        }
        shuffleOrder?.let {
            exoPlayer.setShuffleOrder(it)
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
                    override fun getCurrentContentTitle(player: Player): String =
                        metadata.value?.title ?: metadata.value?.nodeName ?: ""

                    override fun createCurrentContentIntent(player: Player): PendingIntent? =
                        pendingIntent

                    override fun getCurrentContentText(player: Player): String =
                        metadata.value?.artist ?: ""

                    override fun getCurrentLargeIcon(
                        player: Player,
                        callback: PlayerNotificationManager.BitmapCallback,
                    ): Bitmap? {
                        val thumbnail = thumbnail.value
                        if (thumbnail == null || !thumbnail.exists()) {
                            return ContextCompat.getDrawable(
                                context,
                                R.drawable.ic_default_audio_cover
                            )?.toBitmap()
                        }
                        return BitmapFactory.decodeFile(
                            thumbnail.absolutePath,
                            BitmapFactory.Options()
                        )
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

    override fun getCurrentItemDuration(): Long = player.duration

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

    override fun getCurrentPlayingPosition(): Long = exoPlayer.currentPosition

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        player.playWhenReady = playWhenReady
    }

    override fun getPlayWhenReady(): Boolean = player.playWhenReady

    override fun mediaItemRemoved(index: Int): String? {
        crashReporter.log(
            "playingItem: ${player.currentMediaItem?.mediaId} removed item: ${
                player.getMediaItemAt(
                    index
                ).mediaId
            }"
        )
        if (index < player.mediaItemCount) {
            val nextIndex = player.nextMediaItemIndex
            player.removeMediaItem(index)
            if (nextIndex != C.INDEX_UNSET) {
                return player.getMediaItemAt(nextIndex).mediaId
            }
        }
        return null
    }

    override fun playerPrepare() = player.prepare()

    override fun playerStop() = player.stop()


    override fun playerRelease() = player.release()

    override fun playerSeekTo(index: Int) = player.seekTo(index, 0)

    override fun playerSeekToPositionInMs(positionInMs: Long) = player.seekTo(positionInMs)

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

    override fun removeListener(listener: Player.Listener) = player.removeListener(listener)

    override fun invalidatePlayerNotification() {
        playerNotificationManager?.invalidate()
    }

    override fun setPlayerForNotification() {
        playerNotificationManager?.setPlayer(player)
    }

    override fun clearPlayerForNotification() {
        playerNotificationManager?.setPlayer(null)
    }

    override fun setRepeatToggleMode(repeatToggleMode: RepeatToggleMode) {
        exoPlayer.repeatMode = convertToRepeatMode(repeatToggleMode)
    }

    private fun convertToRepeatMode(repeatToggleMode: RepeatToggleMode) =
        repeatToggleModeMapper(repeatToggleMode)

    override fun addPlayerListener(listener: Player.Listener) =
        player.wrappedPlayer.addListener(listener)

    override fun getPlaybackState() = player.playbackState

    override fun setupPlayerView(
        playerView: StyledPlayerView,
        useController: Boolean,
        controllerShowTimeoutMs: Int,
        controllerHideOnTouch: Boolean,
        isAudioPlayer: Boolean,
        showShuffleButton: Boolean?,
    ) {
        with(playerView) {
            player = this@MediaPlayerFacade.player
            this.useController = useController
            this.controllerShowTimeoutMs = controllerShowTimeoutMs
            this.controllerHideOnTouch = controllerHideOnTouch
            if (isAudioPlayer) {
                setRepeatToggleModes(REPEAT_TOGGLE_MODE_ONE or REPEAT_TOGGLE_MODE_ALL)
            }
            showShuffleButton?.run {
                setShowShuffleButton(this)
            }
            showController()
        }
    }

    override fun addSubtitle(subtitleFileUrl: String) {
        val videoUri: Uri? = player.currentMediaItem?.localConfiguration?.uri
        val mediaId = player.currentMediaItem?.mediaId
        val uri = Uri.parse(subtitleFileUrl)
        if (videoUri != null && mediaId != null && uri != null) {
            val subtitle = MediaItem.SubtitleConfiguration.Builder(uri)
                .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
            val mediaItem = MediaItem.Builder()
                .setUri(videoUri)
                .setMediaId(mediaId)
                .setSubtitleConfigurations(ImmutableList.of(subtitle))
                .build()
            val oldPosition = player.currentPosition
            // Stop player to set new media item that has subtitle
            playerStop()
            // Set new media item and start play video from the stop location
            player.setMediaItem(mediaItem, oldPosition)
            player.prepare()
            player.play()
            showSubtitle()
        }
    }

    override fun showSubtitle() {
        trackSelector.parameters = DefaultTrackSelector.Parameters.Builder(context)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false).build()
    }

    override fun hideSubtitle() {
        trackSelector.parameters = DefaultTrackSelector.Parameters.Builder(context)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build()
    }

    companion object {
        private const val INCREMENT_TIME_IN_MS = 15000L
    }
}