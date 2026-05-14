package mega.privacy.android.app.presentation.videoplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
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
import mega.privacy.android.app.appstate.content.navigation.NavigationResultManager
import mega.privacy.android.app.appstate.content.navigation.rememberPendingBackStack
import mega.privacy.android.app.appstate.content.transfer.AppTransferViewModel
import mega.privacy.android.app.appstate.global.snackbar.SnackbarEventsViewModel
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.di.mediaplayer.VideoPlayer
import mega.privacy.android.app.mediaplayer.MediaSessionHelper
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.service.AudioPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerCallback
import mega.privacy.android.app.mediaplayer.service.Metadata
import mega.privacy.android.app.presentation.container.AppContainer
import mega.privacy.android.app.presentation.psa.PsaContainer
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.app.presentation.snackbar.MegaSnackbarShower
import mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponent
import mega.privacy.android.app.presentation.videoplayer.model.MediaPlaybackState
import mega.privacy.android.app.presentation.videoplayer.model.VideoSize
import mega.privacy.android.app.presentation.videoplayer.navigation.VideoPlayerNavigationHandler
import mega.privacy.android.app.presentation.videoplayer.navigation.VideoPlayerScreenNavKey
import mega.privacy.android.app.presentation.videoplayer.navigation.videoPlayerEntryProvider
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.ChatUtil.AUDIOFOCUS_DEFAULT
import mega.privacy.android.app.utils.ChatUtil.getRequest
import mega.privacy.android.core.sharedcomponents.extension.isDarkMode
import mega.privacy.android.core.sharedcomponents.snackbar.MegaSnackbarDuration
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.bottomsheet.BottomSheetSceneStrategy
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.mobile.analytics.event.VideoPlayerScreenEvent
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

    private val videoPlayerViewModelV2: VideoPlayerViewModelV2 by viewModels()

    private val headsetPlugReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_HEADSET_PLUG) {
                if (intent.getIntExtra(INTENT_KEY_STATE, -1) == STATE_HEADSET_UNPLUGGED) {
                    mediaPlayerGateway.setPlayWhenReady(false)
                }
            }
        }
    }

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
        enableEdgeToEdge()
        setupImmersiveMode()
        val player = createPlayer()
        videoPlayerViewModelV2.initRepeatToggleMode()
        setContent {
            val mode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            var passcodeEnabled by remember { mutableStateOf(true) }
            val backStack = rememberPendingBackStack(VideoPlayerScreenNavKey)
            val navigationHandler = remember {
                VideoPlayerNavigationHandler(backStack, navigationResultManager)
            }
            val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }
            val bottomSheetStrategy = remember { BottomSheetSceneStrategy<NavKey>() }
            val uiState by videoPlayerViewModelV2.uiState.collectAsStateWithLifecycle()
            val appTransferViewModel = hiltViewModel<AppTransferViewModel>()
            val transferState by appTransferViewModel.state.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }
            val snackbarEventsViewModel = hiltViewModel<SnackbarEventsViewModel>()
            val snackbarEventsState by snackbarEventsViewModel.snackbarEventState.collectAsStateWithLifecycle()

            LaunchedEffect(uiState.snackBarMessage) {
                uiState.snackBarMessage?.let { message ->
                    snackbarHostState.showAutoDurationSnackbar(message)
                    videoPlayerViewModelV2.updateSnackBarMessage(null)
                }
            }

            EventEffect(
                event = snackbarEventsState,
                onConsumed = snackbarEventsViewModel::consumeEvent,
                action = { event ->
                    snackbarHostState.showAutoDurationSnackbar(event.attributes.message.orEmpty())
                }
            )

            val containers: List<@Composable (@Composable () -> Unit) -> Unit> = listOf(
                { OriginalTheme(isDark = mode.isDarkMode(), content = it) },
                {
                    PasscodeContainer(
                        canLock = { passcodeEnabled },
                        content = it
                    )
                },
                { PsaContainer(content = it) }
            )

            AppContainer(
                containers = containers
            ) {
                CompositionLocalProvider(
                    LocalSnackBarHostState provides snackbarHostState
                ) {
                    NavDisplay(
                        backStack = backStack,
                        onBack = { navigationHandler.back() },
                        sceneStrategies = listOf(dialogStrategy, bottomSheetStrategy),
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                        ),
                        entryProvider = entryProvider {
                            videoPlayerEntryProvider(
                                navigationHandler = navigationHandler,
                                viewModel = videoPlayerViewModelV2,
                                player = player,
                                handleAutoReplayIfPaused = videoPlayerViewModelV2::handleAutoReplayIfPaused,
                                onTransfer = appTransferViewModel::setTransferEvent,
                                featureDestinations = featureDestinations,
                                onRetry = { mediaPlayerGateway.mediaPlayerRetry(true) },
                                onFinish = { if (!isFinishing) finish() },
                            )
                        },
                    )

                    StartTransferComponent(
                        event = transferState.transferEvent,
                        onConsumeEvent = appTransferViewModel::consumedTransferEvent,
                    )
                }
            }
        }
        videoPlayerViewModelV2.initVideoPlayerData(intent)
        AudioPlayerService.pauseAudioPlayer(this)
        registerReceiver(headsetPlugReceiver, IntentFilter(Intent.ACTION_HEADSET_PLUG))
        setupObserver()
        initMediaSession()
    }

    private fun setupImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS

            window.setFlags(FLAG_LAYOUT_NO_LIMITS, FLAG_LAYOUT_NO_LIMITS)

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

                override fun onPlayerErrorCallback(errorCode: Int) = videoPlayerViewModelV2.onPlayerError(errorCode)

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
        videoPlayerViewModelV2.pauseForBackground()
    }

    override fun onStart() {
        super.onStart()
        videoPlayerViewModelV2.handleAutoReplayIfPaused()
    }

    override fun onDestroy() {
        mediaPlayerGateway.playerStop()
        mediaPlayerGateway.playerRelease()
        AudioPlayerService.resumeAudioPlayer(this)
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

