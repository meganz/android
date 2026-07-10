package mega.privacy.android.app.presentation.videoplayer.view

import android.app.Activity
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.SystemUiController

/**
 * Native-API overload that requires no Accompanist dependency.
 *
 * Uses [WindowCompat.getInsetsController] and [android.view.Window.navigationBarColor] directly.
 * Restores the original navigation bar appearance on dispose.
 *
 * The Accompanist overload below is kept for the video player until it is migrated separately.
 */
@Composable
internal fun TransparentNavigationBarEffect() {
    val view = LocalView.current
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window ?: return@DisposableEffect onDispose {}
        val insetsController = WindowCompat.getInsetsController(window, view)
        val originalNavBarColor = window.navigationBarColor
        val originalLightIcons = insetsController.isAppearanceLightNavigationBars
        val originalContrastEnforced = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced
        } else {
            false
        }

        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        insetsController.isAppearanceLightNavigationBars = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        onDispose {
            window.navigationBarColor = originalNavBarColor
            insetsController.isAppearanceLightNavigationBars = originalLightIcons
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = originalContrastEnforced
            }
        }
    }
}

/**
 * Forces the system navigation bar fully transparent — with the contrast scrim disabled — for as
 * long as this effect stays in composition, then restores the window's original navigation bar
 * color, icon contrast, contrast-scrim enforcement and visibility when it leaves.
 *
 * The transparency keeps the bar from covering full-screen content (e.g. while entering or adjusting
 * full-screen mode), and disabling contrast enforcement avoids the translucent scrim the system
 * applies to transparent bars on API 29+ (most visible with 3-button navigation).
 *
 * Restoring on dispose matters for screens hosted inside the shared single-activity window: the
 * navigation bar state (and any immersive/hidden state) would otherwise leak into the next
 * destination. Accompanist exposes no getter for the bar color, so it is read from the window before
 * being overridden.
 *
 * @param systemUiController the controller for the hosting window, e.g. from `rememberSystemUiController()`.
 */
@Composable
internal fun TransparentNavigationBarEffect(
    systemUiController: SystemUiController,
) {
    val context = LocalContext.current
    DisposableEffect(systemUiController) {
        val window = (context as? Activity)?.window
        val originalNavBarColor = window?.navigationBarColor?.let { Color(it) }
        val originalDarkIcons = systemUiController.navigationBarDarkContentEnabled
        val originalContrastEnforced = systemUiController.isNavigationBarContrastEnforced

        systemUiController.setNavigationBarColor(
            color = Color.Transparent,
            darkIcons = false,
            navigationBarContrastEnforced = false,
        )
        onDispose {
            systemUiController.isSystemBarsVisible = true
            if (originalNavBarColor != null) {
                systemUiController.setNavigationBarColor(
                    color = originalNavBarColor,
                    darkIcons = originalDarkIcons,
                    navigationBarContrastEnforced = originalContrastEnforced,
                )
            } else {
                systemUiController.isNavigationBarContrastEnforced = originalContrastEnforced
            }
        }
    }
}
