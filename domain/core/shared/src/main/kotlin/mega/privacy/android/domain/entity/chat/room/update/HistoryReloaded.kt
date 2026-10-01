package mega.privacy.android.domain.entity.chat.room.update

import mega.privacy.android.domain.entity.chat.ChatRoom

/**
 * History reloaded
 *
 * @property chatRoom
 */
data class HistoryReloaded(
    val chatRoom: ChatRoom,
) : Update