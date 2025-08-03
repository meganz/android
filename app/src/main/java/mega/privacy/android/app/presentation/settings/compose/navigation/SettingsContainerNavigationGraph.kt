package mega.privacy.android.app.presentation.settings.compose.navigation

import androidx.navigation.NavGraphBuilder
import mega.privacy.android.app.presentation.settings.compose.home.emptySettings
import mega.privacy.android.navigation.contract.NavigationHandler

/**
 * Settings graph
 *
 */
fun NavGraphBuilder.settingsGraph(
    navigationHandler: NavigationHandler,
    featureSettings: List<NavGraphBuilder.(NavigationHandler) -> Unit>,
) {
    emptySettings(navigationHandler)
    featureSettings.forEach {
        it(this, navigationHandler)
    }
}