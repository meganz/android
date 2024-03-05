package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.compose.runtime.Composable
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 *  Invite message action for bottom sheet and tool bar in select mode
 */
class InviteMessageAction(
    private val chatViewModel: ChatViewModel,
) : MessageAction(
    text = R.string.contact_invite,
    icon = R.drawable.ic_icon_plus_circle_medium_regular_outline,
    testTag = "action_invite"
) {
    override fun appliesTo(messages: Set<TypedMessage>): Boolean =
        messages.isNotEmpty() && messages.all {
            it is ContactAttachmentMessage && !it.isMe && (!it.isContact || it.userHandle != -1L)
        }

    @Composable
    override fun OnTrigger(messages: Set<TypedMessage>, onHandled: () -> Unit) {
        chatViewModel.inviteContacts(messages.map { it as ContactAttachmentMessage }.toSet())
        onHandled()
    }
}
