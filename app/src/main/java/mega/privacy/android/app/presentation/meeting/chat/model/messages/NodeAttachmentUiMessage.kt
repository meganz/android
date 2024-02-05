package mega.privacy.android.app.presentation.meeting.chat.model.messages

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.meeting.chat.extension.canForward
import mega.privacy.android.app.presentation.meeting.chat.view.message.attachment.NodeAttachmentMessageView
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Node attachment Ui message
 *
 * @param message [NodeAttachmentMessageView]
 * @param chatId
 */
data class NodeAttachmentUiMessage(
    private val message: NodeAttachmentMessage,
    private val chatId: Long,
    override val reactions: List<UIReaction>,
) : AvatarMessage() {

    @Composable
    override fun RowScope.ContentComposable(onLongClick: (TypedMessage) -> Unit) {
        NodeAttachmentMessageView(message, chatId)
    }

    override val showDate = message.shouldShowDate
    override val showTime = message.shouldShowTime
    override val showAvatar = message.shouldShowAvatar
    override val displayAsMine = message.isMine
    override val canForward = message.canForward
    override val timeSent = message.time
    override val userHandle = message.userHandle
    override val id = message.msgId
}