package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.DeleteMessagesConfirmationDialog
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

internal class DeleteMessageAction(
    private val chatViewModel: ChatViewModel,
) : MessageAction(
    text = R.string.context_delete,
    icon = R.drawable.ic_trash_medium_regular_outline,
    testTag = "action_delete",
) {
    override fun appliesTo(messages: Set<TypedMessage>) = messages
        .all { it.isDeletable && it.isMine }

    @Composable
    override fun OnTrigger(messages: Set<TypedMessage>, onHandled: () -> Unit) {
        var showDialog by remember {
            mutableStateOf(true)
        }

        if (showDialog){
            DeleteMessagesConfirmationDialog(
                messagesCount = messages.size,
                onDismiss = {
                    showDialog = false
                    onHandled()
                },
                onConfirm = {
                    chatViewModel.onDeletedMessages(messages)
                    showDialog = false
                    onHandled()
                })
        }
    }
}