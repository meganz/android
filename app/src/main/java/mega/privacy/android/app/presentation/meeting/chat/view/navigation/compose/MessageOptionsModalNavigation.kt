package mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.bottomSheet
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.MessageOptionsBottomSheet

private const val messageIdArg = "messageId"

@OptIn(ExperimentalMaterialNavigationApi::class)
internal fun NavGraphBuilder.messageOptionsModal(
    navController: NavHostController,
    navigateToEmojiPicker: (Long) -> Unit,
    closeBottomSheets: () -> Unit,
) {
    bottomSheet(route = "messageOptionsModal/{$messageIdArg}") { backStackEntry ->
        val viewModel = backStackEntry.sharedViewModel<ChatViewModel>(navController)

        val messageId = backStackEntry.arguments?.getString(messageIdArg)?.toLongOrNull()
            ?: throw IllegalArgumentException("messageId cannot be null for messageOptionsModal")

        MessageOptionsBottomSheet(
            messageId = messageId,
            onReactionClicked = {
                viewModel.onAddReaction(messageId, it)
                closeBottomSheets()
            },
            onMoreReactionsClicked = navigateToEmojiPicker,
            actions = viewModel.getApplicableBotomsheetActions { closeBottomSheets() },
        )
    }
}

internal fun NavHostController.navigateToMessageOptionsModal(
    messageId: Long,
    navOptions: NavOptions? = null,
) {
    navigate("messageOptionsModal/$messageId", navOptions)
}