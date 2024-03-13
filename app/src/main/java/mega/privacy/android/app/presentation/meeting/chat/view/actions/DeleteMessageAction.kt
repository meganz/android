package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.model.messages.actions.MessageActionGroup
import mega.privacy.android.app.presentation.meeting.chat.view.dialog.DeleteMessagesConfirmationDialog
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.mobile.analytics.event.ChatConversationDeleteActionMenuEvent
import mega.privacy.mobile.analytics.event.ChatConversationRemoveActionMenuItemEvent

internal class DeleteMessageAction(
    private val chatViewModel: ChatViewModel,
) : MessageAction(
    text = R.string.context_delete,
    icon = R.drawable.ic_trash_medium_regular_outline,
    testTag = "action_delete",
    group = MessageActionGroup.Delete,
) {
    override fun shouldDisplayFor(messages: Set<TypedMessage>) = messages.isNotEmpty() &&
            messages.all { it.isDeletable && it.isMine }

    override val appliesToSendError = true

    override fun isBottomSheetItemDestructive() = true

    @Composable
    override fun OnTrigger(messages: Set<TypedMessage>, onHandled: () -> Unit) {
        var showDialog by rememberSaveable { mutableStateOf(true) }

        if (showDialog) {
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

    override fun trackTriggerEvent(source: TriggerSource) {
        when (source) {
            TriggerSource.BottomSheet -> {
                Analytics.tracker.trackEvent(ChatConversationRemoveActionMenuItemEvent)
            }

            TriggerSource.Toolbar -> {
                Analytics.tracker.trackEvent(ChatConversationDeleteActionMenuEvent)
            }
        }
    }
}