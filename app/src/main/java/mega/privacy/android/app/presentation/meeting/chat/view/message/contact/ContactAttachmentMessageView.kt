package mega.privacy.android.app.presentation.meeting.chat.view.message.contact

import mega.privacy.android.shared.original.core.ui.controls.chat.messages.ContactAttachmentMessageView as CoreContactAttachmentMessageView
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.meeting.chat.view.ChatAvatar
import mega.privacy.android.shared.original.core.ui.controls.chat.UiChatStatus
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage

/**
 * Contact attachment message view
 *
 * @param message Contact attachment message
 * @param modifier Modifier
 */
@Composable
fun ContactAttachmentMessageView(
    message: ContactAttachmentMessage,
    userName: String,
    status: UiChatStatus?,
    modifier: Modifier = Modifier,
) {

    CoreContactAttachmentMessageView(
        modifier = modifier,
        isMe = message.isMine,
        userName = userName,
        email = message.contactEmail,
        status = status,
        avatar = {
            ChatAvatar(handle = message.contactHandle, modifier = Modifier.size(40.dp))
        },
        isVerified = message.isVerified
    )
}

