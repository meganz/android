package mega.privacy.android.domain.entity.chat.message.request

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageInfo
import mega.privacy.android.domain.entity.node.Node

/**
 * Create typed message request
 *
 * @property message [ChatMessage]
 * @property isMine True if the message is mine.
 * @property shouldShowAvatar True if the avatar should be shown.
 * @property shouldShowTime True if the time should be shown.
 * @property shouldShowDate True if the date should be shown.
 */
data class CreateTypedMessageRequest(
    val message: ChatMessage,
    override val isMine: Boolean,
    override val shouldShowAvatar: Boolean,
    override val shouldShowTime: Boolean,
    override val shouldShowDate: Boolean,
) : ChatMessageInfo by message, CreateTypedMessageInfo {
    override val metaType = message.containsMeta?.type
    override val textMessage = message.containsMeta?.textMessage
    override val richPreview = message.containsMeta?.richPreview
    override val geolocation = message.containsMeta?.geolocation
    override val giphy = message.containsMeta?.giphy
    override val nodeList: List<Node> = message.nodeList
}