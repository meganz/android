package mega.privacy.android.app.appstate.content.view

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigation3.ui.NavDisplay
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.appstate.content.AppContentStateViewModel
import mega.privacy.android.app.appstate.content.model.AppContentState
import mega.privacy.android.app.appstate.content.navigation.NavigationHandlerImpl
import mega.privacy.android.app.appstate.content.navigation.view.HomeScreens
import mega.privacy.android.app.appstate.content.navigation.view.HomeScreensNavKey
import mega.privacy.android.app.appstate.content.navigation.view.PermissionScreensNavKey
import mega.privacy.android.app.appstate.content.transfer.AppTransferViewModel
import mega.privacy.android.app.appstate.content.transfer.TransferHandlerImpl
import mega.privacy.android.app.appstate.global.event.AppDialogViewModel
import mega.privacy.android.app.presentation.login.LoginInProgressViewModel
import mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponent
import mega.privacy.android.navigation.contract.bottomsheet.BottomSheetSceneStrategy
import mega.privacy.android.navigation.contract.transparent.TransparentSceneStrategy

@Composable
internal fun AppContentView(
    viewModel: AppContentStateViewModel,
    snackbarHostState: SnackbarHostState,
    navKey: MegaActivity.LoggedInScreens,
) {
    val backStack = rememberNavBackStack(HomeScreensNavKey(null))
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        when (val appState = state) {
            is AppContentState.Data -> {
                if (appState.onboardingPermissionsCheckResult?.requestPermissionsOnFirstLaunch == true) {
                    val permissionScreensNavKey = PermissionScreensNavKey(
                        onlyShowNotificationPermission = appState.onboardingPermissionsCheckResult.onlyShowNotificationPermission
                    )
                    backStack.add(permissionScreensNavKey)
                }
            }

            else -> {
            }
        }
    }
    val navigationHandler = remember { NavigationHandlerImpl(backStack) }
    val appTransferViewModel = hiltViewModel<AppTransferViewModel>()
    val transferHandler = remember { TransferHandlerImpl(appTransferViewModel) }
    val transferState by appTransferViewModel.state.collectAsStateWithLifecycle()
    val appDialogViewModel = hiltViewModel<AppDialogViewModel>()
    val dialogEvents by appDialogViewModel.dialogEvents.collectAsStateWithLifecycle()
    val transparentStrategy = remember { TransparentSceneStrategy<NavKey>() }
    val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }
    val bottomSheetStrategy = remember { BottomSheetSceneStrategy<NavKey>() }

    EventEffect(event = dialogEvents, onConsumed = appDialogViewModel::dialogDisplayed) {
        backStack.add(it.dialogDestination)
    }

    when (val appState = state) {
        is AppContentState.Data -> {
            CompositionLocalProvider(
                LocalSnackBarHostState provides snackbarHostState
            ) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    sceneStrategy = transparentStrategy.chain(dialogStrategy)
                        .chain(bottomSheetStrategy),
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    entryProvider = entryProvider {
                        appState.featureDestinations
                            .forEach { destination ->
                                destination.navigationGraph(
                                    this,
                                    navigationHandler,
                                    transferHandler
                                )
                            }

                        appState.appDialogDestinations.forEach { destination ->
                            destination.navigationGraph(
                                this,
                                navigationHandler,
                                appDialogViewModel::eventHandled
                            )
                        }

                        entry<HomeScreensNavKey> {
                            HomeScreens(
                                transferHandler = transferHandler,
                                navigationHandler = navigationHandler,
                                initialDestination = it.initialDestination,
                            )
                        }
                    }
                )

                StartTransferComponent(
                    event = transferState.transferEvent,
                    onConsumeEvent = appTransferViewModel::consumedTransferEvent,
                )
            }
        }

        AppContentState.Loading -> {}

        AppContentState.FetchingNodes -> {
            val viewModel =
                hiltViewModel<LoginInProgressViewModel, LoginInProgressViewModel.Factory>(
                    key = "LoginInProgressViewModel ${navKey.session}",
                    creationCallback = { factory ->
                        factory.create(navKey)
                    }
                )
            FetchingContentView(viewModel = viewModel)
        }
    }
}

infix fun <T : Any> SceneStrategy<T>.chain(sceneStrategy: SceneStrategy<T>): SceneStrategy<T> =
    object : SceneStrategy<T> {
        override fun SceneStrategyScope<T>.calculateScene(
            entries: List<NavEntry<T>>,
        ): Scene<T>? =
            this@chain.run { calculateScene(entries) } ?: with(sceneStrategy) {
                calculateScene(
                    entries
                )
            }
    }
