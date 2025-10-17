package mega.privacy.android.app.appstate.content.view

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import mega.privacy.android.app.appstate.content.navigation.view.HomeScreens
import mega.privacy.android.app.appstate.content.navigation.view.HomeScreensNavKey
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MegaApp(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
    onInteraction: () -> Unit,
    navigationParameter: HomeScreensNavKey,
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
        HomeScreens(
            transferHandler = transferHandler,
            navigationHandler = navigationHandler,
            initialDestination = navigationParameter.initialDestination,
        )
    }
}