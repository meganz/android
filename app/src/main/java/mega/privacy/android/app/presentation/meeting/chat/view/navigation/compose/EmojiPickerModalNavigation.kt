package mega.privacy.android.app.presentation.meeting.chat.view.navigation.compose

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.bottomSheet
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.EmojiBottomSheet

private const val messageIdArg = "messageId"

@OptIn(ExperimentalMaterialNavigationApi::class)
internal fun NavGraphBuilder.emojiPickerModal(
    navController: NavHostController,
    closeBottomSheets: () -> Unit,
) {
    bottomSheet(route = "emojiModal/{$messageIdArg}") { backStackEntry ->
        val viewModel = backStackEntry.sharedViewModel<ChatViewModel>(navController)

        val messageId = backStackEntry.arguments?.getString(messageIdArg)?.toLongOrNull()
            ?: throw IllegalArgumentException("messageId cannot be null for emojiModal")

        EmojiBottomSheet(
            onReactionClicked = {
                viewModel.onAddReaction(messageId, it)
                closeBottomSheets()
            },
        )
    }
}

internal fun NavHostController.navigateToEmojiPickerModal(messageId: Long, navOptions: NavOptions? = null) {
    navigate("emojiModal/$messageId", navOptions)
}