package mega.privacy.android.app.mediaplayer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
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
import mega.privacy.android.app.di.mediaplayer.VideoPlayer
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.gateway.VideoPlayerServiceGateway
import mega.privacy.android.app.mediaplayer.gateway.VideoPlayerServiceViewModelGateway
import mega.privacy.android.app.mediaplayer.model.MediaPlaySources
import mega.privacy.android.app.mediaplayer.model.PlaybackPositionState
import mega.privacy.android.app.mediaplayer.service.AudioPlayerService.Companion.pauseAudioPlayer
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.domain.entity.mediaplayer.PlaybackInformation
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import timber.log.Timber
import javax.inject.Inject

/**
 * The service for playing video
 */
@AndroidEntryPoint
class VideoPlayerService : LifecycleService(), LifecycleEventObserver, VideoPlayerServiceGateway {
    /**
     * MediaPlayerGateway for audio player
     */
    @VideoPlayer
    @Inject
    lateinit var mediaPlayerGateway: MediaPlayerGateway

    /**
     * ServiceViewModelGateway
     */
    @Inject
    lateinit var viewModelGateway: VideoPlayerServiceViewModelGateway

    private val binder by lazy { MediaPlayerServiceBinder(this, viewModelGateway) }

    private val metadata = MutableLiveData<Metadata>()
    private val orientationUpdate = MutableLiveData<Pair<Int, Int>>()

    private var mediaPlayerIntent: Intent? = null
    private var videoPlayType = VIDEO_TYPE_SHOW_PLAYBACK_POSITION_DIALOG
    private var isPlayingAfterReady = false
    private var currentPlayingHandle: Long? = null
    private val showPlaybackPositionDialogUpdate = MutableStateFlow(PlaybackPositionState())

    private var currentMediaPlaySources: MediaPlaySources? = null

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
        createPlayer()
        pauseAudioPlayer(this)
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
                }

                override fun onRepeatModeChangedCallback(repeatToggleMode: RepeatToggleMode) {
                    setVideoRepeatMode(repeatToggleMode)
                }

                override fun onPlayWhenReadyChangedCallback(playWhenReady: Boolean) {
                    setPaused(!playWhenReady)
                }

                override fun onPlaybackStateChangedCallback(state: Int) {
                    when {
                        state == MEDIA_PLAYER_STATE_ENDED && !isPaused() -> {
                            setPaused(true)
                        }

                        state == MEDIA_PLAYER_STATE_READY -> {
                            // This case is only for video player
                            if (isPaused() && mediaPlayerGateway.getPlayWhenReady()) {
                                setPaused(false)
                                viewModelGateway.sendVideoPlayerActivatedEvent()
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
                    }
                }

                override fun onPlayerErrorCallback() {
                    onPlayerError()
                }

                override fun onVideoSizeCallback(videoWidth: Int, videoHeight: Int) {
                    orientationUpdate.value = Pair(videoWidth, videoHeight)
                }
            }

            mediaPlayerGateway.createPlayer(
                repeatToggleMode = videoRepeatToggleMode(),
                nameChangeCallback = nameChangeCallback,
                mediaPlayerCallback = mediaPlayerCallback
            )
        }
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleScope.launch {
            currentPlayingHandle = intent?.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)

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
        return super.onStartCommand(intent, flags, startId)
    }

    private fun initVideoSources(intent: Intent?) {
        lifecycleScope.launch {
            viewModelGateway.buildPlayerSource(intent)
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
    }

    private fun playSource(mediaPlaySources: MediaPlaySources) {
        Timber.d("playSource ${mediaPlaySources.mediaItems.size} items")

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
        viewModelGateway.cancelSearch()

        viewModelGateway.clear()
        mediaPlayerGateway.playerRelease()
        // Remove observer when the service is destroyed to avoid the memory leak, causing Service cannot be stopped.
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        unregisterReceiver(headsetPlugReceiver)
    }

    override fun mainPlayerUIClosed() {
        stopPlayer()
    }

    override fun stopPlayer() {
        mediaPlayerGateway.playerStop()
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
            LiveEventBus.get(Constants.EVENT_NOT_ALLOW_PLAY, Boolean::class.java)
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

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {}

    companion object {
        private const val MEDIA_PLAYER_STATE_ENDED = 4
        private const val MEDIA_PLAYER_STATE_READY = 3

        private const val VIDEO_TYPE_RESUME_PLAYBACK_POSITION = 123
        private const val VIDEO_TYPE_RESTART_PLAYBACK_POSITION = 124
        private const val VIDEO_TYPE_SHOW_PLAYBACK_POSITION_DIALOG = 125

        private const val INTENT_KEY_STATE = "state"
        private const val STATE_HEADSET_UNPLUGGED = 0
    }
}
