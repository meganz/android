package mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.dialog
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.ClearChatConfirmationDialog

private const val isMeetingArg = "isMeeting"
internal fun NavGraphBuilder.clearChatConfirmationDialog(
    navController: NavHostController,
) {
    dialog(route = "clearChatConfirmation/{$isMeetingArg}") { backStackEntry ->
        val viewModel = backStackEntry.sharedViewModel<ChatViewModel>(navController)
        val isMeeting =
            backStackEntry.arguments?.getString(isMeetingArg)?.toBooleanStrictOrNull() ?: false

        ClearChatConfirmationDialog(
            isMeeting = isMeeting,
            onDismiss = { navController.popBackStack() },
            onConfirm = {
                viewModel.clearChatHistory()
                navController.popBackStack()
            }
        )
    }
}

internal fun NavHostController.navigateToClearChatConfirmationDialog(isMeeting: Boolean, navOptions: NavOptions? = null) {
    navigate("clearChatConfirmation/$isMeeting", navOptions)
}