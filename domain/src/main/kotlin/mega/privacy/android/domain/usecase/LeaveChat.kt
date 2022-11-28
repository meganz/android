package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.ChatRequest

/**
 * Use case for leave a chat
 */
fun interface LeaveChat {

    /**
     * Invoke.
     *
     * @param chatId    The chat id.
     * @return          The Chat Request.
     */
    suspend operator fun invoke(
        chatId: Long,
    ): ChatRequest
}