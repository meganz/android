package mega.privacy.android.app.mediaplayer.service

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.di.mediaplayer.AudioPlayer
import mega.privacy.android.app.mediaplayer.AudioPlayerActivity
import mega.privacy.android.app.mediaplayer.MediaSessionHelper
import mega.privacy.android.app.mediaplayer.gateway.AudioPlayerServiceViewModelGateway
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerServiceGateway
import mega.privacy.android.app.mediaplayer.miniplayer.MiniAudioPlayerController
import mega.privacy.android.app.mediaplayer.model.AudioSpeedPlaybackItem
import mega.privacy.android.app.mediaplayer.model.MediaPlaySources
import mega.privacy.android.app.mediaplayer.model.PlayerNotificationCreatedParams
import mega.privacy.android.app.presentation.videoplayer.model.PlaybackPositionStatus
import mega.privacy.android.app.utils.CallUtil.participatingInACall
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.ChatUtil.AUDIOFOCUS_DEFAULT
import mega.privacy.android.app.utils.ChatUtil.STREAM_MUSIC_DEFAULT
import mega.privacy.android.app.utils.ChatUtil.getAudioFocus
import mega.privacy.android.app.utils.ChatUtil.getRequest
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_AUDIO_PLAYER_ID
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.entity.mediaplayer.MediaType
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.domain.usecase.login.IsUserLoggedInUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.TrackAudioPlaybackInfoUseCase
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.mobile.analytics.event.AudioPlayerIsActivatedEvent
import mega.privacy.mobile.analytics.event.AudioPlayerLoopPlayingItemEnabledEvent
import mega.privacy.mobile.analytics.event.AudioPlayerLoopQueueEnabledEvent
import mega.privacy.mobile.analytics.event.AudioPlayerShuffleEnabledEvent
import timber.log.Timber
import javax.inject.Inject

/**
 * The service for playing audio
 */
@AndroidEntryPoint
class AudioPlayerService : LifecycleService(), LifecycleEventObserver, MediaPlayerServiceGateway {
    /**
     * MediaPlayerGateway for audio player
     */
    @AudioPlayer
    @Inject
    lateinit var mediaPlayerGateway: MediaPlayerGateway

    /**
     * ServiceViewModelGateway
     */
    @Inject
    lateinit var viewModelGateway: AudioPlayerServiceViewModelGateway

    /**
     * IsUserLoggedIn
     */
    @Inject
    lateinit var isUserLoggedInUseCase: IsUserLoggedInUseCase

    /**
     * CrashReporter
     */
    @Inject
    lateinit var crashReporter: CrashReporter

    @Inject
    lateinit var trackAudioPlaybackInfoUseCase: TrackAudioPlaybackInfoUseCase

    private val binder by lazy { MediaPlayerServiceBinder(this, viewModelGateway) }

    private val metadata = MutableLiveData<Metadata>()

    private var currentPlayingHandle: Long? = null

    private var needPlayWhenGoForeground = false
    private var needPlayWhenReceiveResumeCommand = false

    private val mainHandler = Handler(Looper.getMainLooper())

    private var currentMediaPlaySources: MediaPlaySources? = null

    private var isNotificationCreated = false

    private var isForeground = true

    private var audioClosable = true

    private var currentNotification: Notification? = null

    private var adapterType = INVALID_VALUE

    private var playbackPositionStatus = PlaybackPositionStatus.Initial

    // We need keep it as Runnable here, because we need remove it from handler later,
    // using lambda doesn't work when remove it from handler.
    private val resumePlayRunnable = Runnable {
        if (needPlayWhenReceiveResumeCommand) {
            if (!mediaPlayerGateway.getPlayWhenReady()) {
                setPlayWhenReady(true)
            }
            needPlayWhenReceiveResumeCommand = false
        }
        audioClosable = true
    }

    private lateinit var mediaSessionHelper: MediaSessionHelper
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioFocusRequested = false
    private val audioFocusListener =
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    if (mediaPlayerGateway.getPlayWhenReady()) {
                        setPlayWhenReady(false)
                        needPlayWhenGoForeground = false
                        needPlayWhenReceiveResumeCommand = false
                    }
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        audioFocusRequested = false
                    }
                }

                AudioManager.AUDIOFOCUS_GAIN -> {
                    if (!mediaPlayerGateway.getPlayWhenReady()
                        && isForeground
                        && needPlayWhenReceiveResumeCommand
                    ) {
                        setPlayWhenReady(true)
                    }
                }
            }
        }

    private val headsetPlugReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_HEADSET_PLUG) {
                if (intent.getIntExtra(INTENT_KEY_STATE, -1) == STATE_HEADSET_UNPLUGGED) {
                    setPlayWhenReady(false)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Analytics.tracker.trackEvent(AudioPlayerIsActivatedEvent)
        if (!isNotificationCreated) {
            createPlayerControlNotification()
            isNotificationCreated = true
        }
        createPlayer()
        audioManager = (getSystemService(AUDIO_SERVICE) as AudioManager)
        audioFocusRequest = getRequest(audioFocusListener, AUDIOFOCUS_DEFAULT)
        initMediaSession()
        observeData()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        registerReceiver(headsetPlugReceiver, IntentFilter(Intent.ACTION_HEADSET_PLUG))
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
                    handle?.let {
                        setCurrentPlayingHandle(it.toLong())
                        if (isUpdateName) {
                            val nodeName = getPlaylistItem(it)?.nodeName ?: ""
                            metadata.value = Metadata(null, null, null, nodeName)
                        }
                        currentPlayingHandle = handle.toLong()
                    }
                }

                override fun onShuffleModeEnabledChangedCallback(shuffleModeEnabled: Boolean) {
                    setShuffleEnabled(shuffleModeEnabled)

                    if (shuffleModeEnabled) {
                        Analytics.tracker.trackEvent(AudioPlayerShuffleEnabledEvent)
                        mediaPlayerGateway.setShuffleOrder(newShuffleOrder())
                    }
                }

                override fun onRepeatModeChangedCallback(repeatToggleMode: RepeatToggleMode) {
                    when (repeatToggleMode) {
                        RepeatToggleMode.REPEAT_ONE ->
                            Analytics.tracker.trackEvent(AudioPlayerLoopPlayingItemEnabledEvent)

                        RepeatToggleMode.REPEAT_ALL ->
                            Analytics.tracker.trackEvent(AudioPlayerLoopQueueEnabledEvent)

                        else -> {}
                    }
                    setAudioRepeatMode(repeatToggleMode)
                }

                override fun onPlayWhenReadyChangedCallback(playWhenReady: Boolean) {
                    setPaused(!playWhenReady)
                }

                override fun onPlaybackStateChangedCallback(state: Int) {
                    when {
                        state == MEDIA_PLAYER_STATE_ENDED && !isPaused() -> setPaused(true)

                        state == MEDIA_PLAYER_STATE_READY -> {
                            if (!mediaPlayerGateway.getPlayWhenReady() &&
                                playbackPositionStatus != PlaybackPositionStatus.DialogShowing
                            ) {
                                setPlayWhenReady(true)
                            }
                            if (!isNotificationCreated) {
                                createPlayerControlNotification()
                            }
                            if (isPaused()) {
                                setPaused(false)
                            }
                        }
                    }
                }

                override fun onPlayerErrorCallback() {
                    onPlayerError()
                }

                override fun onVideoSizeCallback(videoWidth: Int, videoHeight: Int) {
                }
            }

            mediaPlayerGateway.createPlayer(
                shuffleEnabled = shuffleEnabled(),
                shuffleOrder = getShuffleOrder(),
                repeatToggleMode = audioRepeatToggleMode(),
                nameChangeCallback = nameChangeCallback,
                mediaPlayerCallback = mediaPlayerCallback
            )
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
                        putExtra(Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST, false)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                ),
                thumbnail = viewModelGateway.getPlayingThumbnail(),
                smallIcon = iconPackR.drawable.ic_stat_notify,
                onNotificationPostedCallback = { notificationId, notification, ongoing ->
                    if (ongoing && isForeground) {
                        // Make sure the service will not get destroyed while playing media.
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                startForeground(
                                    notificationId, notification,
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                                    } else {
                                        0
                                    },
                                )
                            } else {
                                startForeground(notificationId, notification)
                            }
                        } catch (e: Exception) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                                && e is ForegroundServiceStartNotAllowedException
                            ) {
                                Timber.e("App not in a valid state to start foreground service: ${e.message}")
                            }
                        }
                        currentNotification = notification
                    } else {
                        // Make notification cancellable.
                        stopForeground(STOP_FOREGROUND_DETACH)
                    }
                },
                onNotificationCancelledCallback = {
                    isNotificationCreated = false
                    mediaPlayerGateway.playerStop()
                }
            )
        )
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mainHandler.removeCallbacks(resumePlayRunnable)
        val command = intent?.getIntExtra(INTENT_EXTRA_KEY_COMMAND, COMMAND_CREATE)
        if (command == COMMAND_PAUSE || command == COMMAND_RESUME || command == COMMAND_STOP) {
            currentNotification?.let {
                crashReporter.log("currentNotification is not null")
                startForeground(PLAYBACK_NOTIFICATION_ID, it)
            } ?: run {
                crashReporter.log("currentNotification is null and create new notification")
                startForeground(PLAYBACK_NOTIFICATION_ID, createNotificationForStartForeground())
            }
        }
        when (command) {
            COMMAND_PAUSE -> {
                if (playing()) {
                    setPlayWhenReady(false)
                    needPlayWhenReceiveResumeCommand = true
                }
            }

            COMMAND_RESUME -> {
                audioClosable = false
                requestAudioFocus()
                mainHandler.postDelayed(resumePlayRunnable, RESUME_DELAY_MS)
            }

            COMMAND_STOP -> {
                stopPlayer()
            }

            else -> {
                if (!isNotificationCreated) {
                    createPlayerControlNotification()
                    isNotificationCreated = true
                }
                lifecycleScope.launch {
                    if (viewModelGateway.buildPlayerSource(intent)) {
                        MiniAudioPlayerController.notifyAudioPlayerPlaying(true)
                        intent?.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)?.let {
                            adapterType = it
                        }

                        trackAudioPlaybackInfoUseCase {
                            val handle =
                                mediaPlayerGateway.getCurrentMediaItem()?.mediaId?.toLongOrNull()
                                    ?: -1
                            MediaPlaybackInfo(
                                mediaHandle = handle,
                                totalDuration = mediaPlayerGateway.getCurrentItemDuration(),
                                currentPosition = mediaPlayerGateway.getCurrentPlayingPosition(),
                                mediaType = MediaType.Audio
                            )
                        }
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @SuppressLint("ServiceCast")
    private fun createNotificationForStartForeground(): Notification {
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_AUDIO_PLAYER_ID,
                getString(R.string.audio_player_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_AUDIO_PLAYER_ID)
            .setSmallIcon(R.drawable.ic_mega_logo)
            .build()
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModelGateway.playerSourceUpdate().collect { mediaPlaySources ->
                if (mediaPlaySources.mediaItems.isNotEmpty()) {
                    currentMediaPlaySources = mediaPlaySources
                    playSource(mediaPlaySources)
                }
            }
        }

        lifecycleScope.launch {
            viewModelGateway.mediaItemToRemoveUpdate().collect { (index, _) ->
                if (index != -1) {
                    mediaPlayerGateway.mediaItemRemoved(index)?.let { handle ->
                        val nodeName = viewModelGateway.getPlaylistItem(handle)?.nodeName ?: ""
                        metadata.value = Metadata(null, null, null, nodeName)
                    }
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

        viewModelGateway.getPlayingThumbnail().observe(this) {
            mediaPlayerGateway.invalidatePlayerNotification()
        }
    }

    private fun initMediaSession() {
        mediaSessionHelper = MediaSessionHelper(
            applicationContext,
            onPlayPauseClicked = { setPlayWhenReady(!mediaPlayerGateway.getPlayWhenReady()) },
            onNextClicked = { mediaPlayerGateway.playNext() },
            onPreviousClicked = { mediaPlayerGateway.playPrev() }
        )
        audioFocusRequest?.let {
            if (audioManager?.requestAudioFocus(it) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mediaSessionHelper.setupMediaSession()
            }
        }
    }

    override fun monitorMediaNotAllowPlayState() =
        mediaPlayerGateway.monitorMediaNotAllowPlayState()

    private fun playSource(mediaPlaySources: MediaPlaySources) {
        Timber.d("playSource ${mediaPlaySources.mediaItems.size} items")

        requestAudioFocus()
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

        if (audioManager != null) {
            ChatUtil.abandonAudioFocus(audioFocusListener, audioManager, audioFocusRequest)
        }
        viewModelGateway.clear()
        mediaPlayerGateway.clearPlayerForNotification()
        mediaPlayerGateway.playerRelease()
        mediaPlayerGateway.updatePlaybackSpeed(AudioSpeedPlaybackItem.PlaybackSpeed_1X)
        // Remove observer when the service is destroyed to avoid the memory leak, causing Service cannot be stopped.
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        unregisterReceiver(headsetPlugReceiver)
        mediaSessionHelper.releaseMediaSession()
    }

    override fun stopPlayer() {
        if (audioClosable) {
            mediaPlayerGateway.playerStop()

            MiniAudioPlayerController.notifyAudioPlayerPlaying(false)
            stopSelf()
        }
    }

    /**
     * Set playWhenReady of player
     *
     * @param playWhenReady true is play when ready, otherwise is false.
     */
    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (!playWhenReady) {
            mediaPlayerGateway.setPlayWhenReady(false)
        } else if (participatingInACall()) {
            mediaPlayerGateway.updateMediaNotAllowPlayState(true)
        } else {
            mediaPlayerGateway.setPlayWhenReady(true)
        }
    }

    override fun seekTo(index: Int, handle: Long) {
        mediaPlayerGateway.playerSeekTo(index)
        with(viewModelGateway) {
            resetRetryState()
            setCurrentPlayingHandle(handle)
        }
    }

    /**
     * Service is moved to foreground
     */
    private fun onMoveToForeground() {
        isForeground = true
        if (needPlayWhenGoForeground) {
            setPlayWhenReady(true)
            needPlayWhenGoForeground = false
        }
    }

    /**
     * Service is moved to background
     */
    private fun onMoveToBackground() {
        isForeground = false
        with(viewModelGateway) {
            if (!backgroundPlayEnabled() && playing()) {
                setPlayWhenReady(false)
                needPlayWhenGoForeground = true
            }
        }
    }

    override fun metadataUpdate() = metadata.asFlow()

    override fun removeListener(listener: Player.Listener) {
        mediaPlayerGateway.removeListener(listener)
    }

    override fun addPlayerListener(listener: Player.Listener) {
        mediaPlayerGateway.addPlayerListener(listener)
    }

    override fun getCurrentMediaItem() = mediaPlayerGateway.getCurrentMediaItem()

    override fun getCurrentPlayingPosition() = mediaPlayerGateway.getCurrentPlayingPosition()

    override fun getPlaybackState() = mediaPlayerGateway.getPlaybackState()

    override fun setupPlayerView(
        playerView: PlayerView,
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

    override fun stopAudioServiceWhenAudioPlayerClosedWithUserNotLogin() {
        lifecycleScope.launch {
            if (!isUserLoggedInUseCase()) {
                stopPlayer()
            }
        }
    }

    override fun getCurrentAdapterType(): Int = adapterType

    override fun updatePlaybackPositionStatus(value: PlaybackPositionStatus) {
        playbackPositionStatus = value
    }

    override fun getPlaybackPositionStatus(): PlaybackPositionStatus = playbackPositionStatus

    override fun playing() = mediaPlayerGateway.mediaPlayerIsPlaying()

    private fun requestAudioFocus() {
        if (!audioFocusRequested) {
            audioFocusRequested = true
            getAudioFocus(
                audioManager, audioFocusListener, audioFocusRequest, AUDIOFOCUS_DEFAULT,
                STREAM_MUSIC_DEFAULT
            )
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> onMoveToForeground()
            Lifecycle.Event.ON_STOP -> onMoveToBackground()
            else -> return
        }
        Timber.d("AudioPlayerService isForeground: $isForeground")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        //Stop audio player when app is killed.
        stopPlayer()
    }

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

        private const val INTENT_KEY_STATE = "state"
        private const val STATE_HEADSET_UNPLUGGED = 0

        /**
         * Pause the audio player when play video, play/record audio clip, start/receive call.
         *
         * @param context Android context
         */
        @JvmStatic
        fun pauseAudioPlayer(context: Context) {
            sendCommandToAudioPlayer(context = context, command = COMMAND_PAUSE)
        }

        /**
         * Resume the audio player when go back from pauseAudioPlayer.
         *
         * @param context Android context
         */
        @JvmStatic
        fun resumeAudioPlayer(context: Context) {
            sendCommandToAudioPlayer(context = context, command = COMMAND_RESUME)
        }

        /**
         * Resume the audio player when go back from pauseAudioPlayer, and when there is no ongoing
         * call.
         *
         * @param context Android context
         */
        @JvmStatic
        fun resumeAudioPlayerIfNotInCall(context: Context) {
            if (!participatingInACall()) {
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
            sendCommandToAudioPlayer(context = context, command = COMMAND_STOP)
        }

        /**
         * Use for companion object injection
         */
        @EntryPoint
        @InstallIn(SingletonComponent::class)
        interface CrashReporterEntryPoint {
            /**
             * Get [CrashReporter]
             *
             * @return [CrashReporter] instance
             */
            fun crashReporter(): CrashReporter
        }

        /**
         * Send the command to audio player service
         * Check the AudioPlayerService whether is started before send command to avoid ForegroundServiceDidNotStartInTimeException
         *
         * @param context
         * @param command
         */
        @Suppress("DEPRECATION")
        private fun sendCommandToAudioPlayer(context: Context, command: Int) {
            val am = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
            am.getRunningServices(Int.MAX_VALUE).firstOrNull { runningServiceInfo ->
                runningServiceInfo.service.className == AudioPlayerService::class.java.name
            }?.let { runningServiceInfo ->
                if (runningServiceInfo.started) {
                    Timber.d("sendCommandToAudioPlayer invoked")
                    // Add crash log
                    EntryPointAccessors.fromApplication(
                        context,
                        CrashReporterEntryPoint::class.java
                    ).run {
                        if (context is Activity) {
                            crashReporter()
                                .log("Activity name: ${context.javaClass.name}")
                        }
                        crashReporter().log(
                            "command: ${
                                when (command) {
                                    COMMAND_PAUSE -> "COMMAND_PAUSE"
                                    COMMAND_RESUME -> "COMMAND_RESUME"
                                    COMMAND_STOP -> "COMMAND_STOP"
                                    else -> command
                                }
                            }"
                        )
                    }

                    val audioPlayerIntent = Intent(context, AudioPlayerService::class.java)
                    audioPlayerIntent.putExtra(INTENT_EXTRA_KEY_COMMAND, command)
                    ContextCompat.startForegroundService(context, audioPlayerIntent)
                }
            }
        }
    }
}
