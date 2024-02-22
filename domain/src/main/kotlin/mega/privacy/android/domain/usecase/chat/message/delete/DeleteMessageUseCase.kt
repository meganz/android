package mega.privacy.android.domain.usecase.chat.message.delete

import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Delete messages use case.
 */
abstract class DeleteMessageUseCase {

    /**
     * Invoke
     *
     * @param messages Messages to delete
     * @param
     */
    suspend operator fun invoke(messages: List<TypedMessage>): List<TypedMessage> {
        val (handledMessages, unhandled) = messages.partition { canDelete(it) }
        handledMessages.forEach { message -> deleteMessage(message) }
        return unhandled
    }

    /**
     * Delete message.
     *
     * @param message Message to delete.
     */
    protected abstract suspend fun deleteMessage(message: TypedMessage)

    /**
     * Check if the message can be deleted.
     */
    protected abstract suspend fun canDelete(message: TypedMessage): Boolean
}

