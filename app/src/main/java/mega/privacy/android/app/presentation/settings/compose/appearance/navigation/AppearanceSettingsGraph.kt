package mega.privacy.android.app.presentation.settings.compose.appearance.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import mega.privacy.android.app.presentation.settings.compose.appearance.MediaDiscoverySettings
import mega.privacy.android.app.presentation.settings.compose.appearance.appearanceSettings
import mega.privacy.android.app.presentation.settings.compose.appearance.mediaDiscoverySettings

/**
 * Appearance settings graph
 */
fun NavGraphBuilder.appearanceSettingsGraph(
    navController: NavHostController,
) {
    appearanceSettings { navController.navigate(MediaDiscoverySettings) }
    mediaDiscoverySettings()
}