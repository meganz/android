package mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.dialog
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.ChatLocationView

internal fun NavGraphBuilder.chatLocationDialog(
    navController: NavHostController,
) {
    dialog(route = "location") { backStackEntry ->
        val viewModel = backStackEntry.sharedViewModel<ChatViewModel>(navController)
        val state by viewModel.state.collectAsStateWithLifecycle()
        ChatLocationView(
            isGeolocationEnabled = state.isGeolocationEnabled,
            onEnableGeolocation = viewModel::onEnableGeolocation,
            onSendLocationMessage = viewModel::sendLocationMessage,
            onDismissView = navController::popBackStack,
        )
    }
}

internal fun NavHostController.navigateChatLocationDialog(
    navOptions: NavOptions? = null,
) {
    navigate("location", navOptions)
}