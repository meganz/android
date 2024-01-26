package mega.privacy.android.domain.entity.chat.messages.paging

import mega.privacy.android.domain.entity.chat.ChatGeolocation
import mega.privacy.android.domain.entity.chat.Giphy
import mega.privacy.android.domain.entity.chat.RichPreview

/**
 * PagedChatMessage
 *
 * @property id Identifier of the message
 * @property chatMessageInfo [ChatMessageInfo]
 * @property shouldShowDate Whether the date should be shown
 * @property shouldShowTime Whether the time should be shown
 * @property shouldShowAvatar Whether the avatar should be shown
 * @property geoLocation [ChatGeolocation]
 * @property richPreview [richPreview]
 * @property giphyId [Giphy]
 */
interface PagedChatMessage {
    val id: Long
    val chatMessageInfo: ChatMessageInfo
    val shouldShowDate: Boolean
    val shouldShowTime: Boolean
    val shouldShowAvatar: Boolean
    val geoLocation: ChatGeolocation?
    val richPreview: RichPreview?
    val giphyId: Giphy?
}