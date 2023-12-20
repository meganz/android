package mega.privacy.android.app.presentation.meeting.chat.view.message.contact

import mega.privacy.android.core.ui.controls.chat.messages.ContactAttachmentMessageView as CoreContactAttachmentMessageView
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.app.presentation.extensions.isValid
import mega.privacy.android.app.presentation.meeting.chat.view.ChatAvatar
import mega.privacy.android.core.ui.controls.chat.ChatStatusIcon
import mega.privacy.android.core.ui.controls.chat.UiChatStatus
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserVisibility

/**
 * Contact attachment message view
 *
 * @param message Contact attachment message
 * @param modifier Modifier
 * @param viewModel Contact attachment message view model
 */
@Composable
fun ContactAttachmentMessageView(
    message: ContactAttachmentMessage,
    modifier: Modifier = Modifier,
    viewModel: ContactAttachmentMessageViewModel = hiltViewModel(),
) {
    var status by remember { mutableStateOf<UserChatStatus?>(null) }
    var userName by remember { mutableStateOf(message.contactUserName) }
    LaunchedEffect(message.contactEmail) {
        val item = viewModel.loadContactInfo(
            contactEmail = message.contactEmail
        )
        status = item?.status?.takeIf { item.visibility == UserVisibility.Visible }
        userName =
            item?.contactData?.alias ?: item?.contactData?.fullName ?: message.contactUserName
    }
    CoreContactAttachmentMessageView(
        modifier = modifier,
        isMe = message.isMine,
        userName = userName,
        email = message.contactEmail,
        avatar = {
            ChatAvatar(handle = message.contactHandle, modifier = Modifier.size(40.dp))
        },
        statusIcon = {
            if (status?.isValid() == true) {
                val chatStatus = when (status) {
                    UserChatStatus.Online -> UiChatStatus.Online
                    UserChatStatus.Away -> UiChatStatus.Away
                    UserChatStatus.Busy -> UiChatStatus.Busy
                    else -> UiChatStatus.Offline
                }
                ChatStatusIcon(
                    modifier = Modifier.align(Alignment.TopEnd),
                    status = chatStatus
                )
            }
        },
    )
}