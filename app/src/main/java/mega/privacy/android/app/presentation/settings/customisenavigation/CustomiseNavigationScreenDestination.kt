package mega.privacy.android.app.presentation.settings.customisenavigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import mega.privacy.android.analytics.decorator.withScreenViewEvent
import mega.privacy.android.app.presentation.settings.customisenavigation.model.CustomiseNavigationUiState
import mega.privacy.android.app.presentation.settings.model.customiseNavigationTargetPreference
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.metadata.buildMetadata
import mega.privacy.android.navigation.destination.CustomiseNavigationNavKey
import mega.privacy.android.navigation.destination.LegacySettingsNavKey
import mega.privacy.mobile.analytics.event.CustomiseNavigationScreenEvent

/**
 * Feature destination exposing the Customise navigation settings screen.
 */
class CustomiseNavigationFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, _ ->
            customiseNavigationScreen(navigationHandler = navigationHandler)
        }
}

internal fun EntryProviderScope<NavKey>.customiseNavigationScreen(
    navigationHandler: NavigationHandler,
) {
    entry<CustomiseNavigationNavKey>(
        metadata = buildMetadata {
            withScreenViewEvent(CustomiseNavigationScreenEvent)
        }
    ) { key ->
        val viewModel = hiltViewModel<CustomiseNavigationViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        (uiState as? CustomiseNavigationUiState.Data)?.let { data ->
            EventEffect(
                event = data.savedEvent,
                onConsumed = viewModel::onSavedEventConsumed,
            ) {
                navigationHandler.exitCustomiseNavigation(key)
            }
        }

        BackHandler(enabled = key.returnToSettings) {
            navigationHandler.exitCustomiseNavigation(key)
        }

        CustomiseNavigationScreen(
            state = uiState,
            onBackPressed = { navigationHandler.exitCustomiseNavigation(key) },
            onSave = viewModel::save,
        )
    }
}

internal fun NavigationHandler.exitCustomiseNavigation(key: CustomiseNavigationNavKey) {
    if (key.returnToSettings) {
        navigateAndClearTo(
            destination = LegacySettingsNavKey(customiseNavigationTargetPreference),
            newParent = key,
            inclusive = true,
        )
    } else {
        back()
    }
}
