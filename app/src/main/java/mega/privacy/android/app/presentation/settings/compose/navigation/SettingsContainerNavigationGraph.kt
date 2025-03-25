package mega.privacy.android.app.presentation.settings.compose.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import mega.privacy.android.app.presentation.settings.compose.home.emptySettings

/**
 * Settings graph
 *
 */
fun NavGraphBuilder.settingsGraph(
    navController: NavHostController,
    featureSettings: List<NavGraphBuilder.(NavHostController) -> Unit>,
) {
    emptySettings(navController)
    featureSettings.forEach {
        it(this, navController)
    }
}