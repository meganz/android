package mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.compose.material.navigation.bottomSheet
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.MessageNotSentBottomSheet

internal fun NavGraphBuilder.messageNotSentModal(
    navController: NavHostController,
    closeBottomSheets: () -> Unit,
) {
    bottomSheet(route = "messageNotSentModal") { backStackEntry ->
        val viewModel = backStackEntry.sharedViewModel<ChatViewModel>(navController)

        MessageNotSentBottomSheet(
            actions = viewModel.getApplicableBotomsheetActions { closeBottomSheets() },
            areTransfersPaused = viewModel.areTransfersPaused()
        )
    }
}

internal fun NavHostController.navigateToMessageNotSentModal(navOptions: NavOptions? = null) {
    navigate("messageNotSentModal", navOptions)
}