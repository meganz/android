package mega.privacy.android.app.mediaplayer.facade

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.Surface
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL
import androidx.media3.common.util.RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.ui.PlayerNotificationManager
import androidx.media3.ui.PlayerView
import com.google.common.collect.ImmutableList
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.flowOf
import mega.privacy.android.app.mediaplayer.MediaMegaPlayer
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.mapper.ExoPlayerRepeatModeMapper
import mega.privacy.android.app.mediaplayer.mapper.RepeatToggleModeByExoPlayerMapper
import mega.privacy.android.app.mediaplayer.model.MediaPlaySources
import mega.privacy.android.app.mediaplayer.model.PlayerNotificationCreatedParams
import mega.privacy.android.app.mediaplayer.model.SpeedPlaybackItem
import mega.privacy.android.app.mediaplayer.service.MediaPlayerCallback
import mega.privacy.android.app.mediaplayer.service.MetadataExtractor
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.icon.pack.R
import timber.log.Timber
import javax.inject.Inject
import androidx.core.net.toUri

/**
 * The implementation of MediaPlayerGateway
 */
@OptIn(UnstableApi::class)
class MediaPlayerFacade @Inject constructor(
    @ApplicationContext private val context: Context,
    private val crashReporter: CrashReporter,
    private val repeatToggleModeMapper: RepeatToggleModeByExoPlayerMapper,
    private val exoPlayerRepeatModeMapper: ExoPlayerRepeatModeMapper,
) : MediaPlayerGateway {

    private lateinit var exoPlayer: ExoPlayer
    private var player: MediaMegaPlayer? = null
    private lateinit var trackSelector: DefaultTrackSelector
    private var playerNotificationManager: PlayerNotificationManager? = null
    private var notificationDismissed = false

    @Volatile
    private var isSubtitleHidden = false

    @Volatile
    private var hasSwitchTrackOnInit = false

    override fun createPlayer(
        shuffleEnabled: Boolean?,
        shuffleOrder: ShuffleOrder?,
        repeatToggleMode: RepeatToggleMode,
        nameChangeCallback: (title: String?, artist: String?, album: String?) -> Unit,
        mediaPlayerCallback: MediaPlayerCallback,
    ): ExoPlayer {
        trackSelector = DefaultTrackSelector(context)
        val renderersFactory = DefaultRenderersFactory(context).setExtensionRendererMode(
            DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
        )
        exoPlayer = ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setSeekBackIncrementMs(INCREMENT_TIME_IN_MS)
            .build().apply {
                addListener(MetadataExtractor { title, artist, album ->
                    crashReporter.log("playing item: ${player?.currentMediaItem?.mediaId}, title: $title")
                    nameChangeCallback(title, artist, album)
                })
                addListener(object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        mediaPlayerCallback.onMediaItemTransitionCallback(
                            handle = mediaItem?.mediaId,
                            isUpdateName = reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED
                        )
                        hasSwitchTrackOnInit = false
                    }

                    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                        mediaPlayerCallback.onShuffleModeEnabledChangedCallback(shuffleModeEnabled)
                    }

                    override fun onRepeatModeChanged(repeatMode: Int) {
                        mediaPlayerCallback.onRepeatModeChangedCallback(
                            repeatToggleModeMapper(
                                repeatMode
                            )
                        )
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
                        Timber.e(error)
                        mediaPlayerCallback.onPlayerErrorCallback()
                    }

                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        super.onVideoSizeChanged(videoSize)
                        mediaPlayerCallback.onVideoSizeCallback(videoSize.width, videoSize.height)
                    }

                    override fun onTracksChanged(tracks: Tracks) {
                        val availableTrackTypes = (0 until exoPlayer.rendererCount).map {
                            exoPlayer.getRendererType(it)
                        }.filter { tracks.isTypeSelected(it) }

                        if (availableTrackTypes.any { it == C.TRACK_TYPE_TEXT }.not()
                            && tracks.isTypeSupported(C.TRACK_TYPE_TEXT)
                        ) {
                            switchRendererToTextTrackType()
                        }
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
        playerNotificationManager?.setPlayer(player)
        return exoPlayer
    }

    private fun switchRendererToTextTrackType() {
        if (hasSwitchTrackOnInit && isSubtitleHidden) return
        val mappedTrackInfo = trackSelector.currentMappedTrackInfo
        val mediaUri = exoPlayer.currentMediaItem?.localConfiguration?.uri
        if (mappedTrackInfo != null) {
            (0 until mappedTrackInfo.rendererCount).firstOrNull {
                mappedTrackInfo.getRendererType(it) == C.TRACK_TYPE_TEXT
            }?.let {
                Timber.d("SwitchTrackInfo: Switch renderer to text track type, the media uri: $mediaUri")
                val parameters = trackSelector.parameters.buildUpon()
                    .setRendererDisabled(it, false)
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .build()
                trackSelector.setParameters(parameters)
                exoPlayer.prepare()
                exoPlayer.play()
            }
                ?: Timber.d("SwitchTrackInfo: There is no text track type found, the media uri: $mediaUri")
        } else {
            Timber.d("SwitchTrackInfo: There is no mapped track info found, the media uri: $mediaUri")
        }
        if (!hasSwitchTrackOnInit) {
            hasSwitchTrackOnInit = true
        }
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
                            onNotificationCancelledCallback()
                        }
                    }
                })
                .build().apply {
                    setSmallIcon(smallIcon)
                    setUseChronometer(useChronometer)
                    setUseNextActionInCompactView(useNextActionInCompactView)
                    setUsePreviousActionInCompactView(usePreviousActionInCompactView)
                    setPlayer(player ?: ExoPlayer.Builder(context).build())
                }
        }
    }

    override fun getCurrentMediaItem(): MediaItem? = player?.currentMediaItem

    override fun getCurrentItemDuration(): Long = player?.duration ?: 0

    override fun mediaPlayerIsPlaying(): Boolean =
        player?.playWhenReady == true && player?.playbackState != STATE_ENDED

    override fun mediaPlayerRetry(isRetry: Boolean) {
        if (isRetry && player?.playbackState == STATE_IDLE) {
            player?.prepare()
        }
    }

    override fun setShuffleOrder(newShuffleOrder: ShuffleOrder) {
        if (exoPlayer.shuffleModeEnabled && newShuffleOrder.length > 2) {
            runCatching {
                exoPlayer.setShuffleOrder(newShuffleOrder)
            }.recover {
                Timber.e(it)
            }
        }
    }

    override fun getCurrentPlayingPosition(): Long = exoPlayer.currentPosition

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        player?.playWhenReady = playWhenReady
    }

    override fun getPlayWhenReady(): Boolean = player?.playWhenReady == true

    override fun mediaItemRemoved(index: Int): String? =
        player?.let { player ->
            crashReporter.log(
                "playingItem: ${player.currentMediaItem?.mediaId}, " +
                        "playing item index:${player.currentMediaItemIndex}, " +
                        "play sources size: ${player.mediaItemCount}, removed item: $index"
            )

            var result: String? = null
            if (index < player.mediaItemCount) {
                val nextIndex = player.nextMediaItemIndex
                crashReporter.log(
                    "nextIndex is $nextIndex, play sources size: ${player.mediaItemCount}"
                )
                player.removeMediaItem(index)
                if (nextIndex != C.INDEX_UNSET) {
                    if (nextIndex < player.mediaItemCount)
                        result = player.getMediaItemAt(nextIndex).mediaId
                }
                crashReporter.log("next media id: $result")
            }
            result
        }

    override fun playerPrepare() {
        player?.prepare()
    }

    override fun playerStop() {
        player?.stop()
    }

    override fun playerRelease() {
        player?.release()
        if (::exoPlayer.isInitialized) {
            exoPlayer.release()
        }
        hasSwitchTrackOnInit = false
    }

    override fun playerSeekTo(index: Int) {
        player?.seekTo(index, 0)
    }

    override fun playerSeekToPositionInMs(positionInMs: Long) {
        player?.seekTo(positionInMs)
    }

    override fun buildPlaySources(mediaPlaySources: MediaPlaySources) {
        with(mediaPlaySources) {
            if (newIndexForCurrentItem == INVALID_VALUE) {
                player?.setMediaItems(mediaItems)
            } else {
                player?.let { player ->
                    val oldIndexForCurrentItem = player.currentMediaItemIndex
                    val oldItemsCount = player.mediaItemCount
                    // Check the parameters whether matched the required of removeMediaItems() function
                    if (oldIndexForCurrentItem >= -1 && oldItemsCount >= 0) {
                        if (oldItemsCount > oldIndexForCurrentItem + 1) {
                            player.removeMediaItems(oldIndexForCurrentItem + 1, oldItemsCount)
                        }
                        if (oldIndexForCurrentItem > 0) {
                            player.removeMediaItems(0, oldIndexForCurrentItem)
                        }
                    }

                    // Check parameters to ensure "fromIndex >= 0 && toIndex <= size && toIndex >= fromIndex"
                    if (newIndexForCurrentItem in mediaItems.indices) {
                        if (newIndexForCurrentItem > 0) {
                            player.addMediaItems(0, mediaItems.subList(0, newIndexForCurrentItem))
                        }
                        if (mediaItems.size > newIndexForCurrentItem + 1) {
                            player.addMediaItems(
                                mediaItems.subList(newIndexForCurrentItem + 1, mediaItems.size)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun removeListener(listener: Player.Listener) {
        player?.removeListener(listener)
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

    override fun setRepeatToggleMode(repeatToggleMode: RepeatToggleMode) {
        exoPlayer.repeatMode = convertToRepeatMode(repeatToggleMode)
    }

    private fun convertToRepeatMode(repeatToggleMode: RepeatToggleMode) =
        exoPlayerRepeatModeMapper(repeatToggleMode)

    override fun addPlayerListener(listener: Player.Listener) {
        player?.wrappedPlayer?.addListener(listener)
    }

    override fun getPlaybackState() = player?.playbackState

    override fun setupPlayerView(
        playerView: PlayerView,
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

    override fun addSubtitle(subtitleFileUrl: String) =
        player?.let { player ->
            val videoUri: Uri? = player.currentMediaItem?.localConfiguration?.uri
            val mediaId = player.currentMediaItem?.mediaId
            Timber.d("SubtitleTesting, videoUri: $videoUri, mediaId: $mediaId, subtitleFileUrl: $subtitleFileUrl")
            val uri = subtitleFileUrl.toUri()
            if (videoUri != null && mediaId != null) {
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
                true
            } else {
                false
            }
        } == true


    override fun showSubtitle() {
        isSubtitleHidden = false
        if (!hasSwitchTrackOnInit) hasSwitchTrackOnInit = true
        trackSelector.parameters = DefaultTrackSelector.Parameters.Builder(context)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false).build()
    }

    override fun hideSubtitle() {
        isSubtitleHidden = true
        trackSelector.parameters = DefaultTrackSelector.Parameters.Builder(context)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build()
    }

    override fun updatePlaybackSpeed(item: SpeedPlaybackItem) {
        player?.playbackParameters = PlaybackParameters(item.speed)
    }

    override fun updateMediaNotAllowPlayState(value: Boolean) {
        player?.updateMediaNotAllowPlayState(value)
    }

    override fun monitorMediaNotAllowPlayState() =
        player?.monitorMediaNotAllowPlayState() ?: flowOf(false)

    override fun setSurface(surface: Surface) {
        exoPlayer.setVideoSurface(surface)
    }

    override fun getCurrentPlaybackSpeed(): Float = player?.playbackParameters?.speed ?: 1f

    override fun playNext() {
        player?.seekToNext()
    }

    override fun playPrev() {
        player?.seekToPrevious()
    }

    companion object {
        private const val INCREMENT_TIME_IN_MS = 15000L
    }
}
