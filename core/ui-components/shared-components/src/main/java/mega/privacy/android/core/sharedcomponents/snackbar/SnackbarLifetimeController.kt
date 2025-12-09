package mega.privacy.android.core.sharedcomponents.snackbar

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarData
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.LocalAccessibilityManager
import kotlinx.coroutines.delay
import mega.android.core.ui.components.LocalSnackBarHostState

/**
 * Composable that controls the lifetime of the current snackbar based on its duration
 * and accessibility settings.
 *
 * This is a temporary workaround while the same [SnackbarData] is shared across
 * multiple [SnackbarHost] instances. It ensures that the snackbar is dismissed after
 * the expected duration, regardless of whether it is restarted or re-hosted by another
 * [SnackbarHost].
 *
 * It basically does the same as the [SnackbarHost] but without actually showing the Snackbar.
 *
 * Place this composable anywhere within a scope where [LocalSnackBarHostState] is provided
 * to automatically enforce correct timeout behavior for snackbars.
 */
@Composable
fun SnackbarLifetimeController() {
    val currentSnackbarData = LocalSnackBarHostState.current?.currentSnackbarData
    val accessibilityManager = LocalAccessibilityManager.current
    LaunchedEffect(currentSnackbarData) {
        if (currentSnackbarData != null) {
            val duration = currentSnackbarData.visuals.duration.toMillis(
                currentSnackbarData.visuals.actionLabel != null,
                accessibilityManager,
            )
            delay(duration)
            currentSnackbarData.dismiss()
        }
    }
}

/**
 * This is a copy of the method in SnackbarHost, because it's currently internal and we want to use the same duration used there
 */
private fun SnackbarDuration.toMillis(
    hasAction: Boolean,
    accessibilityManager: AccessibilityManager?,
): Long {
    val original =
        when (this) {
            SnackbarDuration.Indefinite -> Long.MAX_VALUE
            SnackbarDuration.Long -> 10000L
            SnackbarDuration.Short -> 4000L
        }
    if (accessibilityManager == null) {
        return original
    }
    return accessibilityManager.calculateRecommendedTimeoutMillis(
        original,
        containsIcons = true,
        containsText = true,
        containsControls = hasAction,
    )
}