package mega.privacy.android.domain.entity.chat.room.update

import mega.privacy.android.domain.entity.chat.ChatMessage

/**
 * History truncated.
 *
 * @property message
 */
data class HistoryTruncated(
    override val message: ChatMessage,
) : ChatRoomMessageUpdate