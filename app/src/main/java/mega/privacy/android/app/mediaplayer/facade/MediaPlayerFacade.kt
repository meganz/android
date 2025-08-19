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
import androidx.core.net.toUri
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
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.ui.PlayerNotificationManager
import androidx.media3.ui.PlayerView
import com.google.common.collect.ImmutableList
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.flowOf
import mega.privacy.android.analytics.Analytics
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
import mega.privacy.mobile.analytics.event.VideoBufferingExceeded_1_SecondEvent
import timber.log.Timber
import javax.inject.Inject

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

    private val bandwidthMeter = DefaultBandwidthMeter.Builder(context).build()

    private val loggingTransferListener = object : androidx.media3.datasource.TransferListener {
        private var totalBytesTransferred = 0L
        private var transferStartTime = 0L
        private var lastLogTime = 0L
        private var lastTotalBytesTransferred = 0L

        override fun onTransferInitializing(
            source: androidx.media3.datasource.DataSource,
            dataSpec: androidx.media3.datasource.DataSpec,
            isNetwork: Boolean,
        ) {
            Timber.d("TransferListener initializing: ${dataSpec.uri}, isNetwork: $isNetwork")
        }

        override fun onTransferStart(
            source: androidx.media3.datasource.DataSource,
            dataSpec: androidx.media3.datasource.DataSpec,
            isNetwork: Boolean,
        ) {
            transferStartTime = System.currentTimeMillis()
            lastLogTime = transferStartTime
            lastTotalBytesTransferred = 0L
            Timber.d("TransferListener started: ${dataSpec.uri}, isNetwork: $isNetwork")
        }

        override fun onBytesTransferred(
            source: androidx.media3.datasource.DataSource,
            dataSpec: androidx.media3.datasource.DataSpec,
            isNetwork: Boolean,
            bytesTransferred: Int,
        ) {
            if (isNetwork && bytesTransferred > 0) {
                totalBytesTransferred += bytesTransferred
                val currentTime = System.currentTimeMillis()

                // Log only if 1 second has passed since last log
                if (currentTime - lastLogTime >= 1000) {
                    val bitrate = bandwidthMeter.bitrateEstimate
                    val duration = currentTime - transferStartTime

                    // Calculate current bitrate for the last second
                    val bytesInLastSecond = totalBytesTransferred - lastTotalBytesTransferred
                    val currentBitrate = bytesInLastSecond * 8

                    if (duration > 0) {
                        val averageBitrate = (totalBytesTransferred * 8 * 1000) / duration
                        Timber.d(
                            """
                                TransferListener Bandwidth:
                                Estimated: %.2f kbps
                                Current: %.2f kbps (last second)
                                Average: %.2f kbps (from start)
                                Total transferred: %.2f KB
                                Duration: %ds
                                Bytes in last second: %.2f KB
                            """.trimIndent(),
                            bitrate / 1024f,
                            currentBitrate / 1024f,
                            averageBitrate / 1024f,
                            totalBytesTransferred / 1024f,
                            duration / 1000,
                            bytesInLastSecond / 1024f
                        )
                    } else {
                        Timber.d(
                            """
                                TransferListener Bandwidth:
                                Estimated: %.2f kbps
                                Current: %.2f kbps (last second)
                                Total transferred: %.2f KB
                                Duration: %ds
                                Bytes in last second: %.2f KB
                            """.trimIndent(),
                            bitrate / 1024f,
                            currentBitrate / 1024f,
                            totalBytesTransferred / 1024f,
                            duration / 1000,
                            bytesInLastSecond / 1024f
                        )
                    }

                    lastLogTime = currentTime
                    lastTotalBytesTransferred = totalBytesTransferred
                }
            }
        }

        override fun onTransferEnd(
            source: androidx.media3.datasource.DataSource,
            dataSpec: androidx.media3.datasource.DataSpec,
            isNetwork: Boolean,
        ) {
            val currentTime = System.currentTimeMillis()
            val duration = currentTime - transferStartTime
            val bitrate = bandwidthMeter.bitrateEstimate

            if (duration > 0) {
                val averageBitrate = (totalBytesTransferred * 8 * 1000) / duration
                Timber.d(
                    """
                        TransferListener Transfer ended:
                        URI: %s
                        Total: %.2f KB
                        Duration: %ds
                        Avg bitrate: %.2f kbps
                        Estimated: %.2f kbps
                    """.trimIndent(),
                    dataSpec.uri,
                    totalBytesTransferred / 1024f,
                    duration / 1000,
                    averageBitrate / 1024f,
                    bitrate / 1024f
                )
            } else {
                Timber.d(
                    """
                        TransferListener Transfer ended:
                        URI: %s
                        Total: %.2f KB
                        Estimated: %.2f kbps
                    """.trimIndent(),
                    dataSpec.uri,
                    totalBytesTransferred / 1024f,
                    bitrate / 1024f
                )
            }

            totalBytesTransferred = 0L
            transferStartTime = 0L
            lastLogTime = 0L
            lastTotalBytesTransferred = 0L
        }
    }

    private val dataSourceFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
        .setUserAgent("ExoPlayer")
        .setTransferListener(loggingTransferListener)

    private val mediaSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory)

    private var bufferingStartTime: Long = 0L

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
                        startSavingBufferingState(playbackState)
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

    private fun startSavingBufferingState(playbackState: Int) {
        when (playbackState) {
            Player.STATE_BUFFERING -> bufferingStartTime = System.currentTimeMillis()
            Player.STATE_READY -> {
                if (bufferingStartTime > 0) {
                    val duration = System.currentTimeMillis() - bufferingStartTime
                    if (duration > 1000) {
                        logBufferingEvent(duration)
                    }
                    bufferingStartTime = 0L
                }
            }
        }
    }

    private fun logBufferingEvent(duration: Long) {
        val seconds = duration / 1000.0
        Analytics.tracker.trackEvent(VideoBufferingExceeded_1_SecondEvent)
        Timber.d("Buffering > 1s detected: %.2f s".format(seconds))
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
            runCatching {
                val mediaSources = convertMediaItemsToMediaSources(mediaItems)

                if (newIndexForCurrentItem == INVALID_VALUE) {
                    exoPlayer.setMediaSources(mediaSources)
                } else {
                    val oldIndexForCurrentItem = exoPlayer.currentMediaItemIndex
                    val oldItemsCount = exoPlayer.mediaItemCount

                    if (oldIndexForCurrentItem >= -1 && oldItemsCount >= 0) {
                        // Remove items after the current item
                        if (oldItemsCount > oldIndexForCurrentItem + 1) {
                            for (i in oldItemsCount - 1 downTo oldIndexForCurrentItem + 1) {
                                exoPlayer.removeMediaItem(i)
                            }
                        }
                        // Remove items before the current item
                        if (oldIndexForCurrentItem > 0) {
                            for (i in oldIndexForCurrentItem - 1 downTo 0) {
                                exoPlayer.removeMediaItem(i)
                            }
                        }
                    }

                    if (newIndexForCurrentItem in mediaSources.indices) {
                        if (newIndexForCurrentItem > 0) {
                            exoPlayer.addMediaSources(
                                0,
                                mediaSources.subList(0, newIndexForCurrentItem)
                            )
                        }
                        if (mediaSources.size > newIndexForCurrentItem + 1) {
                            exoPlayer.addMediaSources(
                                mediaSources.subList(newIndexForCurrentItem + 1, mediaSources.size)
                            )
                        }
                    }
                }
            }.onFailure { e ->
                Timber.e(e, "Error building play sources")
                e.message?.let {
                    crashReporter.log(it)
                }
            }
        }
    }

    /**
     * Convert MediaItems to MediaSources using the custom MediaSource.Factory
     *
     * @param mediaItems List of MediaItems to convert
     * @return List of MediaSources
     */
    private fun convertMediaItemsToMediaSources(mediaItems: List<MediaItem>): List<MediaSource> {
        return mediaItems.map { mediaItem ->
            runCatching {
                mediaSourceFactory.createMediaSource(mediaItem)
            }.onFailure { e ->
                Timber.e(e, "Error creating MediaSource for mediaItem: ${mediaItem.mediaId}")
                e.message?.let {
                    crashReporter.log(it)
                }
            }.getOrThrow()
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
