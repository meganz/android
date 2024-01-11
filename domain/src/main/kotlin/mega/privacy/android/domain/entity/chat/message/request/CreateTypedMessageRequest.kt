package mega.privacy.android.domain.entity.chat.message.request

import mega.privacy.android.domain.entity.chat.ChatMessage

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
    val isMine: Boolean,
    val shouldShowAvatar: Boolean,
    val shouldShowTime: Boolean,
    val shouldShowDate: Boolean,
)