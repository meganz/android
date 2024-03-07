package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.compose.runtime.Composable
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

internal class SaveToDeviceMessageAction(
    private val chatViewModel: ChatViewModel,
) : MessageAction(
    text = R.string.general_save_to_device,
    icon = mega.privacy.android.icon.pack.R.drawable.ic_menu_download,
    testTag = "save_to_device",
) {
    override fun shouldDisplayFor(messages: Set<TypedMessage>) = messages.isNotEmpty() &&
            messages.all { it is NodeAttachmentMessage }

    @Composable
    override fun OnTrigger(messages: Set<TypedMessage>, onHandled: () -> Unit) {
        chatViewModel.onDownloadNode(
            messages.map { (it as NodeAttachmentMessage).fileNode }
        )
        onHandled()
    }
}