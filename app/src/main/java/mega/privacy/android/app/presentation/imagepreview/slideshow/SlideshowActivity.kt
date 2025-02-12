package mega.privacy.android.app.presentation.imagepreview.slideshow

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.imagepreview.slideshow.view.SlideshowScreen
import mega.privacy.android.app.presentation.imagepreview.slideshow.view.SlideshowSettingScreen
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import javax.inject.Inject

@AndroidEntryPoint
class SlideshowActivity : BaseActivity() {

    @Inject
    lateinit var getThemeMode: GetThemeMode
    private val slideshowViewModel: SlideshowViewModel by viewModels()

    override fun shouldSetStatusBarTextColor() = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setupInsets()
        val insetsController = WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            val navController = rememberNavController()
            val systemUiController = rememberSystemUiController()

            val theme by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val isDarkMode = theme.isDarkMode()

            LaunchedEffect(systemUiController, isDarkMode) {
                systemUiController.setSystemBarsColor(
                    color = Color.Transparent,
                    darkIcons = !isDarkMode
                )
            }

            OriginalTempTheme(isDark = isDarkMode) {
                NavHost(navController, startDestination = "slideshow") {
                    composable("slideshow") {
                        val state by slideshowViewModel.state.collectAsStateWithLifecycle()
                        LaunchedEffect(state.isPlaying) {
                            if (state.isPlaying) {
                                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                            } else {
                                insetsController.show(WindowInsetsCompat.Type.systemBars())
                            }
                        }

                        SlideshowScreen(
                            viewModel = slideshowViewModel,
                            onClickSettingMenu = {
                                navController.navigate("slideshowSetting")
                            },
                            onClickBack = ::finish,
                        )
                    }

                    composable("slideshowSetting") {
                        SlideshowSettingScreen()
                    }
                }
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
        }
    }

    override fun onDestroy() {
        slideshowViewModel.clearImageResultCache()
        super.onDestroy()
    }
}