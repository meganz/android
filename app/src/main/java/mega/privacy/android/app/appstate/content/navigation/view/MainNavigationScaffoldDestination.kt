package mega.privacy.android.app.appstate.content.navigation.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import mega.privacy.android.app.R
import mega.privacy.android.app.appstate.content.navigation.MainNavigationStateViewModel
import mega.privacy.android.app.appstate.content.navigation.model.MainNavState
import mega.privacy.android.app.main.ads.NewAdsContainer
import mega.privacy.android.app.presentation.login.view.MEGA_LOGO_TEST_TAG
import mega.privacy.android.app.presentation.psa.PsaContainer
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.mobile.navigation.snowflake.IndicatorDot
import mega.privacy.mobile.navigation.snowflake.MainNavigationScaffold
import timber.log.Timber

@Serializable
data object MainNavigationScaffoldDestination : NavKey

@Serializable
data object MainGraph : NavKey


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationScaffold(
    transferHandler: TransferHandler,
    navigationHandler: NavigationHandler,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<MainNavigationStateViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

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
            val backStack = rememberNavBackStack(currentState.initialDestination)
            val currentDestination = backStack.lastOrNull()
            NewAdsContainer(
                modifier = modifier.fillMaxSize(),
            ) { modifier ->
                MainNavigationScaffold(
                    modifier = modifier
                        .fillMaxWidth()
                        .weight(1f),
                    mainNavItems = currentState.mainNavItems,
                    onDestinationClick = { destination ->
                        if (destination != currentDestination) {
                            if (backStack.size > 1) {
                                // keep only the first initial destination and remove others
                                backStack.removeLastOrNull()
                            }
                            if (destination != backStack.firstOrNull()) {
                                backStack.add(destination)
                            }
                        }
                    },
                    isSelected = { destination ->
                        currentDestination == destination
                    },
                    mainNavItemBadge = { IndicatorDot() },
                    navContent = { navigationUiController ->
                        PsaContainer {
                            NavDisplay(
                                modifier = Modifier.fillMaxSize(),
                                backStack = backStack,
                                onBack = { backStack.removeLastOrNull() },
                                entryDecorators = listOf(
                                    rememberSaveableStateHolderNavEntryDecorator(),
                                    rememberViewModelStoreNavEntryDecorator()
                                ),
                                entryProvider = entryProvider {
                                    currentState.mainNavScreens.forEach { screenProvider ->
                                        screenProvider(
                                            navigationHandler,
                                            navigationUiController,
                                            transferHandler
                                        )
                                    }
                                }
                            )
                        }
                    },
                )
            }
        }
    }
}