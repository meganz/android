package mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.dialog
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.EmojiPickerDialog

private const val messageIdArg = "messageId"

internal fun NavGraphBuilder.emojiPickerModal(
    navController: NavHostController,
    closeView: () -> Unit,
) {
    dialog(route = "emojiModal/{$messageIdArg}") { backStackEntry ->
        val viewModel = backStackEntry.sharedViewModel<ChatViewModel>(navController)

        val messageId = backStackEntry.arguments?.getString(messageIdArg)?.toLongOrNull()
            ?: throw IllegalArgumentException("messageId cannot be null for emojiModal")

        EmojiPickerDialog(
            onReactionClicked = {
                viewModel.onAddReaction(messageId, it)
                closeView()
            },
        )
    }
}

internal fun NavHostController.navigateToEmojiPickerModal(
    messageId: Long,
    navOptions: NavOptions? = null,
) {
    navigate("emojiModal/$messageId", navOptions)
}