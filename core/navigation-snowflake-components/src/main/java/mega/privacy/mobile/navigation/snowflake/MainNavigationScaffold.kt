package mega.privacy.mobile.navigation.snowflake

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuite
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.collections.immutable.ImmutableSet
import mega.android.core.ui.components.badge.Badge
import mega.android.core.ui.components.badge.BadgeType
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.navigation.contract.MainNavItem
import kotlin.reflect.KClass


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationScaffold(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    mainNavItems: ImmutableSet<MainNavItem>,
    startDestination: KClass<*>,
    onDestinationClick: (NavHostController, MainNavItem) -> Unit,
    builder: NavGraphBuilder.() -> Unit,
) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination

    val scaffoldColors = NavigationScaffoldColors.scaffoldColors()
    val itemColors = NavigationScaffoldColors.itemColors()
    val navSuiteType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
    val borderColor = DSTokens.colors.border.subtle
    NavigationSuiteScaffoldLayout(
        navigationSuite = {
            NavigationSuite(
                colors = scaffoldColors,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .drawBehind {
                        val strokeWidth = 2.dp.toPx()
                        if (navSuiteType == NavigationSuiteType.NavigationBar) {
                            drawLine(
                                color = borderColor,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = strokeWidth
                            )
                        } else {
                            drawLine(
                                color = borderColor,
                                start = Offset(size.width, 0f),
                                end = Offset(size.width, size.height),
                                strokeWidth = strokeWidth
                            )
                        }
                    }
            ) {
                mainNavItems.forEach { navItem ->
                    item(
                        icon = {
                            Icon(
                                modifier = Modifier.size(32.dp),
                                painter = painterResource(navItem.iconRes),
                                contentDescription = navItem.label
                            )
                        },
                        badge = navItem.badge
                            ?.let {
                                {
                                    val badgeText = it.collectAsState(initial = null)
                                    badgeText.value?.let { text ->
                                        Badge(
                                            modifier = Modifier,
                                            badgeType = BadgeType.Mega,
                                            text = text
                                        )
                                    }
                                }
                            },
                        label = { Text(text = navItem.label) },
                        selected = currentDestination.isTopLevelDestinationInHierarchy(navItem.destinationClass),
                        onClick = { onDestinationClick(navController, navItem) },
                        colors = itemColors,
                    )
                }
            }

        },
        layoutType = navSuiteType,
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

private fun NavDestination?.isTopLevelDestinationInHierarchy(route: KClass<*>) =
    this?.hierarchy?.any {
        it.hasRoute(route)
    } == true