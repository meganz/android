package mega.privacy.android.domain.usecase.chat.message.retry

import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Abstract class to retry message use cases for specific messages types and/or states
 */
abstract class RetryMessageUseCase {

    /**
     * @return true if the message can be retried by this use case, false otherwise
     */
    abstract fun canRetryMessage(message: TypedMessage): Boolean

    /**
     * Invoke
     */
    abstract suspend operator fun invoke(message: TypedMessage)
}