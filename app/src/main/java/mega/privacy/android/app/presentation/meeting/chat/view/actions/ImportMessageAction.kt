package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.compose.runtime.Composable
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

internal class ImportMessageAction(
    private val chatViewModel: ChatViewModel,
) : MessageAction(
    text = R.string.general_import,
    icon = R.drawable.ic_cloud_upload_medium_regular_outline,
    testTag = "import_node",
) {
    override fun appliesTo(messages: Set<TypedMessage>) = messages.isNotEmpty()
            && messages.all { it is NodeAttachmentMessage }

    @Composable
    override fun OnTrigger(messages: Set<TypedMessage>, onHandled: () -> Unit) {
        onHandled()
    }
}