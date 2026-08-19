package mega.privacy.android.feature.mediaplayer.components

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.feature.mediaplayer.components.AudioPlayerWindowState.activeCount
import mega.privacy.android.feature.mediaplayer.components.AudioPlayerWindowState.savedActivity
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * Window state shared by all active [AudioPlayerWindowEffect] instances.
 *
 * [activeCount] guards restoration: originals are restored only when the last instance leaves
 * composition. Originals are saved at most **once per Activity** ([savedActivity]): when the
 * player round-trips to another destination and back (e.g. via a node-options action), the
 * window state at re-entry may have been polluted by overlays that write to the Activity window
 * without restoring (a force-dark bottom sheet's theme SideEffect can land after this effect's
 * restore during the exit transition). Re-saving at that point would capture the polluted values
 * and later "restore" them onto the home screen, so the first-entry originals are kept instead.
 * All access is on the main thread (Compose applier thread); no synchronisation needed.
 */
private object AudioPlayerWindowState {
    var activeCount = 0
    var savedActivity: WeakReference<Activity>? = null
    var statusBarColor = 0
    var navBarColor = 0
    var bgColor = 0
    var bgColorValid = false
    var lightStatusBars = true
    var lightNavBars = true
    var contrastEnforced = false
}

/**
 * Manages all system-bar window state for the audio player screens and restores it when
 * all audio player screens leave composition.
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
 * **Reference-counting across multiple screens.** Both AudioPlayerScreen and
 * AudioPlayerQueueScreen call this composable. When Navigation3's slide-up transition removes
 * AudioPlayerScreen from composition while QueueScreen is active (or vice versa during the back
 * transition), a naive per-screen save/restore would momentarily flash the home-screen bar colors.
 * To prevent this, original values are shared and saved only by the **first** instance to enter
 * composition, and restored only by the **last** instance to leave. As long as any audio-player
 * screen remains in the composition tree the home-screen colors are never touched.
 *
 * **Dark colors are applied both in [DisposableEffect] setup and [SideEffect].** Applying in
 * setup means the colors are set immediately at composition time — without waiting for a
 * recomposition. This is important because Compose's smart-recomposition may skip recomposing
 * a screen whose inputs have not changed (e.g. QueueScreen when AudioPlayerScreen is removed),
 * which would prevent [SideEffect] from re-running and leave the home-screen colors visible.
 */
@Suppress("DEPRECATION") // statusBarColor / navigationBarColor deprecated on API 35+; all calls are guarded with SDK_INT < VANILLA_ICE_CREAM
@Composable
fun AudioPlayerWindowEffect() {
    val pageBackground = DSTokens.colors.background.pageBackground
    val pageBackgroundDrawable = remember(pageBackground) { pageBackground.toArgb().toDrawable() }
    val activity = LocalActivity.current
    val view = LocalView.current

    DisposableEffect(activity, view) {
        val window = activity?.window ?: run {
            Timber.w("AudioPlayerWindowEffect: activity or window is null, skipping")
            return@DisposableEffect onDispose {}
        }
        val insetsController = WindowCompat.getInsetsController(window, view)

        with(AudioPlayerWindowState) {
            if (activeCount == 0 && savedActivity?.get() !== activity) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    statusBarColor = window.statusBarColor
                    navBarColor = window.navigationBarColor
                }
                val bgDrawable = window.decorView.background
                if (bgDrawable is ColorDrawable) {
                    bgColor = bgDrawable.color
                    bgColorValid = true
                } else {
                    bgColorValid = false
                }
                lightStatusBars = insetsController.isAppearanceLightStatusBars
                lightNavBars = insetsController.isAppearanceLightNavigationBars
                contrastEnforced = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced
                } else {
                    false
                }
                savedActivity = WeakReference(activity)
            }
            activeCount++
        }

        // Apply dark settings immediately at composition time.
        // SideEffect also applies them on every recomposition, but a screen whose inputs
        // have not changed may be skipped by smart-recomposition, leaving this DisposableEffect
        // setup as the only guarantee that the correct colors are active.
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

        onDispose {
            // Restore original values only when the last audio-player screen leaves composition.
            // While any audio-player screen is still active (count > 0) we must not restore,
            // otherwise navigating between AudioPlayerScreen and QueueScreen would flash the
            // home-screen bar colors.
            with(AudioPlayerWindowState) {
                activeCount--
                if (activeCount == 0) {
                    runCatching {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                            window.statusBarColor = statusBarColor
                            window.navigationBarColor = navBarColor
                        }
                    }.onFailure { Timber.e(it, "Failed to restore status/nav bar colors") }

                    runCatching {
                        if (bgColorValid) {
                            window.setBackgroundDrawable(bgColor.toDrawable())
                        } else {
                            window.setBackgroundDrawable(null)
                        }
                    }.onFailure { Timber.e(it, "Failed to restore window background") }

                    runCatching {
                        insetsController.isAppearanceLightStatusBars = lightStatusBars
                        insetsController.isAppearanceLightNavigationBars = lightNavBars
                    }.onFailure { Timber.e(it, "Failed to restore light bar appearance") }

                    runCatching {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            window.isNavigationBarContrastEnforced = contrastEnforced
                        }
                    }.onFailure {
                        Timber.e(
                            it,
                            "Failed to restore navigation bar contrast enforcement"
                        )
                    }
                }
            }
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
