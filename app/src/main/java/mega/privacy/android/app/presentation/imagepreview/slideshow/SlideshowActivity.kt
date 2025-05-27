package mega.privacy.android.app.presentation.imagepreview.slideshow

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.bottomSheet
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.presentation.imagepreview.slideshow.view.SecureSlideshowTutorialBottomSheet
import mega.privacy.android.app.presentation.imagepreview.slideshow.view.SlideshowScreen
import mega.privacy.android.app.presentation.imagepreview.slideshow.view.SlideshowSettingScreen
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.controls.sheets.MegaBottomSheetLayout
import mega.privacy.android.shared.original.core.ui.navigation.rememberExtendedBottomSheetNavigator
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import javax.inject.Inject

@AndroidEntryPoint
class SlideshowActivity : BaseActivity() {

    @Inject
    lateinit var getThemeMode: GetThemeMode
    private val slideshowViewModel: SlideshowViewModel by viewModels()

    override fun shouldSetStatusBarTextColor() = false

    @OptIn(ExperimentalMaterialNavigationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        setupInsets()
        val insetsController = WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        activeSecureMode()

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

            OriginalTheme(isDark = isDarkMode) {
                MegaBottomSheetLayout(
                    bottomSheetNavigator = bottomSheetNavigator,
                    sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            var leftInset = insets.stableInsetLeft
            var rightInset = insets.stableInsetRight
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                leftInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).left
                rightInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).right
            }

            v.setPadding(leftInset, 0, rightInset, 0)
            insets
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            // No scrim behind transparent navigation bar.
            window.setFlags(FLAG_LAYOUT_NO_LIMITS, FLAG_LAYOUT_NO_LIMITS)
        }
    }

    override fun onDestroy() {
        slideshowViewModel.clearImageResultCache()
        super.onDestroy()
    }
}

internal const val SlideshowRoute = "slideshow"
internal const val SlideshowSettingsRoute = "settings"
internal const val SlideshowSecureTutorialRoute = "secure-tutorial"