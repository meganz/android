package mega.privacy.android.app.presentation.meeting.chat.view.actions

import mega.privacy.android.icon.pack.R.drawable as IconPack
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.messages.actions.MessageActionGroup
import mega.privacy.android.app.presentation.meeting.chat.view.message.error.SendErrorViewModel
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.mobile.analytics.event.ChatConversationRetryMenuItemEvent

internal class RetryMessageAction : MessageAction(
    text = R.string.message_option_retry,
    icon = IconPack.ic_rotate_ccw_medium_regular_outline,
    testTag = "action_retry",
    group = MessageActionGroup.Retry,
) {
    override fun shouldDisplayFor(messages: Set<TypedMessage>) = messages.isNotEmpty()
            && messages.all { it.status == ChatMessageStatus.SENDING_MANUAL || it.status == ChatMessageStatus.SERVER_REJECTED }

    override val appliesToSendError = true

    @Composable
    override fun OnTrigger(messages: Set<TypedMessage>, onHandled: () -> Unit) {
        Analytics.tracker.trackEvent(ChatConversationRetryMenuItemEvent)
        val sendErrorViewModel: SendErrorViewModel = hiltViewModel()
        sendErrorViewModel.retry(messages)
        onHandled()
    }
}
