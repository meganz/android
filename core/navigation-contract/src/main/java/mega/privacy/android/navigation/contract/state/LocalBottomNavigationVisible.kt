package mega.privacy.android.navigation.contract.state

import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal providing the current visibility state of the bottom navigation bar.
 */
val LocalBottomNavigationVisible = compositionLocalOf { false }

/**
 * CompositionLocal providing the current visibility state of the navigation rail.
 */
val LocalNavigationRailVisible = compositionLocalOf { false }

/**
 * Controller for selection mode state in the home flow.
 * Used to hide the mini player when any screen is in selection mode.
 *
 * @param isSelectionModeActive Read-only state indicating if selection mode is active
 * @param onSelectionModeChanged Event sink to report selection mode changes
 */
data class SelectionModeController(
    val isSelectionModeActive: Boolean,
    val onSelectionModeChanged: (Boolean) -> Unit,
)

/**
 * CompositionLocal providing [SelectionModeController] for the home flow.
 * Screens in selection mode should call [SelectionModeController.onSelectionModeChanged] to report their state.
 */
val LocalSelectionModeController = compositionLocalOf {
    SelectionModeController(
        isSelectionModeActive = false,
        onSelectionModeChanged = {},
    )
}
