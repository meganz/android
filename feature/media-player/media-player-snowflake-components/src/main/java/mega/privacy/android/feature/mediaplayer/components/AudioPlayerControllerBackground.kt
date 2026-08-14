package mega.privacy.android.feature.mediaplayer.components

import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import mega.android.core.ui.tokens.theme.DSTokens
import timber.log.Timber

/**
 * Owns all system-bar window state for the audio player screen and restores it when
 * the screen leaves composition.
 *
 * Sets the window state required by the dark audio player UI:
 * - Status bar and navigation bar colors → transparent (gradient shows through)
 * - Window background → DSTokens.colors.background.pageBackground (nav-bar area on API 35+)
 * - Status bar and navigation bar icon appearance → light (white icons for dark background)
 * - Navigation bar contrast enforcement → disabled (prevents system scrim)
 *
 * **Must be called inside mega.android.core.ui.theme.AndroidTheme with `isDark = true` and
 * `useLegacyStatusBarColor = false`.** The `useLegacyStatusBarColor = false` flag suppresses
 * AndroidTheme's own window-color SideEffect entirely, so this composable's [SideEffect] is
 * the sole owner of system-bar state. Calling this outside AndroidTheme, or without
 * `useLegacyStatusBarColor = false`, would let AndroidTheme's SideEffect fight and override
 * these settings on every recomposition.
 *
 * **Original values are preserved across composition re-entries using [rememberSaveable].**
 * Navigation3's slide-up transition removes AudioPlayerScreen from composition when QueueScreen
 * is pushed, then re-adds it when navigating back. Without preservation the re-entry would see the
 * dark-mode state (still applied by QueueScreen's effects) and save it as "original", causing dark
 * system bars on the Home screen after the player is closed. [rememberSaveable] is kept by
 * Navigation3's `rememberSaveableStateHolderNavEntryDecorator` across the round-trip, so the
 * first-entry (Home) state is used for restoration regardless of how many sub-screens were visited.
 */
@Suppress("DEPRECATION") // statusBarColor / navigationBarColor deprecated on API 35+; all calls are guarded with SDK_INT < VANILLA_ICE_CREAM
@Composable
fun AudioPlayerWindowEffect() {
    val pageBackground = DSTokens.colors.background.pageBackground
    val pageBackgroundDrawable = remember(pageBackground) { pageBackground.toArgb().toDrawable() }
    val activity = LocalActivity.current
    val view = LocalView.current

    // Preserved across composition re-entries by Navigation3's SaveableStateHolder.
    // Only populated on the very first entry so that re-entries (e.g. back from QueueScreen)
    // do not overwrite the Home-screen originals with the dark-mode values that are active
    // while QueueScreen's AudioPlayerWindowEffect is still running.
    var hasSavedOriginals by rememberSaveable { mutableStateOf(false) }
    var originalStatusBarColor by rememberSaveable { mutableIntStateOf(0) }
    var originalNavBarColor by rememberSaveable { mutableIntStateOf(0) }
    var originalBgColorSaved by rememberSaveable { mutableStateOf(false) }
    var originalBgColor by rememberSaveable { mutableIntStateOf(0) }
    var originalLightStatusBars by rememberSaveable { mutableStateOf(true) }
    var originalLightNavBars by rememberSaveable { mutableStateOf(true) }
    var originalContrastEnforced by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(activity, view) {
        val window = activity?.window ?: run {
            Timber.w("AudioPlayerWindowEffect: activity or window is null, skipping")
            return@DisposableEffect onDispose {}
        }
        val insetsController = WindowCompat.getInsetsController(window, view)

        if (!hasSavedOriginals) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                originalStatusBarColor = window.statusBarColor
                originalNavBarColor = window.navigationBarColor
            }
            val bgDrawable = window.decorView.background
            if (bgDrawable is ColorDrawable) {
                originalBgColor = bgDrawable.color
                originalBgColorSaved = true
            }
            originalLightStatusBars = insetsController.isAppearanceLightStatusBars
            originalLightNavBars = insetsController.isAppearanceLightNavigationBars
            originalContrastEnforced = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced
            } else {
                false
            }
            hasSavedOriginals = true
        }

        onDispose {
            runCatching {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    window.statusBarColor = originalStatusBarColor
                    window.navigationBarColor = originalNavBarColor
                }
            }.onFailure { Timber.e(it, "Failed to restore status/nav bar colors") }

            runCatching {
                if (originalBgColorSaved) {
                    window.setBackgroundDrawable(originalBgColor.toDrawable())
                } else {
                    window.setBackgroundDrawable(null)
                }
            }.onFailure { Timber.e(it, "Failed to restore window background") }

            runCatching {
                insetsController.isAppearanceLightStatusBars = originalLightStatusBars
                insetsController.isAppearanceLightNavigationBars = originalLightNavBars
            }.onFailure { Timber.e(it, "Failed to restore light bar appearance") }

            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = originalContrastEnforced
                }
            }.onFailure { Timber.e(it, "Failed to restore navigation bar contrast enforcement") }
        }
    }

    SideEffect {
        val window = activity?.window ?: run {
            Timber.w("AudioPlayerWindowEffect: activity or window is null, skipping SideEffect")
            return@SideEffect
        }
        val insetsController = WindowCompat.getInsetsController(window, view)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.statusBarColor = AndroidColor.TRANSPARENT
            window.navigationBarColor = AndroidColor.TRANSPARENT
        }
        window.setBackgroundDrawable(pageBackgroundDrawable)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
    }
}
