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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.AudioPlayerActivity
import mega.privacy.android.app.mediaplayer.MediaPlayerActivity
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerServiceGateway
import mega.privacy.android.app.mediaplayer.gateway.PlayerServiceViewModelGateway
import mega.privacy.android.app.mediaplayer.miniplayer.MiniAudioPlayerController
import mega.privacy.android.app.mediaplayer.model.MediaPlaySources
import mega.privacy.android.app.mediaplayer.model.PlayerNotificationCreatedParams
import mega.privacy.android.app.mediaplayer.model.RepeatToggleMode
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil.AUDIOFOCUS_DEFAULT
import mega.privacy.android.app.utils.ChatUtil.STREAM_MUSIC_DEFAULT
import mega.privacy.android.app.utils.ChatUtil.abandonAudioFocus
import mega.privacy.android.app.utils.ChatUtil.getAudioFocus
import mega.privacy.android.app.utils.ChatUtil.getRequest
import mega.privacy.android.app.utils.Constants.EVENT_NOT_ALLOW_PLAY
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_AUDIO_PLAYER_ID
import timber.log.Timber
import javax.inject.Inject

/**
 * Media player service
 */
@Suppress("DEPRECATION")
@AndroidEntryPoint
abstract class MediaPlayerService : LifecycleService(), LifecycleEventObserver,
    MediaPlayerServiceGateway {

    /**
     * MediaPlayerGateway
     */
    abstract var mediaPlayerGateway: MediaPlayerGateway

    /**
     * ServiceViewModelGateway
     */
    @Inject
    lateinit var viewModelGateway: PlayerServiceViewModelGateway

    private val binder by lazy { MediaPlayerServiceBinder(this, viewModelGateway) }

    private val metadata = MutableLiveData<Metadata>()
    private val orientationUpdate = MutableLiveData<Pair<Int, Int>>()

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
            viewModelGateway.setCurrentPosition(currentPosition)
        }
    }

    override fun onCreate() {
        super.onCreate()

        viewModelGateway.setAudioPlayer(this is AudioPlayerService)
        audioManager = (getSystemService(AUDIO_SERVICE) as AudioManager)
        audioFocusRequest = getRequest(audioFocusListener, AUDIOFOCUS_DEFAULT)
        createPlayer()
        observeData()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    private fun createPlayer() {
        with(viewModelGateway) {
            val nameChangeCallback: (title: String?, artist: String?, album: String?) -> Unit =
                { title, artist, album ->
                    val nodeName =
                        getPlaylistItem(getCurrentMediaItem()?.mediaId)?.nodeName ?: ""

                    if (!(title.isNullOrEmpty() && artist.isNullOrEmpty()
                                && album.isNullOrEmpty() && nodeName.isEmpty())
                    ) {
                        metadata.value = Metadata(title, artist, album, nodeName)
                        mediaPlayerGateway.invalidatePlayerNotification()
                    }
                }

            val mediaPlayerCallback: MediaPlayerCallback = object : MediaPlayerCallback {
                override fun onMediaItemTransitionCallback(
                    handle: String?,
                    isUpdateName: Boolean,
                ) {
                    handle?.run {
                        setCurrentPlayingHandle(this.toLong())
                        if (isUpdateName) {
                            val nodeName = getPlaylistItem(this)?.nodeName ?: ""
                            metadata.value = Metadata(null, null, null, nodeName)
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
                    setShuffleEnabled(shuffleModeEnabled)

                    if (shuffleModeEnabled) {
                        mediaPlayerGateway.setShuffleOrder(newShuffleOrder())
                    }
                }

                override fun onRepeatModeChangedCallback(repeatToggleMode: RepeatToggleMode) {
                    if (isAudioPlayer()) {
                        setAudioRepeatMode(repeatToggleMode)
                    } else {
                        setVideoRepeatMode(repeatToggleMode)
                    }
                }

                override fun onPlayWhenReadyChangedCallback(playWhenReady: Boolean) {
                    setPaused(!playWhenReady, mediaPlayerGateway.getCurrentPosition())
                }

                override fun onPlaybackStateChangedCallback(state: Int) {
                    when {
                        state == MEDIA_PLAYER_STATE_ENDED && !isPaused() -> {
                            setPaused(true, mediaPlayerGateway.getCurrentPosition())
                        }
                        state == MEDIA_PLAYER_STATE_READY && isPaused() && mediaPlayerGateway.getPlayWhenReady() -> {
                            setPaused(false, mediaPlayerGateway.getCurrentPosition())
                        }
                    }
                }

                override fun onPlayerErrorCallback() {
                    onPlayerError()
                    positionUpdateHandler.removeCallbacks(positionUpdateRunnable)
                }

                override fun onVideoSizeCallback(videoWidth: Int, videoHeight: Int) {
                    orientationUpdate.value = Pair(videoWidth, videoHeight)
                }
            }

            if (isAudioPlayer()) {
                mediaPlayerGateway.createPlayer(
                    shuffleEnabled = shuffleEnabled(),
                    shuffleOrder = getShuffleOrder(),
                    repeatToggleMode = audioRepeatToggleMode(),
                    nameChangeCallback = nameChangeCallback,
                    mediaPlayerCallback = mediaPlayerCallback
                )
            } else {
                mediaPlayerGateway.createPlayer(
                    repeatToggleMode = videoRepeatToggleMode(),
                    nameChangeCallback = nameChangeCallback,
                    mediaPlayerCallback = mediaPlayerCallback
                )
            }
        }
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
                thumbnail = viewModelGateway.getPlayingThumbnail(),
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
                lifecycleScope.launch {
                    if (viewModelGateway.buildPlayerSource(intent)) {
                        if (viewModelGateway.isAudioPlayer()) {
                            MiniAudioPlayerController.notifyAudioPlayerPlaying(true)
                        }
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModelGateway.playerSourceUpdate().collect { mediaPlaySources ->
                playSource(mediaPlaySources)
            }
        }

        lifecycleScope.launch {
            viewModelGateway.mediaItemToRemoveUpdate().collect { index ->
                mediaPlayerGateway.mediaItemRemoved(index)?.let { handle ->
                    val nodeName = viewModelGateway.getPlaylistItem(handle)?.nodeName ?: ""
                    metadata.value = Metadata(null, null, null, nodeName)
                }
            }
        }

        lifecycleScope.launch {
            viewModelGateway.nodeNameUpdate().collect { name ->
                metadata.value?.let {
                    metadata.value = it.copy(nodeName = name)
                }
            }
        }

        lifecycleScope.launch {
            viewModelGateway.retryUpdate().collect { isRetry ->
                mediaPlayerGateway.mediaPlayerRetry(isRetry)
            }
        }

        viewModelGateway.getPlayingThumbnail().observe(this@MediaPlayerService) {
            mediaPlayerGateway.invalidatePlayerNotification()
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
            metadata.value = Metadata(title = null, artist = null, album = null, nodeName = this)
        }

        mediaPlayerGateway.buildPlaySources(mediaPlaySources)

        if (!viewModelGateway.isPaused()) {
            setPlayWhenReady(true)
        }

        mediaPlayerGateway.playerPrepare()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModelGateway.cancelSearch()
        mainHandler.removeCallbacks(resumePlayRunnable)
        positionUpdateHandler.removeCallbacks(positionUpdateRunnable)

        if (audioManager != null) {
            abandonAudioFocus(audioFocusListener, audioManager, audioFocusRequest)
        }
        viewModelGateway.clear()
        mediaPlayerGateway.clearPlayerForNotification()
        mediaPlayerGateway.playerRelease()
        // Remove observer when the service is destroyed to avoid the memory leak, causing Service cannot be stopped.
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }

    override fun mainPlayerUIClosed() {
        if (!viewModelGateway.isAudioPlayer()) {
            stopAudioPlayer()
        }
    }

    override fun stopAudioPlayer() {
        mediaPlayerGateway.playerStop()

        if (viewModelGateway.isAudioPlayer()) {
            MiniAudioPlayerController.notifyAudioPlayerPlaying(false)
        }
        stopSelf()
    }

    override fun setRepeatModeForVideo(repeatToggleMode: RepeatToggleMode) {
        mediaPlayerGateway.setRepeatToggleMode(repeatToggleMode)
    }

    /**
     * Set playWhenReady of player
     *
     * @param playWhenReady true is play when ready, otherwise is false.
     */
    override fun setPlayWhenReady(playWhenReady: Boolean) {
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
    override fun seekTo(index: Int) {
        mediaPlayerGateway.playerSeekTo(index)
        viewModelGateway.resetRetryState()
    }

    /**
     * Service is moved to foreground
     */
    private fun onMoveToForeground() {
        if (needPlayWhenGoForeground) {
            setPlayWhenReady(true)
            needPlayWhenGoForeground = false
        }
    }

    /**
     * Service is moved to background
     */
    fun onMoveToBackground() {
        with(viewModelGateway) {
            if ((!backgroundPlayEnabled() || !isAudioPlayer()) && playing()) {
                setPlayWhenReady(false)
                needPlayWhenGoForeground = true
            }
        }
    }

    override fun metadataUpdate() = metadata.asFlow()

    override fun videoSizeUpdate() = orientationUpdate.asFlow()

    override fun removeListener(listener: Player.Listener) {
        mediaPlayerGateway.removeListener(listener)
    }

    override fun addPlayerListener(listener: Player.Listener) {
        mediaPlayerGateway.addPlayerListener(listener)
    }

    override fun getCurrentMediaItem() = mediaPlayerGateway.getCurrentMediaItem()

    override fun getPlaybackState() = mediaPlayerGateway.getPlaybackState()

    override fun setupPlayerView(
        playerView: StyledPlayerView,
        useController: Boolean,
        controllerShowTimeoutMs: Int,
        controllerHideOnTouch: Boolean,
        isAudioPlayer: Boolean,
        showShuffleButton: Boolean?,
    ) {
        mediaPlayerGateway.setupPlayerView(
            playerView = playerView,
            useController = useController,
            controllerShowTimeoutMs = controllerShowTimeoutMs,
            controllerHideOnTouch = controllerHideOnTouch,
            isAudioPlayer = isAudioPlayer,
            showShuffleButton = showShuffleButton,
        )
    }

    override fun playing() = mediaPlayerGateway.mediaPlayerIsPlaying()

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
