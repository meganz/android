package mega.privacy.android.app.presentation.videoplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
import android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
import android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.android.core.ui.model.SnackbarAttributes
import mega.android.core.ui.model.SnackbarDuration
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.appstate.content.navigation.LegacyActivityScaffold
import mega.privacy.android.app.appstate.content.navigation.NavigationResultManager
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.di.mediaplayer.VideoPlayer
import mega.privacy.android.app.mediaplayer.MediaSessionHelper
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.service.LegacyAudioPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerCallback
import mega.privacy.android.app.mediaplayer.service.Metadata
import mega.privacy.android.app.presentation.container.MegaAppContainer
import mega.privacy.android.app.presentation.snackbar.MegaSnackbarShower
import mega.privacy.android.app.presentation.videoplayer.model.MediaPlaybackState
import mega.privacy.android.app.presentation.videoplayer.model.VideoSize
import mega.privacy.android.app.presentation.videoplayer.navigation.VideoPlayerScreenNavKey
import mega.privacy.android.app.presentation.videoplayer.navigation.videoPlayerEntryProvider
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.ChatUtil.AUDIOFOCUS_DEFAULT
import mega.privacy.android.app.utils.ChatUtil.getRequest
import mega.privacy.android.app.utils.Constants.EXTRA_SERIALIZE_STRING
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CHAT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MSG_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_VIDEO_COLLECTION_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_VIDEO_COLLECTION_TITLE
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.URL_FILE_LINK
import mega.privacy.android.app.utils.Constants.URL_LOCAL_FILE_PATH
import mega.privacy.android.core.sharedcomponents.snackbar.MegaSnackbarDuration
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.mobile.analytics.event.VideoPlayerScreenEvent
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import javax.inject.Inject

/**
 * The activity for the revamped video player
 */
@AndroidEntryPoint
class VideoPlayerActivity : PasscodeActivity(), MegaSnackbarShower {
    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    @Inject
    lateinit var navigationResultManager: NavigationResultManager

    @Inject
    lateinit var featureDestinations: Set<@JvmSuppressWildcards FeatureDestination>

    @Inject
    lateinit var snackbarEventQueue: SnackbarEventQueue

    /**
     * MediaPlayerGateway for video player
     */
    @VideoPlayer
    @Inject
    lateinit var mediaPlayerGateway: MediaPlayerGateway

    private val videoPlayerViewModelV2: VideoPlayerViewModelV2 by viewModels(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<VideoPlayerViewModelV2.Factory> { factory ->
                factory.create(
                    VideoPlayerViewModelV2.Args(
                        fileLinkUrl = intent.getStringExtra(URL_FILE_LINK),
                        localFilePath = intent.getStringExtra(URL_LOCAL_FILE_PATH),
                        adapterType = intent.getIntExtra(
                            INTENT_EXTRA_KEY_ADAPTER_TYPE,
                            INVALID_VALUE
                        ),
                        handle = intent.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE),
                        fileName = intent.getStringExtra(INTENT_EXTRA_KEY_FILE_NAME) ?: "",
                        collectionTitle = intent.getStringExtra(
                            INTENT_EXTRA_KEY_VIDEO_COLLECTION_TITLE
                        ),
                        collectionId =
                            if (intent.hasExtra(INTENT_EXTRA_KEY_VIDEO_COLLECTION_ID))
                                intent.getLongExtra(INTENT_EXTRA_KEY_VIDEO_COLLECTION_ID, -1L)
                            else null,
                        chatId =
                            if (intent.hasExtra(INTENT_EXTRA_KEY_CHAT_ID))
                                intent.getLongExtra(INTENT_EXTRA_KEY_CHAT_ID, -1L)
                            else null,
                        msgId =
                            if (intent.hasExtra(INTENT_EXTRA_KEY_MSG_ID))
                                intent.getLongExtra(INTENT_EXTRA_KEY_MSG_ID, -1L)
                            else null,
                        serializedData = intent.getStringExtra(EXTRA_SERIALIZE_STRING)
                    )
                )
            }
        }
    )

    private val headsetPlugReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_HEADSET_PLUG) {
                if (intent.getIntExtra(INTENT_KEY_STATE, -1) == STATE_HEADSET_UNPLUGGED) {
                    mediaPlayerGateway.setPlayWhenReady(false)
                }
            }
        }
    }

    private val pipManager by lazy(LazyThreadSafetyMode.NONE) {
        VideoPlayerPipManager(
            isPipEnabled = { videoPlayerViewModelV2.uiState.value.isPipEnabled },
            getVideoSize = { videoPlayerViewModelV2.uiState.value.currentPlayingVideoSize },
            onEnterPipMode = ::enterPictureInPictureMode,
            isTaskRoot = { isTaskRoot },
            onLaunchMainApp = {
                packageManager.getLaunchIntentForPackage(packageName)?.let { startActivity(it) }
            },
            onFinish = ::finish,
            mediaPlayerGateway = mediaPlayerGateway,
            packageManager = packageManager,
        )
    }
    private var exoPlayer: ExoPlayer? = null
    private lateinit var mediaSessionHelper: MediaSessionHelper
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioFocusListener =
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    videoPlayerViewModelV2.pausePlaybackNonUserInitiated()
                }

                AudioManager.AUDIOFOCUS_GAIN -> {
                    // Do not resume when the activity is not visible (e.g. app in background).
                    // Otherwise, transient focus loss (ringtone, notification sounds) can call
                    // AUDIOFOCUS_GAIN after the user left the player, incorrectly restarting video.
                    // Do not resume when the user explicitly paused — wait for their play action.
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) &&
                        videoPlayerViewModelV2.shouldResumeOnAudioFocusGain()
                    ) {
                        mediaPlayerGateway.setPlayWhenReady(true)
                    }
                }
            }
        }

    override fun attachBaseContext(newBase: Context?) {
        delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Analytics.tracker.trackEvent(VideoPlayerScreenEvent)
        window.clearFlags(FLAG_TRANSLUCENT_STATUS or FLAG_TRANSLUCENT_NAVIGATION)
        enableEdgeToEdge()
        pipManager.initialize()
        setupImmersiveMode()
        exoPlayer = createPlayer()
        videoPlayerViewModelV2.initRepeatToggleMode()
        setContent {
            val mode by monitorThemeModeUseCase()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val uiState by videoPlayerViewModelV2.uiState.collectAsStateWithLifecycle()

            val isFromLinkWithoutLogin = uiState.isFromLink && !uiState.isLoggedIn
            LegacyActivityScaffold(
                container = { content ->
                    MegaAppContainer(
                        themeMode = mode,
                        isSessionRequired = !isFromLinkWithoutLogin,
                        finishOnSessionRefresh = false,
                        content = content,
                    )
                },
                initialKey = VideoPlayerScreenNavKey,
                navigationResultManager = navigationResultManager,
                featureDestinations = featureDestinations,
                overlayContent = {
                    // LegacyActivityScaffold always installs LocalSnackBarHostState before
                    // composing overlayContent — fail fast if that invariant ever breaks.
                    val snackbarHostState = requireNotNull(LocalSnackBarHostState.current) {
                        "LocalSnackBarHostState not provided"
                    }
                    LaunchedEffect(uiState.snackBarMessage) {
                        uiState.snackBarMessage?.let { message ->
                            snackbarHostState.showAutoDurationSnackbar(message)
                            videoPlayerViewModelV2.updateSnackBarMessage(null)
                        }
                    }
                },
            ) { navigationHandler, transferHandler ->
                videoPlayerEntryProvider(
                    navigationHandler = navigationHandler,
                    viewModel = videoPlayerViewModelV2,
                    player = exoPlayer,
                    handleAutoReplayIfPaused = videoPlayerViewModelV2::handleAutoReplayIfPaused,
                    onTransfer = transferHandler::setTransferEvent,
                    onRetry = { mediaPlayerGateway.mediaPlayerRetry(true) },
                    onFinish = { if (!isFinishing) finish() },
                    onEnterPip = pipManager::enterPipModeIfPossible,
                )
            }
        }
        videoPlayerViewModelV2.initVideoPlayerData(intent)
        LegacyAudioPlayerService.pauseAudioPlayer(this)
        registerReceiver(headsetPlugReceiver, IntentFilter(Intent.ACTION_HEADSET_PLUG))
        setupObserver()
        initMediaSession()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        pipManager.onPipModeChanged(isInPictureInPictureMode)
        videoPlayerViewModelV2.updateIsInPipMode(isInPictureInPictureMode)
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

    private fun setupImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    private fun createPlayer(): ExoPlayer {
        val nameChangeCallback: (title: String?, artist: String?, album: String?) -> Unit =
            { title, artist, album ->
                with(videoPlayerViewModelV2) {
                    val playingItemTitle = uiState.value.currentPlayingItemName ?: ""
                    updateMetadata(Metadata(title, artist, album, playingItemTitle))
                }
            }

        return mediaPlayerGateway.createPlayer(
            repeatToggleMode = RepeatToggleMode.REPEAT_NONE,
            nameChangeCallback = nameChangeCallback,
            mediaPlayerCallback = object : MediaPlayerCallback {
                override fun onMediaItemTransitionCallback(handle: String?, isUpdateName: Boolean) {
                    videoPlayerViewModelV2.onMediaItemTransition(handle, isUpdateName)
                }

                override fun onShuffleModeEnabledChangedCallback(shuffleModeEnabled: Boolean) {
                }

                override fun onRepeatModeChangedCallback(repeatToggleMode: RepeatToggleMode) =
                    videoPlayerViewModelV2.updateRepeatToggleMode(repeatToggleMode)

                override fun onPlayWhenReadyChangedCallback(playWhenReady: Boolean, reason: Int) {
                    val isPausedByUser =
                        reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST && !playWhenReady
                    videoPlayerViewModelV2.onPlayWhenReadyChanged(
                        state = if (playWhenReady) {
                            MediaPlaybackState.Playing
                        } else {
                            MediaPlaybackState.Paused
                        },
                        isPausedByUser = isPausedByUser
                    )
                }

                override fun onPlaybackStateChangedCallback(state: Int) {
                    videoPlayerViewModelV2.onPlaybackStateChanged(state)
                }

                override fun onPlayerErrorCallback(errorCode: Int) =
                    videoPlayerViewModelV2.onPlayerError(errorCode)

                override fun onVideoNotRenderedCallback() =
                    videoPlayerViewModelV2.onPlayerError(PlaybackException.ERROR_CODE_DECODER_INIT_FAILED)

                override fun onVideoSizeCallback(videoWidth: Int, videoHeight: Int) {
                    if (videoWidth == 0 || videoHeight == 0) return
                    videoPlayerViewModelV2.updateCurrentPlayingVideoSize(
                        VideoSize(videoWidth, videoHeight)
                    )
                }
            }
        )
    }

    private fun setupObserver() {
        mediaPlayerGateway.monitorMediaNotAllowPlayState().onEach { notAllow ->
            if (notAllow) {
                videoPlayerViewModelV2.updateSnackBarMessage(getString(R.string.not_allow_play_alert))
            }
        }.launchIn(lifecycleScope)

        videoPlayerViewModelV2.onSnackbarMessage().observe(this) { message ->
            videoPlayerViewModelV2.updateSnackBarMessage(getString(message))
        }

        collectFlow(videoPlayerViewModelV2.uiState.map { it.isClosedAfterHidingNode }
            .distinctUntilChanged()) { isClosed ->
            if (isClosed) {
                finish()
            }
        }
    }

    private fun initMediaSession() {
        audioManager = (getSystemService(AUDIO_SERVICE) as AudioManager)
        audioFocusRequest = getRequest(audioFocusListener, AUDIOFOCUS_DEFAULT)
        mediaSessionHelper = MediaSessionHelper(
            applicationContext,
            onPlayPauseClicked = {
                mediaPlayerGateway.setPlayWhenReady(!mediaPlayerGateway.getPlayWhenReady())
            },
            onNextClicked = { mediaPlayerGateway.playNext() },
            onPreviousClicked = { mediaPlayerGateway.playPrev() }
        )
        audioFocusRequest?.let {
            if (audioManager?.requestAudioFocus(it) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mediaSessionHelper.setupMediaSession()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (pipManager.onStop()) return
        videoPlayerViewModelV2.pauseForBackground()
    }

    override fun onStart() {
        super.onStart()
        pipManager.onStart()
        videoPlayerViewModelV2.handleAutoReplayIfPaused()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (videoPlayerViewModelV2.uiState.value.isInPipMode) {
            // Exit PIP mode so the new video replaces the current one in full screen
            videoPlayerViewModelV2.updateIsInPipMode(false)
        }
        this@VideoPlayerActivity.intent = intent
        videoPlayerViewModelV2.initVideoPlayerData(intent)
    }

    override fun onDestroy() {
        pipManager.onDestroy()
        if (!pipManager.isBeingReplacedByNewInstance) {
            mediaPlayerGateway.playerStop()
            mediaPlayerGateway.playerRelease()
        }
        LegacyAudioPlayerService.resumeAudioPlayer(this)
        unregisterReceiver(headsetPlugReceiver)
        if (audioManager != null) {
            ChatUtil.abandonAudioFocus(audioFocusListener, audioManager, audioFocusRequest)
        }
        mediaSessionHelper.releaseMediaSession()
        super.onDestroy()
    }

    override fun showMegaSnackbar(
        message: String,
        actionLabel: String?,
        duration: MegaSnackbarDuration,
    ) {
        lifecycleScope.launch {
            snackbarEventQueue.queueMessage(
                SnackbarAttributes(
                    message = message,
                    action = actionLabel,
                    duration = mapMegaSnackbarDuration(duration),
                )
            )
        }
    }

    private fun mapMegaSnackbarDuration(snackbarDuration: MegaSnackbarDuration): SnackbarDuration =
        when (snackbarDuration) {
            MegaSnackbarDuration.Short -> SnackbarDuration.Short
            MegaSnackbarDuration.Long -> SnackbarDuration.Long
            MegaSnackbarDuration.Indefinite -> SnackbarDuration.Indefinite
        }

    companion object {
        private const val INTENT_KEY_STATE = "state"
        private const val STATE_HEADSET_UNPLUGGED = 0
    }
}

