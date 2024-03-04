package mega.privacy.android.domain.entity.chat.room.update

import mega.privacy.android.domain.entity.chat.ChatMessage

/**
 * History truncated by retention time
 *
 * @property message
 */
data class HistoryTruncatedByRetentionTime(
    override val message: ChatMessage,
) : ChatRoomMessageUpdate