package mega.privacy.android.domain.usecase.chat.message.forward

import mega.privacy.android.domain.entity.chat.messages.ForwardResult
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Forward messages use case.
 */
abstract class ForwardMessageUseCase {

    /**
     * Invoke
     *
     * @param targetChatIds Target chat ID
     * @param message Message to forward
     * @param
     */
    suspend operator fun invoke(
        targetChatIds: List<Long>,
        message: TypedMessage,
    ): List<ForwardResult> = buildList {
        targetChatIds.forEach { targetChatId ->
            forwardMessage(targetChatId, message)?.let { add(it) }
        }
    }

    /**
     * Forward message
     *
     * @param targetChatId
     * @param message
     * @return result or null if not handled
     */
    protected abstract suspend fun forwardMessage(
        targetChatId: Long,
        message: TypedMessage,
    ): ForwardResult?
}

