package mega.privacy.android.shared.original.core.ui.utils

import android.os.Build
import android.view.Window
import androidx.core.splashscreen.SplashScreen
import androidx.core.view.WindowCompat

/**
 * Disables the splash screen exit animation to prevent a visual "jump" of the app icon.
 *
 * Skipped on Android 13 (Tiramisu) for certain Chinese OEMs (e.g., OPPO, Realme, OnePlus),
 * or specific models (e.g., Galaxy A03 Core), as it may cause crashes.
 * See: https://issuetracker.google.com/issues/242118185
 */
fun SplashScreen.setupSplashExitAnimation(window: Window) {
    val isAndroid13 = Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU
    val isAffectedBrand = Build.BRAND.lowercase() in setOf("oppo", "realme", "oneplus")
    val isAffectedModel = Build.MODEL.lowercase().contains("a03 core")

    if (isAndroid13 && (isAffectedBrand || isAffectedModel)) return

    setOnExitAnimationListener {
        it.remove()
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
}