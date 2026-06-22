package mega.privacy.android.app.presentation.videoplayer.view

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.systemuicontroller.SystemUiController

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
