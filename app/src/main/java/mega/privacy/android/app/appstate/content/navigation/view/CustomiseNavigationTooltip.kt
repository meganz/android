package mega.privacy.android.app.appstate.content.navigation.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import mega.android.core.ui.components.tooltip.direction.TooltipDirection
import mega.android.core.ui.components.tooltip.popup.interactive.InteractiveBottomDirectionTooltipPopup
import mega.android.core.ui.components.tooltip.popup.interactive.InteractiveTooltipButtonProperties
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.appstate.content.navigation.CustomiseNavigationTooltipViewModel
import mega.privacy.android.navigation.contract.state.LocalBottomNavigationVisible
import mega.privacy.android.navigation.contract.state.LocalNavigationRailVisible
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.CustomiseNavigationTooltipDismissButtonPressedEvent
import mega.privacy.mobile.analytics.event.CustomiseNavigationTooltipDisplayedEvent
import mega.privacy.mobile.analytics.event.CustomiseNavigationTooltipExploreButtonPressedEvent

/**
 * Zero-height anchor placed directly above the bottom navigation bar that hosts the one-time
 * "Customise navigation" onboarding tooltip pointing down at the bar.
 *
 * @param isOnStartDestination whether the landing (start) screen is currently displayed
 * @param onExplore invoked when the tooltip's Explore action is clicked, after the tooltip
 * has been dismissed
 */
@Composable
internal fun CustomiseNavigationTooltipAnchor(
    isOnStartDestination: Boolean,
    onExplore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<CustomiseNavigationTooltipViewModel>()
    val showTooltip by viewModel.uiState.collectAsStateWithLifecycle()
    var anchorCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { anchorCoordinates = it }
    )
    CustomiseNavigationTooltip(
        showTooltip = showTooltip && isOnStartDestination,
        anchorCoordinates = anchorCoordinates,
        onDisplayed = {
            Analytics.tracker.trackEvent(CustomiseNavigationTooltipDisplayedEvent)
            viewModel.onTooltipDisplayed()
        },
        onDismiss = {
            Analytics.tracker.trackEvent(CustomiseNavigationTooltipDismissButtonPressedEvent)
            viewModel.onTooltipDismissed()
        },
        onExplore = {
            Analytics.tracker.trackEvent(CustomiseNavigationTooltipExploreButtonPressedEvent)
            viewModel.onTooltipDismissed()
            onExplore()
        },
    )
}

/**
 * Renders the "Customise navigation" tooltip above [anchorCoordinates].
 *
 * The tooltip uses [LayoutCoordinates] captured via `onGloballyPositioned`, which can become
 * stale or detached during state transitions. The visibility is guarded by:
 *  1. [LocalBottomNavigationVisible] and [LocalNavigationRailVisible] — the tooltip only makes
 *  sense while the bottom navigation bar it points at is visible
 *  2. [LayoutCoordinates.isAttached] — the captured coordinates must still be valid
 *  3. Lifecycle state — the screen must be resumed (popup is a separate window)
 *
 * @param onDisplayed invoked the moment the tooltip is displayed
 */
@Composable
internal fun CustomiseNavigationTooltip(
    showTooltip: Boolean,
    anchorCoordinates: LayoutCoordinates?,
    onDisplayed: () -> Unit,
    onDismiss: () -> Unit,
    onExplore: () -> Unit,
) {
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val isVisible = showTooltip
            && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
            && LocalBottomNavigationVisible.current
            && !LocalNavigationRailVisible.current
    if (isVisible) {
        anchorCoordinates?.takeIf { it.isAttached }?.let { coordinates ->
            LaunchedEffect(Unit) { onDisplayed() }
            InteractiveBottomDirectionTooltipPopup(
                modifier = Modifier
                    .testTag(CUSTOMISE_NAVIGATION_TOOLTIP_TAG)
                    .widthIn(max = 280.dp),
                direction = TooltipDirection.Bottom.Centre,
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                ),
                title = stringResource(sharedR.string.settings_customise_navigation_title),
                body = stringResource(sharedR.string.settings_customise_navigation_tooltip_body),
                primaryButton = InteractiveTooltipButtonProperties(
                    text = stringResource(sharedR.string.settings_customise_navigation_tooltip_action),
                    onClick = onExplore,
                ),
                needCloseIcon = true,
                needDivider = true,
                anchorViewCoordinates = coordinates,
                onDismissRequest = onDismiss,
            )
        }
    }
}

internal const val CUSTOMISE_NAVIGATION_TOOLTIP_TAG = "home_screens:customise_navigation_tooltip"
