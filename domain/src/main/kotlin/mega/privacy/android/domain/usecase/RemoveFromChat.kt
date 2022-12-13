package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.ChatRequest

/**
 * Use case for remove participant from chat
 */
fun interface RemoveFromChat {

    /**
     * Invoke.
     *
     * @param chatId    The chat id.
     * @param handle    User handle.
     * @return          The Chat Request.
     */
    suspend operator fun invoke(
        chatId: Long,
        handle: Long,
    ): ChatRequest
}