package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Chat repository
 */
interface ChatRepository {
    /**
     * Notify chat logout
     *
     * @return a flow that emits true whenever chat api is successfully logged out
     */
    fun notifyChatLogout(): Flow<Boolean>

    /**
     * Starts a chat conversation with the provided contacts.
     *
     * @param chatId   The Chat id.
     * @return True if is enabled, false if not.
     */
    suspend fun setOpenInvite(chatId: Long): Boolean
}