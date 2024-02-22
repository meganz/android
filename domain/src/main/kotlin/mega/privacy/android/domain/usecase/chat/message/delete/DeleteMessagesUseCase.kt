package mega.privacy.android.domain.usecase.chat.message.delete

import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import javax.inject.Inject

/**
 * Use case to forward messages.
 */
class DeleteMessagesUseCase @Inject constructor(
    private val deleteMessagesUseCases: Set<@JvmSuppressWildcards DeleteMessageUseCase>,
) {

    /**
     * Invoke.
     *
     * @param messages List of messages to delete.
     * @return List of results of the forward operation.
     */
    suspend operator fun invoke(messages: List<TypedMessage>) {
        deleteMessagesUseCases.fold(messages) { remainingMessages, deleteUseCase ->
            if (remainingMessages.isEmpty()) return
            deleteUseCase(remainingMessages)
        }
    }
}