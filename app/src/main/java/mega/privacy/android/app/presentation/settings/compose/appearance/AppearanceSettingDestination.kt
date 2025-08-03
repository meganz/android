package mega.privacy.android.app.presentation.settings.compose.appearance

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.settings.compose.appearance.view.AppearanceSettingsScreen

@Serializable
data object AppearanceSettings : NavKey

fun NavGraphBuilder.appearanceSettings(
    onNavigateToMediaDiscovery: () -> Unit,
) {
    composable<AppearanceSettings> { backStackEntry ->
        val viewModel = hiltViewModel<AppearanceSettingsViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        AppearanceSettingsScreen(
            state = state,
            onNavigateToMediaDiscovery = onNavigateToMediaDiscovery,
            onShowHiddenItemsToggled = viewModel::onShowHiddenItemsToggled,
            onThemeModeSelected = viewModel::onThemeModeSelected,
        )
    }
}

