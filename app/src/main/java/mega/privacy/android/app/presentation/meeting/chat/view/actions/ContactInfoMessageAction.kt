package mega.privacy.android.app.presentation.meeting.chat.view.actions

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.runtime.Composable
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.model.messages.actions.MessageActionGroup
import mega.privacy.android.core.ui.model.MenuActionWithClick
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.mobile.analytics.event.ChatConversationViewContactsActionMenuItemEvent

internal class ContactInfoMessageAction(
    private val chatViewModel: ChatViewModel,
) : MessageAction(
    text = R.string.general_info,
    icon = iconPackR.drawable.ic_info_medium_regular_outline,
    testTag = "action_contact_info",
    group = MessageActionGroup.Contact,
) {
    override fun shouldDisplayFor(messages: Set<TypedMessage>) = messages.size == 1
            && messages.first() is ContactAttachmentMessage
            && (messages.first() as ContactAttachmentMessage).isContact

    override fun toolbarItem(
        messages: Set<TypedMessage>,
        onClick: () -> Unit,
    ): MenuActionWithClick? = null

    @Composable
    override fun OnTrigger(messages: Set<TypedMessage>, onHandled: () -> Unit) {
        Analytics.tracker.trackEvent(ChatConversationViewContactsActionMenuItemEvent)
        chatViewModel.onOpenContactInfo((messages.first() as ContactAttachmentMessage).contactEmail)
        onHandled()
    }
}