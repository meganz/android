package mega.privacy.android.domain.entity.chat.messages

/**
 * Forward result
 *
 * @constructor Create empty Forward result
 */
sealed class ForwardResult {

    /**
     * Success.
     *
     * @property chatId Chat id where the messages were forwarded.
     */
    data class Success(val chatId: Long) : ForwardResult()

    /**
     * Error when the resource is not available.
     */
    data object ErrorNotAvailable : ForwardResult()

    /**
     * General error.
     */
    data object GeneralError : ForwardResult()
}