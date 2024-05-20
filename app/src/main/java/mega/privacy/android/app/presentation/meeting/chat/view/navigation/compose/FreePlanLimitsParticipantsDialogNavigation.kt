package mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.dialog
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.view.dialog.FreePlanLimitParticipantsDialog

internal fun NavGraphBuilder.freePlanLimitsParticipantsDialog(
    navController: NavHostController,
) {
    dialog(route = "freePlanLimitParticipants") { backStackEntry ->
        val viewModel = backStackEntry.sharedViewModel<ChatViewModel>(navController)

        FreePlanLimitParticipantsDialog(
            onConfirm = {
                viewModel.consumeShowFreePlanParticipantsLimitDialogEvent()
                navController.popBackStack()
            }
        )
    }
}

internal fun NavHostController.navigateToFreePlanLimitsParticipantsDialog(navOptions: NavOptions? = null) {
    navigate("freePlanLimitParticipants", navOptions)
}