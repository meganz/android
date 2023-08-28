package mega.privacy.android.app.presentation.offline.view

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable

/**
 * Route for [OfflineRoute]
 */
internal const val offlineRoute = "offline/main"

/**
 * Composable destination for [OfflineRoute]
 */
fun NavGraphBuilder.offlineScreen() {
    composable(offlineRoute) {
        OfflineRoute()
    }
}

/**
 * Navigation for [OfflineRoute]
 */
fun NavController.navigateToOffline(navOptions: NavOptions? = null) {
    this.navigate(navOptions = navOptions, route = offlineRoute)
}