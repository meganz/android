package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.compose.runtime.Composable
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.model.messages.actions.MessageActionGroup
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.mobile.analytics.event.ChatConversationSendMessageActionMenuEvent
import mega.privacy.mobile.analytics.event.ChatConversationSendMessageActionMenuItemEvent

internal class SendMessageAction(
    private val chatViewModel: ChatViewModel,
) : MessageAction(
    text = R.string.context_send_message,
    icon = R.drawable.ic_send_message_modal,
    testTag = "send_message",
    group = MessageActionGroup.Contact,
) {
    override fun shouldDisplayFor(messages: Set<TypedMessage>) = messages.isNotEmpty() &&
            messages.all { it is ContactAttachmentMessage && it.isContact }

    @Composable
    override fun OnTrigger(messages: Set<TypedMessage>, onHandled: () -> Unit) {
        chatViewModel.onOpenChatWith(messages.map { it as ContactAttachmentMessage })
        onHandled()
    }

    override fun trackTriggerEvent(source: TriggerSource) {
        when (source) {
            TriggerSource.BottomSheet -> {
                Analytics.tracker.trackEvent(ChatConversationSendMessageActionMenuItemEvent)
            }

            TriggerSource.Toolbar -> {
                Analytics.tracker.trackEvent(ChatConversationSendMessageActionMenuEvent)
            }
        }
    }
}