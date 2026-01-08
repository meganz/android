package mega.privacy.android.app.appstate.content.navigation.view

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import mega.privacy.android.app.appstate.content.navigation.MainNavigationStateViewModel
import mega.privacy.android.app.appstate.content.navigation.StorageStatusViewModel
import mega.privacy.android.app.appstate.content.navigation.TopLevelBackStackNavigationHandler
import mega.privacy.android.app.appstate.content.navigation.model.MainNavState
import mega.privacy.android.app.appstate.content.navigation.rememberTopLevelBackStack
import mega.privacy.android.app.main.ads.NewAdsContainer
import mega.privacy.android.app.presentation.search.view.MiniAudioPlayerView
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.transition.OrientationAwareSlideTransitionSceneStrategy
import mega.privacy.android.navigation.contract.transition.fadeTransition
import mega.privacy.android.navigation.destination.HomeScreensNavKey
import mega.privacy.android.navigation.destination.OverQuotaDialogNavKey
import mega.privacy.mobile.navigation.snowflake.MainNavigationScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreens(
    transferHandler: TransferHandler,
    outerNavigationHandler: NavigationHandler,
    key: HomeScreensNavKey,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<MainNavigationStateViewModel>()
    val storageStateViewModel = hiltViewModel<StorageStatusViewModel>()
    val storageUiState by storageStateViewModel.state.collectAsStateWithLifecycle()
    var handledStorageState by rememberSaveable { mutableStateOf(StorageState.Unknown) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val localConfiguration = LocalConfiguration.current
    val isLandscapeMode = localConfiguration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val slideTransitionSceneStrategy = remember(isLandscapeMode) {
        OrientationAwareSlideTransitionSceneStrategy<NavKey>(isLandscapeMode)
    }

    LaunchedEffect(storageUiState.storageState) {
        if (storageUiState.storageState == StorageState.Red
            || storageUiState.storageState == StorageState.Orange
        ) {
            if (storageUiState.storageState.ordinal > handledStorageState.ordinal) {
                outerNavigationHandler.navigate(
                    OverQuotaDialogNavKey(
                        isOverQuota = storageUiState.storageState == StorageState.Red,
                        overQuotaAlert = false
                    )
                )
                handledStorageState = storageUiState.storageState
            }
        }
        handledStorageState == storageUiState.storageState
    }

    when (val currentState = state) {
        MainNavState.Loading -> {
            // add shimmer effect later
        }

        is MainNavState.Data -> {
            val homeScreenStacks = rememberTopLevelBackStack(currentState.initialDestination)
            LaunchedEffect(key) {
                key.root?.let {
                    homeScreenStacks.switchTopLevel(it)
                    key.destinations?.let { destinations -> homeScreenStacks.addAll(destinations) }
                }
            }

            val innerNavigationHandler = remember {
                TopLevelBackStackNavigationHandler(
                    backStack = homeScreenStacks,
                    navigationResultManager = viewModel.navigationResultManager
                )
            }
            NewAdsContainer(
                modifier = modifier.fillMaxSize(),
            ) { modifier ->
                MainNavigationScaffold(
                    modifier = modifier
                        .fillMaxWidth()
                        .weight(1f),
                    mainNavItems = currentState.mainNavItems,
                    onDestinationClick = { destination ->
                        homeScreenStacks.switchTopLevel(destination)
                    },
                    isSelected = { destination ->
                        homeScreenStacks.topLevelKey::class == destination::class
                    },
                    navContent = { navigationUiController ->
                        Column(Modifier.fillMaxSize()) {
                            NavDisplay(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                backStack = homeScreenStacks.backStack,
                                onBack = { homeScreenStacks.removeLast() },
                                entryDecorators = listOf(
                                    rememberSaveableStateHolderNavEntryDecorator(),
                                    rememberViewModelStoreNavEntryDecorator()
                                ),
                                sceneStrategy = slideTransitionSceneStrategy,
                                entryProvider = entryProvider({
                                    fallback(
                                        unknownKey = it,
                                        outerNavigationHandler = outerNavigationHandler,
                                        innerNavigationHandler = innerNavigationHandler
                                    )
                                }) {
                                    currentState.mainNavScreens.forEach { screenProvider ->
                                        screenProvider(
                                            innerNavigationHandler,
                                            navigationUiController,
                                            transferHandler
                                        )
                                    }
                                },
                                predictivePopTransitionSpec = { fadeTransition }
                            )
                            MiniAudioPlayerView(
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                        }
                    },
                )
            }
        }
    }
}

@Serializable
private data object FallbackKey : NavKey

private fun fallback(
    unknownKey: NavKey,
    outerNavigationHandler: NavigationHandler,
    innerNavigationHandler: NavigationHandler,
) = NavEntry<NavKey>(
    key = FallbackKey
) {
    LaunchedEffect(unknownKey) {
        outerNavigationHandler.navigate(unknownKey)
        innerNavigationHandler.back()
    }
}
