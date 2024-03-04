package mega.privacy.android.domain.entity.chat.room.update

import mega.privacy.android.domain.entity.chat.ChatMessage

/**
 * Message received
 *
 * @property message
 */
data class MessageReceived(
    override val message: ChatMessage,
) : ChatRoomMessageUpdate