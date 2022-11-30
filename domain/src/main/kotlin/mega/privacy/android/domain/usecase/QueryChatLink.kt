package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.ChatRequest

/**
 * Use case for query chat link
 */
fun interface QueryChatLink {

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