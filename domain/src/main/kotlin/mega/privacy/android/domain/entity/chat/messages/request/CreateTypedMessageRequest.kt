package mega.privacy.android.domain.entity.chat.messages.request

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ContainsMetaType
import mega.privacy.android.domain.entity.chat.messages.ChatMessageInfo
import mega.privacy.android.domain.entity.chat.messages.meta.ChatGeolocationInfo
import mega.privacy.android.domain.entity.chat.messages.meta.ChatGifInfo
import mega.privacy.android.domain.entity.chat.messages.meta.ChatRichPreviewInfo
import mega.privacy.android.domain.entity.node.Node

/**
 * Create typed message request
 *
 * @property message
 * @property isMine
 * @property shouldShowAvatar
 * @property shouldShowTime
 * @property shouldShowDate
 * @property metaType
 * @property textMessage
 * @property chatRichPreviewInfo
 * @property chatGeolocationInfo
 * @property chatGifInfo
 * @property nodeList
 */
data class CreateTypedMessageRequest(
    val message: ChatMessageInfo,
    override val isMine: Boolean,
    override val shouldShowAvatar: Boolean,
    override val shouldShowTime: Boolean,
    override val shouldShowDate: Boolean,
    override val metaType: ContainsMetaType?,
    override val textMessage: String?,
    override val chatRichPreviewInfo: ChatRichPreviewInfo?,
    override val chatGeolocationInfo: ChatGeolocationInfo?,
    override val chatGifInfo: ChatGifInfo?,
    override val nodeList: List<Node>,
) : ChatMessageInfo by message, CreateTypedMessageInfo {
    constructor(
        chatMessage: ChatMessage,
        isMine: Boolean,
        shouldShowAvatar: Boolean,
        shouldShowTime: Boolean,
        shouldShowDate: Boolean,
    ) : this(
        message = chatMessage,
        isMine = isMine,
        shouldShowAvatar = shouldShowAvatar,
        shouldShowTime = shouldShowTime,
        shouldShowDate = shouldShowDate,
        metaType = chatMessage.containsMeta?.type,
        textMessage = chatMessage.containsMeta?.textMessage,
        chatRichPreviewInfo = chatMessage.containsMeta?.richPreview,
        chatGeolocationInfo = chatMessage.containsMeta?.geolocation,
        chatGifInfo = chatMessage.containsMeta?.giphy,
        nodeList = chatMessage.nodeList,
    )
}