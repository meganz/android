package mega.privacy.android.app.appstate.content.navigation.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import androidx.navigation.navigation
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.app.R
import mega.privacy.android.app.appstate.content.navigation.MainNavigationStateViewModel
import mega.privacy.android.app.appstate.content.navigation.model.MainNavState
import mega.privacy.android.app.main.ads.NewAdsContainer
import mega.privacy.android.app.presentation.login.view.MEGA_LOGO_TEST_TAG
import mega.privacy.android.app.presentation.psa.PsaContainer
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.mobile.navigation.snowflake.MainNavigationScaffold
import kotlin.reflect.KClass

@Serializable
data object MainNavigationScaffoldDestination : NavKey

@Serializable
data object MainGraph : NavKey


@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.mainNavigationScaffold(
    modifier: Modifier = Modifier,
    transferHandler: TransferHandler,
    navigationHandler: NavigationHandler,
) {
    composable<MainNavigationScaffoldDestination> {
        val navController = rememberNavController()

        val viewModel = hiltViewModel<MainNavigationStateViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        val currentDestination = navController.currentBackStackEntryAsState().value?.destination
        when (val currentState = state) {

            MainNavState.Loading -> {
                Box(modifier = modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_splash_logo),
                        contentDescription = stringResource(id = R.string.login_to_mega),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(288.dp)
                            .testTag(MEGA_LOGO_TEST_TAG),
                        contentScale = ContentScale.FillBounds
                    )
                }
            }

            is MainNavState.Data -> {
                NewAdsContainer(
                    modifier = modifier.fillMaxSize(),
                ) { modifier ->
                    MainNavigationScaffold(
                        modifier = modifier
                            .fillMaxWidth()
                            .weight(1f),
                        mainNavItems = currentState.mainNavItems,
                        onDestinationClick = { destination ->
                            navController.navigate(destination, navOptions {
                                popUpTo(MainGraph) {
                                    inclusive = false
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
                            PsaContainer {
                                NavHost(
                                    modifier = Modifier
                                        .consumeWindowInsets(WindowInsets.navigationBars)
                                        .fillMaxSize(),
                                    navController = navController,
                                    startDestination = MainGraph,
                                    builder = {
                                        navigation<MainGraph>(
                                            startDestination = currentState.initialDestination
                                        ) {
                                            currentState.mainNavScreens.forEach {
                                                it(
                                                    navigationHandler,
                                                    navigationUiController,
                                                    transferHandler
                                                )
                                            }
                                        }
                                    },
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

private fun NavDestination?.isTopLevelDestinationInHierarchy(route: KClass<*>) =
    this?.hierarchy?.any {
        it.hasRoute(route)
    } == true