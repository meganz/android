package mega.privacy.android.app.appstate.content.view

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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.privacy.android.app.appstate.content.model.AppContentState
import mega.privacy.android.app.appstate.content.navigation.NavigationHandlerImpl
import mega.privacy.android.app.appstate.content.navigation.view.MainNavigationScaffoldDestination
import mega.privacy.android.app.appstate.content.navigation.view.mainNavigationScaffold
import mega.privacy.android.app.appstate.transfer.AppTransferViewModel
import mega.privacy.android.app.appstate.transfer.TransferHandlerImpl
import mega.privacy.android.app.presentation.settings.model.storageTargetPreference
import mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponent
import mega.privacy.android.navigation.contract.bottomsheet.MegaBottomSheetNavigationProvider
import mega.privacy.android.navigation.contract.bottomsheet.rememberBottomSheetNavigator
import mega.privacy.android.navigation.megaNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MegaApp(
    appContentState: AppContentState.Data,
    onInteraction: () -> Unit,
) {
    val context = LocalContext.current
    val bottomSheetNavigator = rememberBottomSheetNavigator()
    val navController = rememberNavController(bottomSheetNavigator)
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
            MegaBottomSheetNavigationProvider(
                megaBottomSheetNavigator = bottomSheetNavigator
            ) {
                NavHost(
                    navController = navController,
                    startDestination = MainNavigationScaffoldDestination,
                ) {
                    mainNavigationScaffold(
                        transferHandler = transferHandler,
                        navigationHandler = navigationHandler,
                    )
                    appContentState.featureDestinations
                        .forEach {
                            it.navigationGraph(this, navigationHandler, transferHandler)
                        }
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