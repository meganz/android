package mega.privacy.android.domain.entity.chat.messages.request

import mega.privacy.android.domain.entity.chat.ContainsMetaType
import mega.privacy.android.domain.entity.chat.messages.ChatMessageInfo
import mega.privacy.android.domain.entity.chat.messages.meta.ChatGeolocationInfo
import mega.privacy.android.domain.entity.chat.messages.meta.ChatGifInfo
import mega.privacy.android.domain.entity.chat.messages.meta.ChatRichPreviewInfo
import mega.privacy.android.domain.entity.node.Node

/**
 * Create typed message request info
 *
 * @property isMine True if the message is mine.
 * @property shouldShowAvatar True if the avatar should be shown.
 * @property shouldShowTime True if the time should be shown.
 * @property shouldShowDate True if the date should be shown.
 * @property metaType [ContainsMetaType]
 * @property textMessage
 * @property chatRichPreviewInfo [RichPreview]
 * @property chatGeolocationInfo [ChatGeolocation]
 * @property chatGifInfo [Giphy]
 * @property nodeList List of nodes
 */
interface CreateTypedMessageInfo : ChatMessageInfo {
    val isMine: Boolean
    val shouldShowAvatar: Boolean
    val shouldShowTime: Boolean
    val shouldShowDate: Boolean
    val metaType: ContainsMetaType?
    val textMessage: String?
    val chatRichPreviewInfo: ChatRichPreviewInfo?
    val chatGeolocationInfo: ChatGeolocationInfo?
    val chatGifInfo: ChatGifInfo?
    val nodeList: List<Node>
}