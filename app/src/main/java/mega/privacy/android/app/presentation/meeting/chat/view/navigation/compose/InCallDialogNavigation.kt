package mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.dialog
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.ParticipatingInACallDialog

private const val callChatIdArg = "callChatId"
internal fun NavGraphBuilder.inCallDialog(
    navController: NavHostController,
    startMeeting: (Long) -> Unit,
) {
    dialog(route = "participatingInACall/{$callChatIdArg}") { backStackEntry ->
        val callChatId = backStackEntry.arguments?.getString(callChatIdArg)?.toLongOrNull() ?: -1L
        val onConfirm: () -> Unit = {
            if (callChatId != -1L) {
                startMeeting(callChatId)
            }
        }

        ParticipatingInACallDialog(
            onDismiss = { navController.popBackStack() },
            onConfirm = onConfirm
        )
    }
}

internal fun NavHostController.navigateToInCallDialog(callChatId: Long, navOptions: NavOptions? = null) {
    navigate("participatingInACall/$callChatId", navOptions)
}