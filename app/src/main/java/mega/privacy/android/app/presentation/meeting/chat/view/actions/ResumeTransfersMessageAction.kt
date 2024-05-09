package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.compose.runtime.Composable
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.model.messages.actions.MessageActionGroup
import mega.privacy.android.domain.entity.chat.messages.PendingAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.mobile.analytics.event.ChatConversationResumeTransfersMenuItemEvent

internal class ResumeTransfersMessageAction(
    private val chatViewModel: ChatViewModel,
) : MessageAction(
    text = R.string.option_resume_transfers,
    icon = R.drawable.ic_rotate_cw_medium_regular_solid,
    testTag = "action_resume_transfers",
    group = MessageActionGroup.Retry,
) {
    override fun shouldDisplayFor(messages: Set<TypedMessage>) = messages.isNotEmpty()
            && chatViewModel.areTransfersPaused()
            && messages.all { it is PendingAttachmentMessage && it.isNotSent() }

    override val appliesToSendError = true

    @Composable
    override fun OnTrigger(messages: Set<TypedMessage>, onHandled: () -> Unit) {
        Analytics.tracker.trackEvent(ChatConversationResumeTransfersMenuItemEvent)
        chatViewModel.resumeTransfers()
        onHandled()
    }
}