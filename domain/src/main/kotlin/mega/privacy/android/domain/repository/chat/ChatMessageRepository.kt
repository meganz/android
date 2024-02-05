package mega.privacy.android.domain.repository.chat

/**
 * Chat message repository
 *
 */
interface ChatMessageRepository {
    /**
     * Set message seen
     *
     * @param chatId Chat id
     * @param messageId Message id
     */
    suspend fun setMessageSeen(chatId: Long, messageId: Long): Boolean

    /**
     * Get last message seen id
     *
     * @param chatId Chat id
     * @return Last message seen id
     */
    suspend fun getLastMessageSeenId(chatId: Long): Long
}