package mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.dialog
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.EndCallForAllDialog

internal fun NavGraphBuilder.endCallForAllDialog(
    navController: NavHostController,
) {
    dialog(route = "endCallForAll") { backStackEntry ->
        val viewModel = backStackEntry.sharedViewModel<ChatViewModel>(navController)
        EndCallForAllDialog(
            onDismiss = { navController.popBackStack() },
            onConfirm = {
                viewModel.endCall()
                navController.popBackStack()
            }
        )
    }
}

internal fun NavHostController.navigateToEndCallForAllDialog(navOptions: NavOptions? = null) {
    navigate("endCallForAll", navOptions)
}