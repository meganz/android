package mega.privacy.android.domain.entity.chat.message.request

import mega.privacy.android.domain.entity.chat.ChatGeolocation
import mega.privacy.android.domain.entity.chat.ChatGeolocationInfo
import mega.privacy.android.domain.entity.chat.ChatGifInfo
import mega.privacy.android.domain.entity.chat.ChatMessageInfo
import mega.privacy.android.domain.entity.chat.ChatRichPreviewInfo
import mega.privacy.android.domain.entity.chat.ContainsMetaType
import mega.privacy.android.domain.entity.chat.Giphy
import mega.privacy.android.domain.entity.chat.RichPreview
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