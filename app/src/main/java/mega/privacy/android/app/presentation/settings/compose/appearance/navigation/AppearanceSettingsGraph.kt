package mega.privacy.android.app.presentation.settings.compose.appearance.navigation

import androidx.navigation.NavGraphBuilder
import mega.privacy.android.app.presentation.settings.compose.appearance.MediaDiscoverySettings
import mega.privacy.android.app.presentation.settings.compose.appearance.appearanceSettings
import mega.privacy.android.app.presentation.settings.compose.appearance.mediaDiscoverySettings
import mega.privacy.android.navigation.contract.NavigationHandler

/**
 * Appearance settings graph
 */
fun NavGraphBuilder.appearanceSettingsGraph(
    navigationHandler: NavigationHandler,
) {
    appearanceSettings { navigationHandler.navigate(MediaDiscoverySettings) }
    mediaDiscoverySettings()
}