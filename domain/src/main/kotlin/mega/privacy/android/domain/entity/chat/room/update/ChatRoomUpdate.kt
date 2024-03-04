package mega.privacy.android.domain.entity.chat.room.update

import mega.privacy.android.domain.entity.chat.ChatRoom

/**
 * Chat room update
 *
 * @property chatRoom
 */
data class ChatRoomUpdate(
    val chatRoom: ChatRoom,
) : Update