package mega.privacy.android.app.appstate.view

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.privacy.android.app.appstate.model.AppState
import mega.privacy.android.app.appstate.transfer.AppTransferViewModel
import mega.privacy.android.app.appstate.transfer.TransferHandlerImpl
import mega.privacy.android.app.presentation.settings.model.storageTargetPreference
import mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponent
import mega.privacy.android.navigation.contract.NavigationUiController
import mega.privacy.android.navigation.megaNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MegaApp(
    navController: NavHostController,
    appState: AppState.Data,
    onInteraction: () -> Unit,
) {
    val context = LocalContext.current
    val megaNavigator = remember { context.megaNavigator }
    val appTransferViewModel = hiltViewModel<AppTransferViewModel>()
    val navigationHandler = remember { NavigationHandlerImpl(navController) }
    val transferHandler = remember { TransferHandlerImpl(appTransferViewModel) }
    val transferState by appTransferViewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    Box(modifier = Modifier.pointerInput(Unit) {
        awaitEachGesture {
            do {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press) {
                    onInteraction()
                }
            } while (event.changes.any { it.pressed })
        }
    }) {
        CompositionLocalProvider(
            LocalSnackBarHostState provides snackbarHostState
        ) {
            NavHost(
                navController = navController,
                startDestination = MainNavigationScaffoldDestination,
            ) {
                mainNavigationScaffold(
                    topLevelDestinations = appState.mainNavItems,
                    startDestination = appState.initialMainDestination,
                    builder = { navigationUiController: NavigationUiController ->
                        appState.mainNavScreens.forEach {
                            it(this, navigationHandler, navigationUiController, transferHandler)
                        }
                    }
                )
                appState.featureDestinations
                    .forEach {
                        it.navigationGraph(this, navigationHandler, transferHandler)
                    }
            }

            StartTransferComponent(
                event = transferState.transferEvent,
                onConsumeEvent = appTransferViewModel::consumedTransferEvent,
                navigateToStorageSettings = {
                    megaNavigator.openSettings(
                        context,
                        storageTargetPreference
                    )
                }
            )
        }
    }
}