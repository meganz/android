package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.messages.ForwardResult
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.UserMessage
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import mega.privacy.android.domain.usecase.chat.message.forward.ForwardMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.retry.RetryMessageUseCase
import javax.inject.Inject

/**
 * Resend message use case
 */
class ResendMessageUseCase @Inject constructor(
    private val chatMessageRepository: ChatMessageRepository,
    private val retryMessageUseCases: Set<@JvmSuppressWildcards RetryMessageUseCase>,
    private val forwardMessageUseCases: Set<@JvmSuppressWildcards ForwardMessageUseCase>,
) {
    /**
     * Invoke
     *
     * @param message
     */
    suspend operator fun invoke(message: TypedMessage) {
        val userMessage = message as? UserMessage
            ?: throw IllegalArgumentException("Only messages of type UserMessage can be sent")

        retryMessageUseCases.firstOrNull { useCase ->
            useCase.canRetryMessage(message)
        }?.invoke(message) ?: run {
            //if there's no specific use-case to resend, let's try to forward it to the same chat
            val result =
                forwardMessageUseCases.firstNotNullOfOrNull { useCase ->
                    useCase(
                        targetChatIds = listOf(userMessage.chatId),
                        message = userMessage
                    ).firstOrNull()
                }

            when (result) {
                is ForwardResult.Success -> {
                    chatMessageRepository.removeSentMessage(userMessage)
                }

                null -> throw IllegalStateException("No forward use case found to handle message type ${userMessage::class}")

                else -> throw Exception("Forward use case returned an error result: $result")
            }
        }
    }
}
