package mega.privacy.android.app.mediaplayer.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.util.EventLogger
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.R
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.di.MegaApiFolder
import mega.privacy.android.app.mediaplayer.AudioPlayerActivity
import mega.privacy.android.app.mediaplayer.MediaMegaPlayer
import mega.privacy.android.app.mediaplayer.MediaPlayerActivity
import mega.privacy.android.app.mediaplayer.miniplayer.MiniAudioPlayerController
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil.AUDIOFOCUS_DEFAULT
import mega.privacy.android.app.utils.ChatUtil.STREAM_MUSIC_DEFAULT
import mega.privacy.android.app.utils.ChatUtil.abandonAudioFocus
import mega.privacy.android.app.utils.ChatUtil.getAudioFocus
import mega.privacy.android.app.utils.ChatUtil.getRequest
import mega.privacy.android.app.utils.Constants.EVENT_NOT_ALLOW_PLAY
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_AUDIO_PLAYER_ID
import mega.privacy.android.app.utils.wrapper.GetOfflineThumbnailFileWrapper
import nz.mega.sdk.MegaApiAndroid
import timber.log.Timber
import javax.inject.Inject

@Suppress("DEPRECATION")
@AndroidEntryPoint
open class MediaPlayerService : LifecycleService(), LifecycleEventObserver {

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    @MegaApiFolder
    @Inject
    lateinit var megaApiFolder: MegaApiAndroid

    @Inject
    lateinit var dbHandler: DatabaseHandler

    @Inject
    lateinit var offlineThumbnailFileWrapper: GetOfflineThumbnailFileWrapper

    private val binder by lazy { MediaPlayerServiceBinder(this) }
    private var initialized = false

    lateinit var viewModel: MediaPlayerServiceViewModel

    private lateinit var trackSelector: DefaultTrackSelector
    lateinit var player: MediaMegaPlayer
        private set

    private lateinit var exoPlayer: ExoPlayer

    private var playerNotificationManager: PlayerNotificationManager? = null
    private var notificationDismissed = false

    private val _metadata = MutableLiveData<Metadata>()
    val metadata: LiveData<Metadata> = _metadata

    private var needPlayWhenGoForeground = false
    private var needPlayWhenReceiveResumeCommand = false

    private val mainHandler = Handler(Looper.getMainLooper())

    // We need keep it as Runnable here, because we need remove it from handler later,
    // using lambda doesn't work when remove it from handler.
    private val resumePlayRunnable = Runnable {
        if (needPlayWhenReceiveResumeCommand) {
            setPlayWhenReady(true)
            needPlayWhenReceiveResumeCommand = false
        }
    }

    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioFocusRequested = false
    private val audioFocusListener =
        OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    if (player.playWhenReady) {
                        setPlayWhenReady(false)
                        needPlayWhenGoForeground = false
                        needPlayWhenReceiveResumeCommand = false
                    }
                }
            }
        }

    private val positionUpdateHandler = Handler()
    private val positionUpdateRunnable = object : Runnable {
        override fun run() {
            val currentPosition = exoPlayer.currentPosition
            // Up the frequency of refresh, keeping in sync with Exoplayer.
            positionUpdateHandler.postDelayed(this, 500)
            viewModel.setCurrentPosition(currentPosition)
        }
    }

    override fun onCreate() {
        super.onCreate()

        viewModel = MediaPlayerServiceViewModel(
            this,
            megaApi,
            megaApiFolder,
            dbHandler,
            offlineThumbnailFileWrapper
        )

        audioManager = (getSystemService(AUDIO_SERVICE) as AudioManager)
        audioFocusRequest = getRequest(audioFocusListener, AUDIOFOCUS_DEFAULT)

        createPlayer()
        observeLiveData()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    private fun createPlayer() {
        trackSelector = DefaultTrackSelector(this)
        val renderersFactory = DefaultRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

        exoPlayer = ExoPlayer.Builder(this, renderersFactory)
            .setTrackSelector(trackSelector)
            .setSeekBackIncrementMs(INCREMENT_TIME_IN_MS)
            .build().apply {
                addListener(MetadataExtractor { title, artist, album ->
                    val nodeName =
                        viewModel.getPlaylistItem(player.currentMediaItem?.mediaId)?.nodeName
                            ?: ""

                    if (title.isNullOrEmpty() && artist.isNullOrEmpty()
                        && album.isNullOrEmpty() && nodeName.isEmpty()
                    ) {
                        return@MetadataExtractor
                    }

                    _metadata.value = Metadata(title, artist, album, nodeName)

                    playerNotificationManager?.invalidate()
                })

                shuffleModeEnabled = viewModel.shuffleEnabled()
                repeatMode = viewModel.repeatMode()

                addListener(object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        val handle = mediaItem?.mediaId ?: return
                        viewModel.playingHandle = handle.toLong()

                        if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                            val nodeName = viewModel.getPlaylistItem(handle)?.nodeName ?: ""
                            _metadata.value = Metadata(null, null, null, nodeName)
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        super.onIsPlayingChanged(isPlaying)
                        if (isPlaying) {
                            positionUpdateHandler.post(positionUpdateRunnable)
                        } else {
                            positionUpdateHandler.removeCallbacks(positionUpdateRunnable)
                        }
                    }

                    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                        viewModel.setShuffleEnabled(shuffleModeEnabled)

                        if (shuffleModeEnabled) {
                            setShuffleOrder(viewModel.newShuffleOrder())
                        }
                    }

                    override fun onRepeatModeChanged(repeatMode: Int) {
                        viewModel.setRepeatMode(repeatMode)
                    }

                    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                        viewModel.setPaused(!playWhenReady, exoPlayer.currentPosition)

                        if (playWhenReady && notificationDismissed) {
                            playerNotificationManager?.setPlayer(player)
                            notificationDismissed = false
                        }

                        if (playWhenReady) {
                            if (viewModel.audioPlayer) {
                                pauseVideoPlayer(this@MediaPlayerService)
                            } else {
                                pauseAudioPlayer(this@MediaPlayerService)
                            }
                        }
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        when {
                            state == Player.STATE_ENDED && !viewModel.paused -> {
                                viewModel.setPaused(true, exoPlayer.currentPosition)
                            }
                            state == Player.STATE_READY && viewModel.paused && player.playWhenReady -> {
                                viewModel.setPaused(false, exoPlayer.currentPosition)
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        viewModel.onPlayerError()
                        positionUpdateHandler.removeCallbacks(positionUpdateRunnable)
                    }
                })

                addAnalyticsListener(object :
                    EventLogger(this@MediaPlayerService.trackSelector, "MediaPlayer") {
                    override fun logd(msg: String) {
                        Timber.d(msg)
                    }

                    override fun loge(msg: String) {
                        Timber.e(msg)
                    }
                })

                setShuffleOrder(viewModel.shuffleOrder)
            }

        player = MediaMegaPlayer(exoPlayer)
    }

    private fun createPlayerControlNotification() {
        playerNotificationManager = PlayerNotificationManager.Builder(
            applicationContext,
            PLAYBACK_NOTIFICATION_ID,
            NOTIFICATION_CHANNEL_AUDIO_PLAYER_ID
        ).setChannelNameResourceId(R.string.audio_player_notification_channel_name)
            .setChannelDescriptionResourceId(0)
            .setMediaDescriptionAdapter(object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun getCurrentContentTitle(player: Player): String {
                    val meta = _metadata.value ?: return ""
                    return meta.title ?: meta.nodeName
                }

                @SuppressLint("UnspecifiedImmutableFlag")
                @Nullable
                override fun createCurrentContentIntent(player: Player): PendingIntent? {
                    val intent = Intent(applicationContext, AudioPlayerActivity::class.java)
                    intent.putExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, false)
                    return PendingIntent.getActivity(
                        applicationContext,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }

                @Nullable
                override fun getCurrentContentText(player: Player): String {
                    val meta = _metadata.value ?: return ""
                    return meta.artist ?: ""
                }

                @Nullable
                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback,
                ): Bitmap? {
                    val thumbnail = viewModel.playingThumbnail.value
                    if (thumbnail == null || !thumbnail.exists()) {
                        return ContextCompat.getDrawable(
                            this@MediaPlayerService,
                            R.drawable.ic_default_audio_cover
                        )?.toBitmap()
                    }
                    return BitmapFactory.decodeFile(thumbnail.absolutePath, BitmapFactory.Options())
                }
            }).setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: Notification,
                    ongoing: Boolean,
                ) {
                    if (ongoing) {
                        // Make sure the service will not get destroyed while playing media.
                        startForeground(notificationId, notification)
                    } else {
                        // Make notification cancellable.
                        stopForeground(false)
                    }
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
            }).build().apply {
                setSmallIcon(R.drawable.ic_stat_notify)
                setUseChronometer(false)
                setUseNextActionInCompactView(true)
                setUsePreviousActionInCompactView(true)

                setPlayer(player)
            }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mainHandler.removeCallbacks(resumePlayRunnable)

        when (intent?.getIntExtra(INTENT_EXTRA_KEY_COMMAND, COMMAND_CREATE)) {
            COMMAND_PAUSE -> {
                if (initialized) {
                    if (playing()) {
                        setPlayWhenReady(false)
                        needPlayWhenReceiveResumeCommand = true
                    }
                } else {
                    stopSelf()
                }
            }
            COMMAND_RESUME -> {
                if (initialized) {
                    mainHandler.postDelayed(resumePlayRunnable, RESUME_DELAY_MS)
                } else {
                    stopSelf()
                }
            }
            COMMAND_STOP -> {
                stopAudioPlayer()
            }
            else -> {
                if (MediaPlayerActivity.isAudioPlayer(intent)) {
                    createPlayerControlNotification()
                }

                if (viewModel.buildPlayerSource(intent)) {
                    initialized = true

                    if (viewModel.audioPlayer) {
                        MiniAudioPlayerController.notifyAudioPlayerPlaying(true)
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun observeLiveData() {
        viewModel.playerSource.observe(this, Observer { playSource(it.first, it.second, it.third) })

        viewModel.mediaItemToRemove.observe(this, Observer {
            if (it < player.mediaItemCount) {
                val nextIndex = player.nextMediaItemIndex
                if (nextIndex != C.INDEX_UNSET) {
                    val nextItem = player.getMediaItemAt(nextIndex)
                    val nodeName = viewModel.getPlaylistItem(nextItem.mediaId)?.nodeName ?: ""
                    _metadata.value = Metadata(null, null, null, nodeName)
                }
                player.removeMediaItem(it)
            }
        })

        viewModel.nodeNameUpdate.observe(this, Observer {
            val meta = _metadata.value ?: return@Observer
            _metadata.value = Metadata(meta.title, meta.artist, meta.album, it)
        })

        viewModel.playingThumbnail.observe(this, Observer {
            playerNotificationManager?.invalidate()
        })

        viewModel.retry.observe(this, Observer {
            if (it && player.playbackState == Player.STATE_IDLE) {
                player.prepare()
            }
        })
    }

    private fun playSource(
        mediaItems: List<MediaItem>,
        newIndexForCurrentItem: Int,
        nameToDisplay: String?,
    ) {
        Timber.d("playSource ${mediaItems.size} items")

        if (!audioFocusRequested) {
            audioFocusRequested = true
            getAudioFocus(
                audioManager, audioFocusListener, audioFocusRequest, AUDIOFOCUS_DEFAULT,
                STREAM_MUSIC_DEFAULT
            )
        }

        if (nameToDisplay != null) {
            _metadata.value = Metadata(null, null, null, nameToDisplay)
        }

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

        if (!viewModel.paused) {
            setPlayWhenReady(true)
        }

        player.prepare()
    }

    override fun onDestroy() {
        super.onDestroy()

        viewModel.cancelSearch()
        mainHandler.removeCallbacks(resumePlayRunnable)
        positionUpdateHandler.removeCallbacks(positionUpdateRunnable)

        if (initialized) {
            if (audioManager != null) {
                abandonAudioFocus(audioFocusListener, audioManager, audioFocusRequest)
            }

            viewModel.clear()
        }

        playerNotificationManager?.setPlayer(null)
        player.release()
    }

    fun mainPlayerUIClosed() {
        if (!viewModel.audioPlayer) {
            stopAudioPlayer()
        }
    }

    fun stopAudioPlayer() {
        player.stop()

        if (viewModel.audioPlayer) {
            MiniAudioPlayerController.notifyAudioPlayerPlaying(false)
        }

        stopSelf()
    }

    fun setPlayWhenReady(playWhenReady: Boolean) {
        if (!playWhenReady) {
            player.playWhenReady = false
        } else if (CallUtil.participatingInACall()) {
            LiveEventBus.get(EVENT_NOT_ALLOW_PLAY, Boolean::class.java)
                .post(true)
        } else {
            player.playWhenReady = true
        }
    }

    fun seekTo(index: Int) {
        player.seekTo(index, 0)
        viewModel.resetRetryState()
    }

    fun onMoveToForeground() {
        if (needPlayWhenGoForeground) {
            setPlayWhenReady(true)
            needPlayWhenGoForeground = false
        }
    }

    fun onMoveToBackground() {
        if ((!viewModel.backgroundPlayEnabled() || !viewModel.audioPlayer) && playing()) {
            setPlayWhenReady(false)
            needPlayWhenGoForeground = true
        }
    }

    fun playing() = player.playWhenReady && player.playbackState != Player.STATE_ENDED

    companion object {
        private const val INCREMENT_TIME_IN_MS = 15000L

        private const val PLAYBACK_NOTIFICATION_ID = 1

        private const val INTENT_EXTRA_KEY_COMMAND = "command"
        private const val COMMAND_CREATE = 1
        private const val COMMAND_PAUSE = 2
        private const val COMMAND_RESUME = 3
        private const val COMMAND_STOP = 4

        private const val RESUME_DELAY_MS = 500L

        const val SINGLE_PLAYLIST_SIZE = 2

        /**
         * Pause the video player when play audio.
         *
         * @param context Android context
         */
        @JvmStatic
        fun pauseVideoPlayer(context: Context) {
            val videoPlayerIntent = Intent(context, VideoPlayerService::class.java)
            videoPlayerIntent.putExtra(INTENT_EXTRA_KEY_COMMAND, COMMAND_PAUSE)
            context.startService(videoPlayerIntent)
        }

        /**
         * Pause the audio player when play video, play/record audio clip, start/receive call.
         *
         * @param context Android context
         */
        @JvmStatic
        fun pauseAudioPlayer(context: Context) {
            val audioPlayerIntent = Intent(context, AudioPlayerService::class.java)
            audioPlayerIntent.putExtra(INTENT_EXTRA_KEY_COMMAND, COMMAND_PAUSE)
            context.startService(audioPlayerIntent)
        }

        /**
         * Resume the audio player when go back from pauseAudioPlayer.
         *
         * @param context Android context
         */
        @JvmStatic
        fun resumeAudioPlayer(context: Context) {
            val audioPlayerIntent = Intent(context, AudioPlayerService::class.java)
            audioPlayerIntent.putExtra(INTENT_EXTRA_KEY_COMMAND, COMMAND_RESUME)
            context.startService(audioPlayerIntent)
        }

        /**
         * Resume the audio player when go back from pauseAudioPlayer, and when there is no ongoing
         * call.
         *
         * @param context Android context
         */
        @JvmStatic
        fun resumeAudioPlayerIfNotInCall(context: Context) {
            if (!CallUtil.participatingInACall()) {
                resumeAudioPlayer(context)
            }
        }

        /**
         * Stop the audio player, e.g. when logout.
         *
         * @param context Android context
         */
        @JvmStatic
        fun stopAudioPlayer(context: Context) {
            val audioPlayerIntent = Intent(context, AudioPlayerService::class.java)
            audioPlayerIntent.putExtra(INTENT_EXTRA_KEY_COMMAND, COMMAND_STOP)
            context.startService(audioPlayerIntent)
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> onMoveToForeground()
            Lifecycle.Event.ON_STOP -> onMoveToBackground()
            else -> return
        }
    }
}
