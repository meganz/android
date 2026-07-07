package mega.privacy.android.feature.settings.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.SortingAndViewModeSettingsNavKey

fun EntryProviderScope<NavKey>.sortingAndViewModeSettingsScreen(
    navigationHandler: NavigationHandler,
) {
    entry<SortingAndViewModeSettingsNavKey> {
        // TODO: render SortingAndViewModeSettingsView (added in the screen MR)
    }
}
