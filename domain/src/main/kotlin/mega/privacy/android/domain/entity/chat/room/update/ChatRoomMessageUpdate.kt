package mega.privacy.android.domain.entity.chat.room.update

import mega.privacy.android.domain.entity.chat.ChatMessage

/**
 * Chat room message update
 */
interface ChatRoomMessageUpdate : Update {
    /**
     * Message
     */
    val message: ChatMessage
}