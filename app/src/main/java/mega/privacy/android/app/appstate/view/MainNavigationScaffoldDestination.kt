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
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.serialization.Serializable
import mega.privacy.mobile.navigation.snowflake.MainNavigationScaffold
import mega.privacy.mobile.navigation.snowflake.model.NavigationItem
import kotlin.reflect.KClass

@Serializable
object MainNavigationScaffoldDestination

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.mainNavigationScaffold(
    modifier: Modifier = Modifier,
    topLevelDestinations: ImmutableSet<NavigationItem>,
    startDestination: Any,
    builder: NavGraphBuilder.() -> Unit,
) {
    composable<MainNavigationScaffoldDestination> {
        val navController = rememberNavController()
        val currentDestination = navController.currentBackStackEntryAsState().value?.destination
        MainNavigationScaffold(
            mainNavItems = topLevelDestinations,
            onDestinationClick = { mainNavItem ->
                navController.navigate(mainNavItem.destination)
            },
            isSelected = { navItem ->
                currentDestination?.isTopLevelDestinationInHierarchy(navItem.destination::class) == true
            },
        ) {
            NavHost(
                modifier = modifier
                    .fillMaxSize(),
                navController = navController,
                startDestination = startDestination,
                builder = builder,
            )
        }
    }
}

private fun NavDestination?.isTopLevelDestinationInHierarchy(route: KClass<*>) =
    this?.hierarchy?.any {
        it.hasRoute(route)
    } == true