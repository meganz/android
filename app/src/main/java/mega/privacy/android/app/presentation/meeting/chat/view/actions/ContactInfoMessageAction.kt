package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.compose.runtime.Composable
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.core.ui.model.MenuActionWithClick
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

internal class ContactInfoMessageAction(
    private val chatViewModel: ChatViewModel,
) : MessageAction(
    text = R.string.general_info,
    icon = R.drawable.ic_info_medium_regular_outline,
    testTag = "action_contact_info",
) {
    override fun appliesTo(messages: Set<TypedMessage>) = messages.size == 1
            && messages.first() is ContactAttachmentMessage
            && (messages.first() as ContactAttachmentMessage).isContact

    override fun toolbarItem(
        messages: Set<TypedMessage>,
        onClick: () -> Unit,
    ): MenuActionWithClick? = null

    @Composable
    override fun OnTrigger(messages: Set<TypedMessage>, onHandled: () -> Unit) {
        chatViewModel.onOpenContactInfo((messages.first() as ContactAttachmentMessage).contactEmail)
        onHandled()
    }
}