package mega.privacy.android.app.presentation.meeting.chat.view.actions

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.DeleteMessagesConfirmationDialog
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.options.DeleteBottomSheetOption
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

internal class DeleteMessageAction(
    private val chatViewModel: ChatViewModel,
) : MessageAction {
    override fun appliesTo(messages: List<TypedMessage>) = messages
        .all { it.isDeletable && it.isMine }

    override fun bottomSheetMenuItem(
        messages: List<TypedMessage>,
        context: Context,
        hideBottomSheet: () -> Unit,
    ): @Composable () -> Unit =
        {
            var showConfirmRemoveMessages by rememberSaveable { mutableStateOf(false) }

            DeleteBottomSheetOption {
                showConfirmRemoveMessages = true
            }

            if (showConfirmRemoveMessages) {
                DeleteMessagesConfirmationDialog(
                    messagesCount = messages.size,
                    onDismiss = {
                        hideBottomSheet()
                        showConfirmRemoveMessages = false
                    },
                    onConfirm = {
                        hideBottomSheet()
                        showConfirmRemoveMessages = false
                        chatViewModel.onDeletedMessages(messages)
                    })
            }
        }
}