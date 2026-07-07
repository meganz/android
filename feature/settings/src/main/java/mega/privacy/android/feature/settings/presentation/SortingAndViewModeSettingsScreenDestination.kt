package mega.privacy.android.feature.settings.presentation

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.feature.settings.presentation.view.SortingAndViewModeSettingsView
import mega.privacy.android.navigation.destination.SortingAndViewModeSettingsNavKey

fun EntryProviderScope<NavKey>.sortingAndViewModeSettingsDestination(
    onNavigateBack: (NavKey) -> Unit,
) {
    entry<SortingAndViewModeSettingsNavKey> { key ->
        val viewModel = hiltViewModel<SortingAndViewModeSettingsViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        SortingAndViewModeSettingsView(
            uiState = uiState,
            onSetSortingPreference = viewModel::setSortingPreference,
            onSetViewModePreference = viewModel::setViewModePreference,
            onNavigateBack = { onNavigateBack(key) },
        )
    }
}
