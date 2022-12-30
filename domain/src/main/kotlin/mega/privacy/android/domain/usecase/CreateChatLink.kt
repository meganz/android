package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.ChatRequest

/**
 * Use case for create chat link
 */
fun interface CreateChatLink {

    /**
     * Invoke.
     *
     * @param chatId    The chat id.
     * @return          ChatRequest
     */
    suspend operator fun invoke(
        chatId: Long
    ): ChatRequest
}