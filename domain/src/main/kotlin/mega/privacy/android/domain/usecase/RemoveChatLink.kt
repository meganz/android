package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.ChatRequest

/**
 * Use case for remove chat link
 */
fun interface RemoveChatLink {

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