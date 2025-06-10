package mega.privacy.android.app.presentation.imagepreview.slideshow

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.bottomSheet
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.presentation.imagepreview.slideshow.view.SecureSlideshowTutorialBottomSheet
import mega.privacy.android.app.presentation.imagepreview.slideshow.view.SlideshowScreen
import mega.privacy.android.app.presentation.imagepreview.slideshow.view.SlideshowSettingScreen
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.shared.original.core.ui.controls.sheets.BottomSheetRoundedShape
import mega.privacy.android.shared.original.core.ui.controls.sheets.MegaBottomSheetLayout
import mega.privacy.android.shared.original.core.ui.navigation.rememberExtendedBottomSheetNavigator
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.mobile.analytics.event.SlideshowSecureModeActivatedEvent
import javax.inject.Inject


@AndroidEntryPoint
class SlideshowActivity : BaseActivity() {

    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase
    private val slideshowViewModel: SlideshowViewModel by viewModels()
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    if (isScreenLocked()) {
                        wasDeviceLocked = true
                    }
                }
            }
        }
    }
    private var wasDeviceLocked = false

    override fun shouldSetStatusBarTextColor() = false

    @OptIn(ExperimentalMaterialNavigationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        setupInsets()
        val insetsController = WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        super.onCreate(savedInstanceState)
        activeSecureMode()
        registerScreenLockBroadcastReceiver()

        setContent {
            val bottomSheetNavigator = rememberExtendedBottomSheetNavigator()
            val navController = rememberNavController(bottomSheetNavigator)
            val systemUiController = rememberSystemUiController()
            val isDarkMode = true

            LaunchedEffect(systemUiController, isDarkMode) {
                systemUiController.setSystemBarsColor(
                    color = Color.Transparent,
                    darkIcons = !isDarkMode
                )
            }

            val uiState by slideshowViewModel.state.collectAsStateWithLifecycle()
            LaunchedEffect(uiState.isPlaying) {
                if (uiState.isPlaying) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            OriginalTheme(isDark = isDarkMode) {
                MegaBottomSheetLayout(
                    bottomSheetNavigator = bottomSheetNavigator,
                    sheetShape = BottomSheetRoundedShape()
                ) {
                    NavHost(navController, startDestination = SlideshowRoute) {
                        composable(SlideshowRoute) {
                            SlideshowScreen(
                                viewModel = slideshowViewModel,
                                onNavigate = navController::navigate,
                                onFullScreenModeChanged = { inFullScreenMode ->
                                    if (inFullScreenMode) {
                                        insetsController.hide(WindowInsetsCompat.Type.systemBars())
                                    } else {
                                        insetsController.show(WindowInsetsCompat.Type.systemBars())
                                    }
                                },
                                onClickBack = ::finish,
                            )
                        }

                        composable(SlideshowSettingsRoute) {
                            SlideshowSettingScreen()
                        }

                        bottomSheet(SlideshowSecureTutorialRoute) {
                            SecureSlideshowTutorialBottomSheet(
                                onDismiss = {
                                    navController.popBackStack()
                                    slideshowViewModel.setSecureSlideshowTutorialShown()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Active secure mode which allows the slideshow to be shown on top of the lock screen
     */
    private fun activeSecureMode() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }
        }
    }

    private fun setupInsets() {
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            // No scrim behind transparent navigation bar.
            window.setFlags(FLAG_LAYOUT_NO_LIMITS, FLAG_LAYOUT_NO_LIMITS)
        }
    }

    private fun isScreenLocked() =
        (getSystemService(KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked

    private fun registerScreenLockBroadcastReceiver() {
        registerReceiver(
            receiver = screenReceiver,
            filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
            }
        )
    }

    override fun onPostResume() {
        super.onPostResume()
        // Send analytics event when slideshow is activated
        if (wasDeviceLocked) {
            wasDeviceLocked = false
            Analytics.tracker.trackEvent(SlideshowSecureModeActivatedEvent)
        }
    }

    override fun onDestroy() {
        slideshowViewModel.clearImageResultCache()
        unregisterReceiver(screenReceiver)
        super.onDestroy()
    }
}


internal const val SlideshowRoute = "slideshow"
internal const val SlideshowSettingsRoute = "settings"
internal const val SlideshowSecureTutorialRoute = "secure-tutorial"