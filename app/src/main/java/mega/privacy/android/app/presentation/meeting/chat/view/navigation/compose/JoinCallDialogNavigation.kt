package mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.dialog
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.JoinAnswerCallDialog

private const val isGroupArg = "isGroup"
private const val callsInOtherChatsArg = "callsInOtherChats"

internal fun NavGraphBuilder.joinCallDialog(
    navController: NavHostController,
) {
    dialog(route = "joinAnswerCall/{$isGroupArg}/{$callsInOtherChatsArg}") { backStackEntry ->
        val viewModel = backStackEntry.sharedViewModel<ChatViewModel>(navController)
        val isGroup =
            backStackEntry.arguments?.getString(isGroupArg)?.toBooleanStrictOrNull() ?: false
        val callsInOtherChats =
            backStackEntry.arguments?.getString(callsInOtherChatsArg)?.toIntOrNull() ?: 0

        JoinAnswerCallDialog(
            isGroup = isGroup,
            numberOfCallsInOtherChats = callsInOtherChats,
            onHoldAndAnswer = {
                viewModel.onHoldAndAnswerCall()
                navController.popBackStack()
            },
            onEndAndAnswer = {
                viewModel.onEndAndAnswerCall()
                navController.popBackStack()
            },
            onDismiss = navController::popBackStack,
        )
    }
}

internal fun NavHostController.navigateToJoinCallDialog(isGroup: Boolean, callsInOtherChats: Int, navOptions: NavOptions? = null) {
    navigate("joinAnswerCall/$isGroup/$callsInOtherChats", navOptions)
}
