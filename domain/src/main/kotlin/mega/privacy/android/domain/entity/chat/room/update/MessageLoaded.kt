package mega.privacy.android.domain.entity.chat.room.update

import mega.privacy.android.domain.entity.chat.ChatMessage

/**
 * Message loaded
 *
 * @property message
 */
data class MessageLoaded(
    override val message: ChatMessage,
) : ChatRoomMessageUpdate