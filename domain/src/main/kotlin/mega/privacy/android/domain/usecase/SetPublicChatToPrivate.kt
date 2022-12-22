package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.ChatRequest

/**
 * Use case for set private chat room
 */
fun interface SetPublicChatToPrivate {

    /**
     * Invoke.
     *
     * @param chatId  The chat id.
     * @return The chat conversation handle.
     */
    suspend operator fun invoke(
        chatId: Long
    ): ChatRequest
}