package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.ChatRequest

/**
 * Use case for check chat link
 */
fun interface CheckChatLink {

    /**
     * Invoke.
     *
     * @param link Null-terminated character string with the public chat link.
     * @return The chat basic info.
     */
    suspend operator fun invoke(
        link: String
    ): ChatRequest
}