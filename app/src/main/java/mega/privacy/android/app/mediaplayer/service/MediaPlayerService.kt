package mega.privacy.android.app.mediaplayer.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ProcessLifecycleOwner
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.R
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.di.MegaApiFolder
import mega.privacy.android.app.mediaplayer.AudioPlayerActivity
import mega.privacy.android.app.mediaplayer.MediaPlayerActivity
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerServiceGateway
import mega.privacy.android.app.mediaplayer.miniplayer.MiniAudioPlayerController
import mega.privacy.android.app.mediaplayer.model.MediaPlaySources
import mega.privacy.android.app.mediaplayer.model.PlayerNotificationCreatedParams
import mega.privacy.android.app.usecase.GetGlobalTransferUseCase
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil.AUDIOFOCUS_DEFAULT
import mega.privacy.android.app.utils.ChatUtil.STREAM_MUSIC_DEFAULT
import mega.privacy.android.app.utils.ChatUtil.abandonAudioFocus
import mega.privacy.android.app.utils.ChatUtil.getAudioFocus
import mega.privacy.android.app.utils.ChatUtil.getRequest
import mega.privacy.android.app.utils.Constants.EVENT_NOT_ALLOW_PLAY
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_AUDIO_PLAYER_ID
import mega.privacy.android.app.utils.wrapper.GetOfflineThumbnailFileWrapper
import nz.mega.sdk.MegaApiAndroid
import timber.log.Timber
import javax.inject.Inject

/**
 * Media player service
 */
@Suppress("DEPRECATION")
@AndroidEntryPoint
abstract class MediaPlayerService : LifecycleService(), LifecycleEventObserver {

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

    @Inject
    lateinit var getGlobalTransferUseCase: GetGlobalTransferUseCase

    /**
     * MediaPlayerGateway
     */
    abstract var mediaPlayerGateway: MediaPlayerGateway

    /**
     * MediaPlayerServiceGateway
     */
    abstract var mediaPlayerServiceGateway: MediaPlayerServiceGateway

    private val binder by lazy { MediaPlayerServiceBinder(this) }

    lateinit var viewModel: MediaPlayerServiceViewModel

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
                    if (mediaPlayerGateway.getPlayWhenReady()) {
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
            val currentPosition = mediaPlayerGateway.getCurrentPosition()
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
            offlineThumbnailFileWrapper,
            getGlobalTransferUseCase
        )
        audioManager = (getSystemService(AUDIO_SERVICE) as AudioManager)
        audioFocusRequest = getRequest(audioFocusListener, AUDIOFOCUS_DEFAULT)

        createPlayer()
        observeLiveData()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    private fun createPlayer() {
        mediaPlayerGateway.createPlayer(
            shuffleEnabled = viewModel.shuffleEnabled(),
            shuffleOrder = viewModel.shuffleOrder,
            repeatMode = viewModel.repeatMode(),
            nameChangeCallback = { title, artist, album ->
                val nodeName =
                    viewModel.getPlaylistItem(mediaPlayerGateway.getCurrentMediaItem()?.mediaId)?.nodeName
                        ?: ""

                if (!(title.isNullOrEmpty() && artist.isNullOrEmpty()
                            && album.isNullOrEmpty() && nodeName.isEmpty())
                ) {
                    _metadata.value = Metadata(title, artist, album, nodeName)

                    mediaPlayerGateway.invalidatePlayerNotification()
                }
            },
            mediaPlayerCallback = object : MediaPlayerCallback {
                override fun onMediaItemTransitionCallback(handle: String?, isUpdateName: Boolean) {
                    handle?.run {
                        viewModel.playingHandle = this.toLong()
                        if (isUpdateName) {
                            val nodeName = viewModel.getPlaylistItem(this)?.nodeName ?: ""
                            _metadata.value = Metadata(null, null, null, nodeName)
                        }
                    }

                }

                override fun onIsPlayingChangedCallback(isPlaying: Boolean) {
                    if (isPlaying) {
                        positionUpdateHandler.post(positionUpdateRunnable)
                    } else {
                        positionUpdateHandler.removeCallbacks(positionUpdateRunnable)
                    }
                }

                override fun onShuffleModeEnabledChangedCallback(shuffleModeEnabled: Boolean) {
                    viewModel.setShuffleEnabled(shuffleModeEnabled)

                    if (shuffleModeEnabled) {
                        mediaPlayerGateway.setShuffleOrder(viewModel.newShuffleOrder())
                    }
                }

                override fun onRepeatModeChangedCallback(repeatMode: Int) {
                    viewModel.setRepeatMode(repeatMode)
                }

                override fun onPlayWhenReadyChangedCallback(playWhenReady: Boolean) {
                    viewModel.setPaused(!playWhenReady, mediaPlayerGateway.getCurrentPosition())
                }

                override fun onPlaybackStateChangedCallback(state: Int) {
                    when {
                        state == MEDIA_PLAYER_STATE_ENDED && !viewModel.paused -> {
                            viewModel.setPaused(true, mediaPlayerGateway.getCurrentPosition())
                        }
                        state == MEDIA_PLAYER_STATE_READY && viewModel.paused && mediaPlayerGateway.getPlayWhenReady() -> {
                            viewModel.setPaused(false, mediaPlayerGateway.getCurrentPosition())
                        }
                    }
                }

                override fun onPlayerErrorCallback() {
                    viewModel.onPlayerError()
                    positionUpdateHandler.removeCallbacks(positionUpdateRunnable)
                }
            },
//            listener = object : Player.Listener {
//                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
//                    val handle = mediaItem?.mediaId ?: return
//                    viewModel.playingHandle = handle.toLong()
//
//                    if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
//                        val nodeName = viewModel.getPlaylistItem(handle)?.nodeName ?: ""
//                        _metadata.value = Metadata(null, null, null, nodeName)
//                    }
//                }
//
//                override fun onIsPlayingChanged(isPlaying: Boolean) {
//                    super.onIsPlayingChanged(isPlaying)
//                    if (isPlaying) {
//                        positionUpdateHandler.post(positionUpdateRunnable)
//                    } else {
//                        positionUpdateHandler.removeCallbacks(positionUpdateRunnable)
//                    }
//                }
//
//                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
//                    viewModel.setShuffleEnabled(shuffleModeEnabled)
//
//                    if (shuffleModeEnabled) {
//                        mediaPlayerGateway.setShuffleOrder(viewModel.newShuffleOrder())
//                    }
//                }
//
//                override fun onRepeatModeChanged(repeatMode: Int) {
//                    viewModel.setRepeatMode(repeatMode)
//                }
//
//                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
//                    viewModel.setPaused(!playWhenReady, mediaPlayerGateway.getCurrentPosition())
//
//                    if (playWhenReady && notificationDismissed) {
//                        mediaPlayerGateway.setPlayerForNotification()
//                        notificationDismissed = false
//                    }
//                }
//
//                override fun onPlaybackStateChanged(state: Int) {
//                    when {
//                        state == Player.STATE_ENDED && !viewModel.paused -> {
//                            viewModel.setPaused(true, mediaPlayerGateway.getCurrentPosition())
//                        }
//                        state == Player.STATE_READY && viewModel.paused && mediaPlayerGateway.getPlayWhenReady() -> {
//                            viewModel.setPaused(false, mediaPlayerGateway.getCurrentPosition())
//                        }
//                    }
//                }
//
//                override fun onPlayerError(error: PlaybackException) {
//                    viewModel.onPlayerError()
//                    positionUpdateHandler.removeCallbacks(positionUpdateRunnable)
//                }
//            }
        )
    }

    private fun createPlayerControlNotification() {
        mediaPlayerGateway.createPlayerControlNotification(
            PlayerNotificationCreatedParams(
                notificationId = PLAYBACK_NOTIFICATION_ID,
                channelId = NOTIFICATION_CHANNEL_AUDIO_PLAYER_ID,
                channelNameResourceId = R.string.audio_player_notification_channel_name,
                metadata = metadata,
                pendingIntent = PendingIntent.getActivity(
                    applicationContext,
                    0,
                    Intent(applicationContext, AudioPlayerActivity::class.java).apply {
                        putExtra(INTENT_EXTRA_KEY_REBUILD_PLAYLIST, false)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                ),
                thumbnail = viewModel.playingThumbnail,
                smallIcon = R.drawable.ic_stat_notify,
                onNotificationPostedCallback = { notificationId, notification, ongoing ->
                    if (ongoing) {
                        // Make sure the service will not get destroyed while playing media.
                        startForeground(notificationId, notification)
                    } else {
                        // Make notification cancellable.
                        stopForeground(false)
                    }
                }
            )
        )
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mainHandler.removeCallbacks(resumePlayRunnable)

        when (intent?.getIntExtra(INTENT_EXTRA_KEY_COMMAND, COMMAND_CREATE)) {
            COMMAND_PAUSE -> {
                if (playing()) {
                    setPlayWhenReady(false)
                    needPlayWhenReceiveResumeCommand = true
                }
            }
            COMMAND_RESUME -> {
                mainHandler.postDelayed(resumePlayRunnable, RESUME_DELAY_MS)
            }
            COMMAND_STOP -> {
                stopAudioPlayer()
            }
            else -> {
                if (MediaPlayerActivity.isAudioPlayer(intent)) {
                    createPlayerControlNotification()
                }

                if (viewModel.buildPlayerSource(intent)) {
                    if (viewModel.audioPlayer) {
                        MiniAudioPlayerController.notifyAudioPlayerPlaying(true)
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun observeLiveData() {
        viewModel.playerSource.observe(this) { mediaPlaySources ->
            playSource(mediaPlaySources)
        }

        viewModel.mediaItemToRemove.observe(this) { index ->
            mediaPlayerGateway.mediaItemRemoved(index)?.let { handle ->
                val nodeName = viewModel.getPlaylistItem(handle)?.nodeName ?: ""
                _metadata.value = Metadata(null, null, null, nodeName)
            }
        }

        viewModel.nodeNameUpdate.observe(this) {
            _metadata.value?.let { (title, artist, album) ->
                _metadata.value = Metadata(title, artist, album, it)
            }
        }

        viewModel.playingThumbnail.observe(this) {
            mediaPlayerGateway.invalidatePlayerNotification()
        }

        viewModel.retry.observe(this) { isRetry ->
            mediaPlayerGateway.mediaPlayerRetry(isRetry)
        }
    }

    private fun playSource(mediaPlaySources: MediaPlaySources) {
        Timber.d("playSource ${mediaPlaySources.mediaItems.size} items")

        if (!audioFocusRequested) {
            audioFocusRequested = true
            getAudioFocus(
                audioManager, audioFocusListener, audioFocusRequest, AUDIOFOCUS_DEFAULT,
                STREAM_MUSIC_DEFAULT
            )
        }
        mediaPlaySources.nameToDisplay?.run {
            _metadata.value = Metadata(title = null, artist = null, album = null, nodeName = this)
        }

        mediaPlayerGateway.buildPlaySources(mediaPlaySources)

        if (!viewModel.paused) {
            setPlayWhenReady(true)
        }

        mediaPlayerGateway.playerPrepare()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.cancelSearch()
        mainHandler.removeCallbacks(resumePlayRunnable)
        positionUpdateHandler.removeCallbacks(positionUpdateRunnable)

        if (audioManager != null) {
            abandonAudioFocus(audioFocusListener, audioManager, audioFocusRequest)
        }
        viewModel.clear()
        mediaPlayerGateway.clearPlayerForNotification()
        mediaPlayerGateway.playerRelease()
    }

    /**
     * Close video player UI
     */
    fun mainPlayerUIClosed() {
        if (!viewModel.audioPlayer) {
            stopAudioPlayer()
        }
    }

    /**
     * Stop audio player
     */
    fun stopAudioPlayer() {
        mediaPlayerGateway.playerStop()

        if (viewModel.audioPlayer) {
            MiniAudioPlayerController.notifyAudioPlayerPlaying(false)
        }

        stopSelf()
    }

    /**
     * Set playWhenReady of player
     *
     * @param playWhenReady true is play when ready, otherwise is false.
     */
    fun setPlayWhenReady(playWhenReady: Boolean) {
        if (!playWhenReady) {
            mediaPlayerGateway.setPlayWhenReady(false)
        } else if (CallUtil.participatingInACall()) {
            LiveEventBus.get(EVENT_NOT_ALLOW_PLAY, Boolean::class.java)
                .post(true)
        } else {
            mediaPlayerGateway.setPlayWhenReady(true)
        }
    }

    /**
     * Seek to the index
     *
     * @param index the index that is sought to
     */
    fun seekTo(index: Int) {
        mediaPlayerGateway.playerSeekTo(index)
        viewModel.resetRetryState()
    }

    /**
     * Service is moved to foreground
     */
    fun onMoveToForeground() {
        if (needPlayWhenGoForeground) {
            setPlayWhenReady(true)
            needPlayWhenGoForeground = false
        }
    }

    /**
     * Service is moved to background
     */
    fun onMoveToBackground() {
        if ((!viewModel.backgroundPlayEnabled() || !viewModel.audioPlayer) && playing()) {
            setPlayWhenReady(false)
            needPlayWhenGoForeground = true
        }
    }

    /**
     * Judge the player whether is playing
     *
     * @return true is playing, otherwise is false.
     */
    fun playing() = mediaPlayerGateway.mediaPlayerIsPlaying()

    companion object {
        private const val PLAYBACK_NOTIFICATION_ID = 1

        private const val INTENT_EXTRA_KEY_COMMAND = "command"
        private const val COMMAND_CREATE = 1
        private const val COMMAND_PAUSE = 2
        private const val COMMAND_RESUME = 3
        private const val COMMAND_STOP = 4

        private const val MEDIA_PLAYER_STATE_ENDED = 4
        private const val MEDIA_PLAYER_STATE_READY = 3

        private const val RESUME_DELAY_MS = 500L

        /**
         * The minimum size of single playlist
         */
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
