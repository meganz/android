package mega.privacy.mobile.home.presentation.home

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import mega.android.core.ui.components.tooltip.direction.TooltipDirection
import mega.android.core.ui.components.tooltip.popup.interactive.InteractiveTooltipButtonProperties
import mega.android.core.ui.components.tooltip.popup.interactive.InteractiveTopDirectionTooltipPopup
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.home.presentation.home.model.HomeUiState

/**
 * Renders the tooltip that points to the customization icon in the [HomeScreen] top bar.
 *
 * The tooltip uses [LayoutCoordinates] captured via `onGloballyPositioned`, which can
 * become stale or detached during state transitions. The visibility is guarded by:
 *  1. [HomeUiState.Data.isHomeCustomizationEnabled] — the anchor icon must actually be present
 *  2. [LayoutCoordinates.isAttached] — the captured coordinates must still be valid
 *  3. Lifecycle state — the screen must be resumed (popup is a separate window)
 *
 */
@Composable
internal fun HomeConfigurationTooltip(
    state: HomeUiState,
    iconCoordinates: LayoutCoordinates?,
    onDismiss: () -> Unit,
    onNavigateToConfiguration: () -> Unit,
) {
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val isHomeResumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val showTooltip = isHomeResumed
            && state is HomeUiState.Data
            && state.isHomeCustomizationEnabled
            && state.showHomeConfigurationTooltip
    if (!showTooltip) return

    iconCoordinates?.takeIf { it.isAttached }?.let { coordinates ->
        InteractiveTopDirectionTooltipPopup(
            modifier = Modifier
                .testTag(HOME_CONFIGURATION_TOOLTIP_TAG)
                .widthIn(max = 280.dp),
            direction = TooltipDirection.Top.Right,
            properties = PopupProperties(
                focusable = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
            title = stringResource(sharedR.string.home_configuration_screen_toolbar_title),
            body = stringResource(sharedR.string.home_configuration_tooltip_description),
            primaryButton = InteractiveTooltipButtonProperties(
                text = stringResource(sharedR.string.home_configuration_tooltip_action),
                onClick = onNavigateToConfiguration,
            ),
            needCloseIcon = true,
            needDivider = true,
            anchorViewCoordinates = coordinates,
            onDismissRequest = onDismiss,
        )
    }
}

internal const val HOME_CONFIGURATION_TOOLTIP_TAG = "home_screen:configuration_tooltip"
