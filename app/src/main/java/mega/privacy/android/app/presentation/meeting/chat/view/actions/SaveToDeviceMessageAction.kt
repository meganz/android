package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.compose.runtime.Composable
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.model.messages.actions.MessageActionGroup
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.mobile.analytics.event.ChatConversationDownloadActionMenuEvent

internal class SaveToDeviceMessageAction(
    private val chatViewModel: ChatViewModel,
) : MessageAction(
    text = R.string.general_save_to_device,
    icon = mega.privacy.android.icon.pack.R.drawable.ic_download_medium_regular_outline,
    testTag = "save_to_device",
    group = MessageActionGroup.Transfer,
) {
    override fun shouldDisplayFor(messages: Set<TypedMessage>) = messages.isNotEmpty() &&
            messages.all { it is NodeAttachmentMessage && it.exists }

    @Composable
    override fun OnTrigger(messages: Set<TypedMessage>, onHandled: () -> Unit) {
        chatViewModel.onDownloadNode(
            messages.map { (it as NodeAttachmentMessage).fileNode }
        )
        onHandled()
    }

    override fun trackTriggerEvent(source: TriggerSource) {
        when (source) {
            TriggerSource.BottomSheet -> Unit
            TriggerSource.Toolbar -> {
                Analytics.tracker.trackEvent(ChatConversationDownloadActionMenuEvent)
            }
        }
    }
}