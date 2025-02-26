package mega.privacy.android.app.mediaplayer

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.components.session.SessionContainer
import mega.privacy.android.app.di.mediaplayer.VideoPlayer
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.service.AudioPlayerService
import mega.privacy.android.app.mediaplayer.service.MediaPlayerCallback
import mega.privacy.android.app.mediaplayer.videoplayer.navigation.navigateToVideoPlayerComposeGraph
import mega.privacy.android.app.mediaplayer.videoplayer.navigation.videoPlayerComposeNavigationGraph
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.psa.PsaContainer
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerViewModel
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
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

    private val viewModel: VideoPlayerViewModel by viewModels()

    override fun attachBaseContext(newBase: Context?) {
        delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val player = mediaPlayerGateway.createPlayer(
            repeatToggleMode = RepeatToggleMode.REPEAT_NONE,
            nameChangeCallback = { _, _, _ -> },
            mediaPlayerCallback = object : MediaPlayerCallback {
                override fun onMediaItemTransitionCallback(handle: String?, isUpdateName: Boolean) {
                }

                override fun onShuffleModeEnabledChangedCallback(shuffleModeEnabled: Boolean) {
                }

                override fun onRepeatModeChangedCallback(repeatToggleMode: RepeatToggleMode) {
                }

                override fun onPlayWhenReadyChangedCallback(playWhenReady: Boolean) {
                }

                override fun onPlaybackStateChangedCallback(state: Int) {
                }

                override fun onPlayerErrorCallback() {
                }

                override fun onVideoSizeCallback(videoWidth: Int, videoHeight: Int) {
                }
            }
        )

        setContent {
            val mode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            var passcodeEnabled by remember { mutableStateOf(true) }

            SessionContainer {
                OriginalTheme(isDark = mode.isDarkMode()) {
                    PasscodeContainer(
                        passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                        canLock = { passcodeEnabled },
                        content = {
                            PsaContainer {
                                val bottomSheetNavigator = rememberBottomSheetNavigator()
                                val navHostController =
                                    rememberNavController(bottomSheetNavigator)
                                val scaffoldState = rememberScaffoldState()

                                NavHost(
                                    navController = navHostController,
                                    startDestination = "start"
                                ) {
                                    composable("start") {
                                        navHostController.navigateToVideoPlayerComposeGraph(
                                            navOptions = navOptions {
                                                popUpTo("start") {
                                                    inclusive = true
                                                }
                                            }
                                        )
                                    }

                                    videoPlayerComposeNavigationGraph(
                                        bottomSheetNavigator = bottomSheetNavigator,
                                        scaffoldState = scaffoldState,
                                        player = player
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }

        viewModel.initVideoPlaybackSources(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayerGateway.playerStop()
        mediaPlayerGateway.playerRelease()
        AudioPlayerService.resumeAudioPlayer(this)
    }
}