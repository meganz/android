package mega.privacy.android.domain.entity.chat.room.update

import mega.privacy.android.domain.entity.chat.ChatMessage

/**
 * Message update
 *
 * @property message
 */
data class MessageUpdate(
    override val message: ChatMessage,
) : ChatRoomMessageUpdate