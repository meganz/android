package mega.privacy.android.app.appstate.view

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.MainNavItem
import mega.privacy.mobile.navigation.snowflake.MainNavigationScaffold
import kotlin.reflect.KClass

@Serializable
object MainNavigationScaffoldDestination

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.mainNavigationScaffold(
    topLevelDestinations: ImmutableSet<MainNavItem>,
    startDestination: KClass<*>,
    onDestinationClick: (NavHostController, MainNavItem) -> Unit,
    builder: NavGraphBuilder.() -> Unit,
) {
    composable<MainNavigationScaffoldDestination> {
        MainNavigationScaffold(
            mainNavItems = topLevelDestinations,
            startDestination = startDestination,
            onDestinationClick = onDestinationClick,
            builder = builder,
        )
    }
}