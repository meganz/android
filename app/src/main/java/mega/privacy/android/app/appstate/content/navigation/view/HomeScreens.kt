package mega.privacy.android.app.appstate.content.navigation.view

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.extensions.LaunchedOnceEffect
import mega.privacy.android.analytics.decorator.rememberAnalyticNavEntryDecorator
import mega.privacy.android.app.appstate.content.navigation.MainNavigationStateViewModel
import mega.privacy.android.app.appstate.content.navigation.StorageStatusViewModel
import mega.privacy.android.app.appstate.content.navigation.TopLevelBackStackNavigationHandler
import mega.privacy.android.app.appstate.content.navigation.model.MainNavState
import mega.privacy.android.app.appstate.content.navigation.rememberTopLevelBackStack
import mega.privacy.android.app.main.ads.NewAdsContainer
import mega.privacy.android.app.presentation.search.view.MiniAudioPlayerView
import mega.privacy.android.core.sharedcomponents.requeststatus.RequestStatusProgressContainer
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.navkey.MainNavItemNavKey
import mega.privacy.android.navigation.contract.shared.LocalSharedViewModelStoreOwner
import mega.privacy.android.navigation.contract.state.LocalNavigationRailVisible
import mega.privacy.android.navigation.contract.state.LocalSelectionModeController
import mega.privacy.android.navigation.contract.state.SelectionModeController
import mega.privacy.android.navigation.contract.transition.fadeTransition
import mega.privacy.android.navigation.destination.HomeScreensNavKey
import mega.privacy.android.navigation.destination.OverQuotaDialogNavKey
import mega.privacy.android.navigation.destination.WhatsNewNavKey
import mega.privacy.android.shared.original.core.ui.theme.extensions.conditional
import mega.privacy.mobile.home.presentation.home.Home
import mega.privacy.mobile.home.presentation.whatsnew.WhatsNewViewModel
import mega.privacy.mobile.navigation.snowflake.MainNavigationScaffold
import kotlin.time.Duration.Companion.seconds

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
    var isNetworkChangeHandled by rememberSaveable { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }

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

    val snackbarHostState = LocalSnackBarHostState.current

    when (val currentState = state) {
        MainNavState.Loading -> {
            // add shimmer effect later
        }

        is MainNavState.Data -> {
            var hasSetStackFromKey by rememberSaveable(key) { mutableStateOf(false) }
            val homeScreenStacks =
                rememberTopLevelBackStack(currentState.initialDestination)

            if (hasSetStackFromKey.not()) {
                key.root?.let {
                    homeScreenStacks.switchTopLevel(it)
                    key.destinations?.let { destinations -> homeScreenStacks.addAll(destinations) }
                }
                hasSetStackFromKey = true
            }

            LaunchedEffect(currentState.isConnected) {
                if (!currentState.isConnected) {
                    delay(1.seconds)
                    if (!isNetworkChangeHandled) {
                        homeScreenStacks.switchTopLevel(Home)
                        isSelectionMode = false
                        isNetworkChangeHandled = true
                    }
                } else {
                    isNetworkChangeHandled = false
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
                        snackbarHostState?.currentSnackbarData?.dismiss()
                        if (destination == homeScreenStacks.topLevelKey) {
                            homeScreenStacks.replaceStack()
                        } else {
                            homeScreenStacks.switchTopLevel(destination)
                        }
                    },
                    isSelected = { destination ->
                        homeScreenStacks.topLevelKey::class == destination::class
                    },
                    navContent = { navigationUiController ->
                        val homeScreensOwner = LocalViewModelStoreOwner.current
                        LaunchedEffect(isSelectionMode) {
                            navigationUiController.showNavigation(!isSelectionMode)
                        }
                        Column(Modifier.fillMaxSize()) {
                            val selectionModeController = remember(isSelectionMode) {
                                SelectionModeController(
                                    isSelectionModeActive = isSelectionMode,
                                    onSelectionModeChanged = {
                                        isSelectionMode = it
                                    },
                                )
                            }
                            CompositionLocalProvider(
                                LocalSelectionModeController provides selectionModeController,
                                LocalSharedViewModelStoreOwner provides homeScreensOwner,
                            ) {
                                NavDisplay(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    backStack = homeScreenStacks.backStack,
                                    onBack = { homeScreenStacks.removeLast() },
                                    entryDecorators = listOf(
                                        rememberSaveableStateHolderNavEntryDecorator(),
                                        rememberViewModelStoreNavEntryDecorator(),
                                        rememberAnalyticNavEntryDecorator()
                                    ),
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
                                    transitionSpec = { fadeTransition },
                                    popTransitionSpec = { fadeTransition },
                                    predictivePopTransitionSpec = { fadeTransition }
                                )
                                var isMiniPlayerVisible by remember { mutableStateOf(false) }
                                Box(
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    if (!isSelectionMode) {
                                        MiniAudioPlayerView(
                                            modifier = Modifier
                                                .fillMaxWidth(),
                                            onVisibilityChanged = { isVisible ->
                                                isMiniPlayerVisible = isVisible
                                            }
                                        )
                                    }
                                    RequestStatusProgressContainer(
                                        viewModel = hiltViewModel(LocalActivity.current as ComponentActivity),
                                        modifier = Modifier
                                            .conditional(
                                                (!isMiniPlayerVisible || isSelectionMode) &&
                                                        LocalNavigationRailVisible.current
                                            ) {
                                                navigationBarsPadding()
                                            }
                                    )
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

@Serializable
private class FallbackKey : NavKey

private fun fallback(
    unknownKey: NavKey,
    outerNavigationHandler: NavigationHandler,
    innerNavigationHandler: NavigationHandler,
) = NavEntry<NavKey>(
    key = FallbackKey(),
) {
    LaunchedOnceEffect {
        if (unknownKey !is MainNavItemNavKey) {
            outerNavigationHandler.navigate(unknownKey)
        }
        innerNavigationHandler.back()
    }
}
