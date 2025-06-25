package mega.privacy.android.app.appstate.view

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import mega.privacy.android.app.appstate.model.AppState
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MegaApp(
    navController: NavHostController,
    passcodeCryptObjectFactory: PasscodeCryptObjectFactory,
    appState: AppState.Data,
) {
    OriginalTheme(isDark = appState.themeMode.isDarkMode()) {
        NavHost(
            navController = navController,
            startDestination = MainNavigationScaffoldDestination::class
        ) {
            val navigationHandler = NavigationHandlerImpl(navController)

            mainNavigationScaffold(
                topLevelDestinations = appState.mainNavItems,
                startDestination = appState.initialMainDestination,
                builder = {
                    appState.mainNavItems.forEach {
                        it.navItem.screen(this, navigationHandler)
                    }
                }
            )
            appState.featureDestinations
                .forEach {
                    it.navigationGraph(this, navigationHandler)
                }
        }
    }

}