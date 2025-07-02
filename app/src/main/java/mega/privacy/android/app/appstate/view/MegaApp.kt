package mega.privacy.android.app.appstate.view

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import mega.privacy.android.app.appstate.model.AppState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MegaApp(
    navController: NavHostController,
    appState: AppState.Data,
) {
    NavHost(
        navController = navController,
        startDestination = MainNavigationScaffoldDestination::class
    ) {
        val navigationHandler = NavigationHandlerImpl(navController)

        mainNavigationScaffold(
            topLevelDestinations = appState.mainNavItems,
            startDestination = appState.initialMainDestination,
            builder = {
                appState.mainNavScreens.forEach {
                    it(this, navigationHandler)
                }
            }
        )
        appState.featureDestinations
            .forEach {
                it.navigationGraph(this, navigationHandler)
            }
    }
}