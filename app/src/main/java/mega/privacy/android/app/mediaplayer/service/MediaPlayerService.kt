package mega.privacy.android.app.mediaplayer.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.AudioPlayerActivity
import mega.privacy.android.app.mediaplayer.MediaPlayerActivity
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerServiceGateway
import mega.privacy.android.app.mediaplayer.gateway.PlayerServiceViewModelGateway
import mega.privacy.android.app.mediaplayer.miniplayer.MiniAudioPlayerController
import mega.privacy.android.app.mediaplayer.model.MediaPlaySources
import mega.privacy.android.app.mediaplayer.model.PlaybackPositionState
import mega.privacy.android.app.mediaplayer.model.PlayerNotificationCreatedParams
import mega.privacy.android.app.mediaplayer.model.RepeatToggleMode
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil.AUDIOFOCUS_DEFAULT
import mega.privacy.android.app.utils.ChatUtil.STREAM_MUSIC_DEFAULT
import mega.privacy.android.app.utils.ChatUtil.abandonAudioFocus
import mega.privacy.android.app.utils.ChatUtil.getAudioFocus
import mega.privacy.android.app.utils.ChatUtil.getRequest
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.EVENT_NOT_ALLOW_PLAY
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_REBUILD_PLAYLIST
import mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_AUDIO_PLAYER_ID
import mega.privacy.android.domain.entity.mediaplayer.PlaybackInformation
import nz.mega.sdk.MegaApiJava
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

    private var mediaPlayerIntent: Intent? = null
    private var videoPlayType = VIDEO_TYPE_SHOW_PLAYBACK_POSITION_DIALOG
    private var isPlayingAfterReady = false
    private var currentPlayingHandle: Long? = null
    private val showPlaybackPositionDialogUpdate = MutableStateFlow(PlaybackPositionState())

    private var needPlayWhenGoForeground = false
    private var needPlayWhenReceiveResumeCommand = false

    private val mainHandler = Handler(Looper.getMainLooper())

    private var currentMediaPlaySources: MediaPlaySources? = null

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
        viewModelGateway.setAudioPlayer(this is AudioPlayerService)
        audioManager = (getSystemService(AUDIO_SERVICE) as AudioManager)
        audioFocusRequest = getRequest(audioFocusListener, AUDIOFOCUS_DEFAULT)
        createPlayer()
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
                        lifecycleScope.launch {
                            viewModelGateway.monitorPlaybackTimes(it.toLong()) { positionInMs ->
                                when (videoPlayType) {
                                    VIDEO_TYPE_RESUME_PLAYBACK_POSITION ->
                                        positionInMs?.let { position ->
                                            mediaPlayerGateway.playerSeekToPositionInMs(position)
                                        }

                                    VIDEO_TYPE_RESTART_PLAYBACK_POSITION -> {
                                        // Remove current playback history, if video type is restart
                                        lifecycleScope.launch {
                                            viewModelGateway.deletePlaybackInformation(it.toLong())
                                        }
                                    }

                                    VIDEO_TYPE_SHOW_PLAYBACK_POSITION_DIALOG -> {
                                        // Detect the media item whether is transition by comparing
                                        // currentPlayingHandle if is parameter handle
                                        if (currentPlayingHandle != it.toLong()
                                            && positionInMs != null && positionInMs > 0
                                        ) {
                                            // If the dialog is not showing before build sources,
                                            // the video is paused before the dialog is dismissed.
                                            isPlayingAfterReady = false
                                            showPlaybackPositionDialogUpdate.update { info ->
                                                info.copy(
                                                    showPlaybackDialog = true,
                                                    mediaItemName = getPlaylistItem(it)?.nodeName,
                                                    playbackPosition = positionInMs,
                                                    isDialogShownBeforeBuildSources = false
                                                )
                                            }
                                        } else {
                                            // If currentPlayHandle is parameter handle and there is
                                            // no playback history, the video is playing after ready
                                            isPlayingAfterReady = true
                                            // Set playWhenReady to be true to ensure the video is playing after ready
                                            if (!mediaPlayerGateway.getPlayWhenReady()) {
                                                setPlayWhenReady(true)
                                            }
                                        }
                                    }
                                }
                            }
                        }
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
                    setPaused(!playWhenReady)
                }

                override fun onPlaybackStateChangedCallback(state: Int) {
                    when {
                        state == MEDIA_PLAYER_STATE_ENDED && !isPaused() -> {
                            setPaused(true)
                        }

                        !isAudioPlayer() && state == MEDIA_PLAYER_STATE_READY -> {
                            // This case is only for video player
                            if (isPaused() && mediaPlayerGateway.getPlayWhenReady()) {
                                setPaused(false)
                            } else {
                                // Detect videoPlayType and isPlayingAfterReady after video is ready
                                // If videoPlayType is VIDEO_TYPE_SHOW_PLAYBACK_POSITION_DIALOG, and
                                // isPlayingAfterReady is false, the video is paused after it's ready
                                if (videoPlayType == VIDEO_TYPE_SHOW_PLAYBACK_POSITION_DIALOG
                                    && !isPlayingAfterReady
                                ) {
                                    setPlayWhenReady(false)
                                }
                            }
                        }

                        state == MEDIA_PLAYER_STATE_READY && isPaused()
                                && mediaPlayerGateway.getPlayWhenReady() -> {
                            setPaused(false)
                            if (!isAudioPlayer()) {
                                Timber.d("video is playing")
                                viewModelGateway.sendVideoPlayerActivatedEvent()
                            }
                        }
                    }
                }

                override fun onPlayerErrorCallback() {
                    onPlayerError()
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
                    lifecycleScope.launch {
                        if (viewModelGateway.buildPlayerSource(intent)) {
                            if (viewModelGateway.isAudioPlayer()) {
                                MiniAudioPlayerController.notifyAudioPlayerPlaying(true)
                            }
                        }
                    }
                } else {
                    lifecycleScope.launch {
                        currentPlayingHandle =
                            intent?.getLongExtra(
                                Constants.INTENT_EXTRA_KEY_HANDLE,
                                MegaApiJava.INVALID_HANDLE
                            )
                        viewModelGateway.monitorPlaybackTimes(currentPlayingHandle) { positionInMs ->
                            // If the first video contains playback history, show dialog before build sources
                            if (positionInMs != null && positionInMs > 0) {
                                mediaPlayerIntent = intent
                                showPlaybackPositionDialogUpdate.update { info ->
                                    info.copy(
                                        showPlaybackDialog = true,
                                        mediaItemName = intent?.getStringExtra(Constants.INTENT_EXTRA_KEY_FILE_NAME),
                                        playbackPosition = positionInMs
                                    )
                                }
                            } else {
                                initVideoSources(intent)
                            }
                        }
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun initVideoSources(intent: Intent?) {
        lifecycleScope.launch {
            if (viewModelGateway.buildPlayerSource(intent)) {
                if (viewModelGateway.isAudioPlayer()) {
                    MiniAudioPlayerController.notifyAudioPlayerPlaying(true)
                }
            }

            viewModelGateway.trackPlayback {
                PlaybackInformation(
                    mediaPlayerGateway.getCurrentMediaItem()?.mediaId?.toLong(),
                    mediaPlayerGateway.getCurrentItemDuration(),
                    mediaPlayerGateway.getCurrentPlayingPosition()
                )
            }
        }
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
        lifecycleScope.launch {
            viewModelGateway.savePlaybackTimes()
        }
        mediaPlayerGateway.getCurrentPlayingPosition()
        viewModelGateway.cancelSearch()
        mainHandler.removeCallbacks(resumePlayRunnable)

        if (audioManager != null) {
            abandonAudioFocus(audioFocusListener, audioManager, audioFocusRequest)
        }
        viewModelGateway.clear()
        mediaPlayerGateway.clearPlayerForNotification()
        mediaPlayerGateway.playerRelease()
        // Remove observer when the service is destroyed to avoid the memory leak, causing Service cannot be stopped.
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        unregisterReceiver(headsetPlugReceiver)
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
    private fun onMoveToBackground() {
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

    override fun getCurrentPlayingPosition() = mediaPlayerGateway.getCurrentPlayingPosition()

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

    override fun playbackPositionStateUpdate() = showPlaybackPositionDialogUpdate

    override fun setRestartPlayVideo() {
        updateDialogShownStateAndVideoPlayType(VIDEO_TYPE_RESTART_PLAYBACK_POSITION)
        // Set playWhenReady to be true, making the video is playing after the restart button is clicked
        if (!mediaPlayerGateway.getPlayWhenReady()) {
            setPlayWhenReady(true)
        }
        // If the restart button is clicked, remove playback information of current item
        lifecycleScope.launch {
            viewModelGateway.deletePlaybackInformation(viewModelGateway.getCurrentPlayingHandle())
        }
    }

    override fun setRestartPlayVideoBeforeBuildSources() {
        updateDialogShownStateAndVideoPlayType(VIDEO_TYPE_RESTART_PLAYBACK_POSITION)
        // Initial video sources after the restart button is clicked
        initVideoSources(mediaPlayerIntent)
    }

    override fun setResumePlaybackPosition(playbackPosition: Long?) {
        updateDialogShownStateAndVideoPlayType(VIDEO_TYPE_RESUME_PLAYBACK_POSITION)
        // Seek to playback position history after the resume button is clicked
        playbackPosition?.let {
            mediaPlayerGateway.playerSeekToPositionInMs(it)
        }
        // Set playWhenReady to be true, making the video is playing after the resume button is clicked
        if (!mediaPlayerGateway.getPlayWhenReady()) {
            setPlayWhenReady(true)
        }
    }

    override fun setResumePlaybackPositionBeforeBuildSources(playbackPosition: Long?) {
        updateDialogShownStateAndVideoPlayType(VIDEO_TYPE_RESUME_PLAYBACK_POSITION)
        // Initial video sources after the resume button is clicked
        initVideoSources(mediaPlayerIntent)
    }

    override fun cancelPlaybackPositionDialog() {
        updateDialogShownStateAndVideoPlayType(VIDEO_TYPE_SHOW_PLAYBACK_POSITION_DIALOG)
    }

    override fun cancelPlaybackPositionDialogBeforeBuildSources() {
        updateDialogShownStateAndVideoPlayType(VIDEO_TYPE_SHOW_PLAYBACK_POSITION_DIALOG)
        initVideoSources(mediaPlayerIntent)
        // If the dialog is cancelled, set PlayWhenReady to be false to paused video after build sources.
        setPlayWhenReady(false)
    }

    override fun addSubtitle(subtitleFileUrl: String) {
        mediaPlayerGateway.addSubtitle(subtitleFileUrl)
        // Don't recreate play sources if the playlist is unavailable,
        // to avoid the subtitle not working for a single item played.
        if (mediaPlayerIntent?.getBooleanExtra(
                Constants.INTENT_EXTRA_KEY_IS_PLAYLIST,
                true
            ) == true
        ) {
            currentMediaPlaySources?.let {
                playSource(it)
            }
        }
    }

    override fun showSubtitle() {
        mediaPlayerGateway.showSubtitle()
    }

    override fun hideSubtitle() {
        mediaPlayerGateway.hideSubtitle()
    }

    /**
     * Update dialog shon state and video play type
     *
     * @param type video play type
     */
    private fun updateDialogShownStateAndVideoPlayType(type: Int) {
        // Set showDialog to be false, avoid the dialog is shown repeatedly when screen is rotated
        showPlaybackPositionDialogUpdate.update {
            it.copy(showPlaybackDialog = false)
        }
        videoPlayType = type
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

        private const val VIDEO_TYPE_RESUME_PLAYBACK_POSITION = 123
        private const val VIDEO_TYPE_RESTART_PLAYBACK_POSITION = 124
        private const val VIDEO_TYPE_SHOW_PLAYBACK_POSITION_DIALOG = 125

        private const val INTENT_KEY_STATE = "state"
        private const val STATE_HEADSET_UNPLUGGED = 0

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
            ContextCompat.startForegroundService(context, audioPlayerIntent)
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
            ContextCompat.startForegroundService(context, audioPlayerIntent)
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
            ContextCompat.startForegroundService(context, audioPlayerIntent)
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
