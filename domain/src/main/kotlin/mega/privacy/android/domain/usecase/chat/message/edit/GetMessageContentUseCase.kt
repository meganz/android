package mega.privacy.android.domain.usecase.chat.message.edit

import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import javax.inject.Inject

/**
 * Use case for getting the content of a message.
 */
class GetMessageContentUseCase @Inject constructor(
    private val getContentFromMessagesUseCases: Set<@JvmSuppressWildcards GetContentFromMessageUseCase>,
) {

    /**
     * Invoke.
     *
     * @param message Message to get its content.
     * @return Message content.
     */
    suspend operator fun invoke(message: TypedMessage): String {
        return getContentFromMessagesUseCases.fold(null as String?) { messageContent, getContentMessage ->
            if (messageContent != null) return messageContent
            getContentMessage(message)
        }.orEmpty()
    }
}