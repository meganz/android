package mega.privacy.android.app.appstate.view

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import mega.privacy.android.app.appstate.model.AppState
import mega.privacy.android.navigation.contract.NavigationUiController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MegaApp(
    navController: NavHostController,
    appState: AppState.Data,
    onInteraction: () -> Unit,
) {
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
        NavHost(
            navController = navController,
            startDestination = MainNavigationScaffoldDestination,
        ) {
            val navigationHandler = NavigationHandlerImpl(navController)

            mainNavigationScaffold(
                topLevelDestinations = appState.mainNavItems,
                startDestination = appState.initialMainDestination,
                builder = { navigationUiController: NavigationUiController ->
                    appState.mainNavScreens.forEach {
                        it(this, navigationHandler, navigationUiController)
                    }
                }
            )
            appState.featureDestinations
                .forEach {
                    it.navigationGraph(this, navigationHandler)
                }
        }
    }
}