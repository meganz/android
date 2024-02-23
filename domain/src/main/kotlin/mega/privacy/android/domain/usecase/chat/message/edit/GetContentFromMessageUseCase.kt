package mega.privacy.android.domain.usecase.chat.message.edit

import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Get content from messages.
 */
abstract class GetContentFromMessageUseCase {

    /**
     * Invoke
     *
     * @param message Messages to get its content.
     * @param
     */
    suspend operator fun invoke(message: TypedMessage): String? = getContent(message)

    /**
     * Get content message
     *
     * @param message Message to get its content.
     */
    protected abstract suspend fun getContent(message: TypedMessage): String?
}