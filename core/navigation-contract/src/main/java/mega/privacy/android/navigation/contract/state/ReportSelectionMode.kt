package mega.privacy.android.navigation.contract.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

/**
 * Reports the current selection mode state to [LocalSelectionModeController].
 * Call this from screens that support multiple selection (e.g. RecentsBucketScreen, DriveSyncScreen)
 * to hide the mini player when in selection mode.
 *
 * @param isInSelectionMode true when the screen is in selection mode
 */
@Composable
fun ReportSelectionMode(isInSelectionMode: Boolean) {
    val controller = LocalSelectionModeController.current

    DisposableEffect(isInSelectionMode) {
        controller.onSelectionModeChanged(isInSelectionMode)
        onDispose { controller.onSelectionModeChanged(false) }
    }
}
