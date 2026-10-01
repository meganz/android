package mega.privacy.android.domain.entity.chat.messages.request

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ContainsMetaType
import mega.privacy.android.domain.entity.chat.messages.ChatMessageInfo
import mega.privacy.android.domain.entity.chat.messages.meta.ChatGeolocationInfo
import mega.privacy.android.domain.entity.chat.messages.meta.ChatGifInfo
import mega.privacy.android.domain.entity.chat.messages.meta.ChatRichPreviewInfo
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import mega.privacy.android.domain.entity.node.Node

/**
 * Create typed message request
 *
 * @property message
 * @property isMine
 * @property metaType
 * @property textMessage
 * @property chatRichPreviewInfo
 * @property chatGeolocationInfo
 * @property chatGifInfo
 * @property nodeList
 * @property reactions
 */
data class CreateTypedMessageRequest(
    val message: ChatMessageInfo,
    override val chatId: Long,
    override val isMine: Boolean,
    override val metaType: ContainsMetaType?,
    override val textMessage: String?,
    override val chatRichPreviewInfo: ChatRichPreviewInfo?,
    override val chatGeolocationInfo: ChatGeolocationInfo?,
    override val chatGifInfo: ChatGifInfo?,
    override val nodeList: List<Node>,
    override val reactions: List<Reaction>,
    override val exists: Boolean,
) : ChatMessageInfo by message, CreateTypedMessageInfo {
    constructor(
        chatMessage: ChatMessage,
        chatId: Long,
        isMine: Boolean,
        reactions: List<Reaction>,
        exists: Boolean,
    ) : this(
        message = chatMessage,
        chatId = chatId,
        isMine = isMine,
        metaType = chatMessage.containsMeta?.type,
        textMessage = chatMessage.containsMeta?.textMessage,
        chatRichPreviewInfo = chatMessage.containsMeta?.richPreview,
        chatGeolocationInfo = chatMessage.containsMeta?.geolocation,
        chatGifInfo = chatMessage.containsMeta?.giphy,
        nodeList = chatMessage.nodeList,
        reactions = reactions,
        exists = exists,
    )
}