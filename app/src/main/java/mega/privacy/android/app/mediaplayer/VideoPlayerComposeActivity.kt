package mega.privacy.android.app.mediaplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
import android.database.ContentObserver
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.provider.Settings.System.ACCELEROMETER_ROTATION
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.di.mediaplayer.VideoPlayer
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.service.AudioPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerCallback
import mega.privacy.android.app.mediaplayer.service.Metadata
import mega.privacy.android.app.mediaplayer.videoplayer.navigation.VideoPlayerNavigationGraph
import mega.privacy.android.app.mediaplayer.videoplayer.navigation.videoPlayerComposeNavigationGraph
import mega.privacy.android.app.presentation.container.AppContainer
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.psa.PsaContainer
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerViewModel
import mega.privacy.android.app.presentation.videoplayer.model.MediaPlaybackState
import mega.privacy.android.app.presentation.videoplayer.model.VideoSize
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.ChatUtil.AUDIOFOCUS_DEFAULT
import mega.privacy.android.app.utils.ChatUtil.getRequest
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.mobile.analytics.event.VideoPlayerIsActivatedEvent
import javax.inject.Inject

/**
 * The activity for the video player
 */
@OptIn(ExperimentalMaterialNavigationApi::class)
@AndroidEntryPoint
class VideoPlayerComposeActivity : PasscodeActivity() {
    @Inject
    lateinit var getThemeMode: GetThemeMode

    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

    /**
     * MediaPlayerGateway for video player
     */
    @VideoPlayer
    @Inject
    lateinit var mediaPlayerGateway: MediaPlayerGateway

    private val videoPlayerViewModel: VideoPlayerViewModel by viewModels()

    private var currentOrientation: Int = SCREEN_ORIENTATION_SENSOR_PORTRAIT

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
                    mediaPlayerGateway.setPlayWhenReady(false)
                }

                AudioManager.AUDIOFOCUS_GAIN -> {
                    mediaPlayerGateway.setPlayWhenReady(true)
                }
            }
        }

    private val rotationContentObserver by lazy(LazyThreadSafetyMode.NONE) {
        object : ContentObserver(Handler(mainLooper)) {
            override fun onChange(selfChange: Boolean) {
                val rotationMode = Settings.System.getInt(
                    contentResolver,
                    ACCELEROMETER_ROTATION,
                    SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                requestedOrientation =
                    if (rotationMode == SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                        SCREEN_ORIENTATION_SENSOR
                    } else {
                        currentOrientation
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
        enableEdgeToEdge()
        currentOrientation = resources.configuration.orientation
        observeRotationSettingsChange()
        val player = createPlayer()
        setContent {
            val mode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            var passcodeEnabled by remember { mutableStateOf(true) }

            val containers: List<@Composable (@Composable () -> Unit) -> Unit> = listOf(
                { OriginalTheme(isDark = mode.isDarkMode(), content = it) },
                {
                    PasscodeContainer(
                        passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                        canLock = { passcodeEnabled },
                        content = it
                    )
                },
                { PsaContainer(content = it) }
            )

            AppContainer(
                containers = containers
            ) {
                val bottomSheetNavigator = rememberBottomSheetNavigator()
                val navHostController =
                    rememberNavController(bottomSheetNavigator)
                val scaffoldState = rememberScaffoldState()

                NavHost(
                    navController = navHostController,
                    startDestination = VideoPlayerNavigationGraph
                ) {
                    videoPlayerComposeNavigationGraph(
                        bottomSheetNavigator = bottomSheetNavigator,
                        scaffoldState = scaffoldState,
                        viewModel = videoPlayerViewModel,
                        player = player
                    )
                }
            }
        }

        videoPlayerViewModel.initVideoPlaybackSources(intent)
        registerReceiver(headsetPlugReceiver, IntentFilter(Intent.ACTION_HEADSET_PLUG))
        setupObserver()
        initMediaSession()
    }

    private fun observeRotationSettingsChange() {
        contentResolver.registerContentObserver(
            Settings.System.getUriFor(ACCELEROMETER_ROTATION),
            true,
            rotationContentObserver
        )
    }

    private fun createPlayer(): ExoPlayer {
        val nameChangeCallback: (title: String?, artist: String?, album: String?) -> Unit =
            { title, artist, album ->
                with(videoPlayerViewModel) {
                    val currentPlayingItemHandle =
                        mediaPlayerGateway.getCurrentMediaItem()?.mediaId?.toLong()
                    val playingItemTitle =
                        uiState.value.items.find {
                            it.nodeHandle == currentPlayingItemHandle
                        }?.nodeName ?: ""

                    updateMetadata(Metadata(title, artist, album, playingItemTitle))
                }
            }

        return mediaPlayerGateway.createPlayer(
            repeatToggleMode = videoPlayerViewModel.uiState.value.repeatToggleMode,
            nameChangeCallback = nameChangeCallback,
            mediaPlayerCallback = object : MediaPlayerCallback {
                override fun onMediaItemTransitionCallback(handle: String?, isUpdateName: Boolean) {
                    with(videoPlayerViewModel) {
                        updateCurrentPlayingVideoSize(null)
                        if (handle == null) return
                        if (uiState.value.currentPlayingHandle != handle.toLong())
                            Analytics.tracker.trackEvent(VideoPlayerIsActivatedEvent)
                        updateCurrentPlayingHandle(handle.toLong())
                        saveVideoWatchedTime()
                        if (isUpdateName) {
                            val nodeName = uiState.value.items.find {
                                it.nodeHandle == handle.toLong()
                            }?.nodeName ?: ""
                            updateMetadata(Metadata(null, null, null, nodeName))
                        }

                        if (!mediaPlayerGateway.getPlayWhenReady()) {
                            mediaPlayerGateway.setPlayWhenReady(true)
                        }
                    }
                }

                override fun onShuffleModeEnabledChangedCallback(shuffleModeEnabled: Boolean) {
                }

                override fun onRepeatModeChangedCallback(repeatToggleMode: RepeatToggleMode) =
                    videoPlayerViewModel.updateRepeatToggleMode(repeatToggleMode)

                override fun onPlayWhenReadyChangedCallback(playWhenReady: Boolean) {
                    videoPlayerViewModel.updatePlaybackState(
                        if (playWhenReady)
                            MediaPlaybackState.Playing
                        else
                            MediaPlaybackState.Paused
                    )
                }

                override fun onPlaybackStateChangedCallback(state: Int) {
                    val playbackState = videoPlayerViewModel.uiState.value.mediaPlaybackState
                    when {
                        state == MEDIA_PLAYER_STATE_ENDED &&
                                playbackState == MediaPlaybackState.Playing ->
                            videoPlayerViewModel.updatePlaybackState(MediaPlaybackState.Paused)

                        state == MEDIA_PLAYER_STATE_READY -> {
                            if (playbackState == MediaPlaybackState.Paused
                                && !mediaPlayerGateway.getPlayWhenReady()
                            ) {
                                mediaPlayerGateway.setPlayWhenReady(true)
                            }
                        }
                    }
                }

                override fun onPlayerErrorCallback() = videoPlayerViewModel.onPlayerError()

                override fun onVideoSizeCallback(videoWidth: Int, videoHeight: Int) {
                    if (videoWidth == 0 || videoHeight == 0) return
                    videoPlayerViewModel.updateCurrentPlayingVideoSize(
                        VideoSize(videoWidth, videoHeight)
                    )
                    updateOrientationBasedOnVideoSize(videoWidth, videoHeight)
                }
            }
        )
    }

    /**
     * Update orientation according to the video size.
     *
     * @param videoWidth the width of the video
     * @param videoHeight the height of the video
     */
    private fun updateOrientationBasedOnVideoSize(videoWidth: Int, videoHeight: Int) {
        val rotationMode = Settings.System.getInt(
            contentResolver,
            ACCELEROMETER_ROTATION,
            SCREEN_BRIGHTNESS_MODE_MANUAL
        )

        currentOrientation =
            if (videoWidth > videoHeight) {
                SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                SCREEN_ORIENTATION_SENSOR_PORTRAIT
            }

        requestedOrientation =
            if (rotationMode == SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                SCREEN_ORIENTATION_SENSOR
            } else {
                currentOrientation
            }
    }

    private fun setupObserver() {
        mediaPlayerGateway.monitorMediaNotAllowPlayState().onEach { notAllow ->
            if (notAllow) {
                videoPlayerViewModel.updateSnackBarMessage(getString(R.string.not_allow_play_alert))
            }
        }.launchIn(lifecycleScope)
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

    override fun onDestroy() {
        super.onDestroy()
        contentResolver.unregisterContentObserver(rotationContentObserver)
        mediaPlayerGateway.playerStop()
        mediaPlayerGateway.playerRelease()
        AudioPlayerService.resumeAudioPlayer(this)
        unregisterReceiver(headsetPlugReceiver)
        if (audioManager != null) {
            ChatUtil.abandonAudioFocus(audioFocusListener, audioManager, audioFocusRequest)
        }
        mediaSessionHelper.releaseMediaSession()
    }

    companion object {
        private const val MEDIA_PLAYER_STATE_ENDED = 4
        private const val MEDIA_PLAYER_STATE_READY = 3

        private const val INTENT_KEY_STATE = "state"
        private const val STATE_HEADSET_UNPLUGGED = 0
    }
}