package mega.privacy.android.app.appstate.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import androidx.navigation3.runtime.NavKey
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.NavigationUiController
import mega.privacy.mobile.navigation.snowflake.MainNavigationScaffold
import mega.privacy.mobile.navigation.snowflake.model.NavigationItem
import kotlin.reflect.KClass

@Serializable
data object MainNavigationScaffoldDestination : NavKey

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.mainNavigationScaffold(
    modifier: Modifier = Modifier,
    topLevelDestinations: ImmutableSet<NavigationItem>,
    startDestination: Any,
    builder: NavGraphBuilder.(NavigationUiController) -> Unit,
) {
    composable<MainNavigationScaffoldDestination> {
        val navController = rememberNavController()
        val currentDestination = navController.currentBackStackEntryAsState().value?.destination
        MainNavigationScaffold(
            mainNavItems = topLevelDestinations,
            onDestinationClick = { destination ->
                navController.navigate(destination, navOptions {
                    popUpTo(navController.graph.id) {
                        inclusive = true
                        saveState = true
                    }

                    launchSingleTop = true

                    restoreState = true
                })
            },
            isSelected = { destination ->
                currentDestination?.isTopLevelDestinationInHierarchy(destination::class) == true
            },
            navContent = { navigationUiController ->
                NavHost(
                    modifier = modifier
                        .fillMaxSize(),
                    navController = navController,
                    startDestination = startDestination,
                    builder = { builder(navigationUiController) },
                )
            },
        )
    }
}

private fun NavDestination?.isTopLevelDestinationInHierarchy(route: KClass<*>) =
    this?.hierarchy?.any {
        it.hasRoute(route)
    } == true